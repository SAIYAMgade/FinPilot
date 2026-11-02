package com.finpilot.core.ledger;

import com.finpilot.core.common.ApiException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LedgerService {

    private static final Logger log = LoggerFactory.getLogger(LedgerService.class);

    private final AccountRepository accountRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalEntryLineRepository journalEntryLineRepository;
    private final AccountBalanceSnapshotRepository snapshotRepository;

    // --- Account Management ---

    @Transactional
    public LedgerDtos.AccountResponse createAccount(LedgerDtos.CreateAccountRequest req) {
        if (accountRepository.existsByAccountNumber(req.getAccountNumber())) {
            throw ApiException.conflict("Account with number " + req.getAccountNumber() + " already exists");
        }

        Account account = Account.builder()
                .id("acc-" + UUID.randomUUID().toString().substring(0, 8))
                .accountNumber(req.getAccountNumber().trim())
                .accountName(req.getAccountName().trim())
                .accountType(req.getAccountType())
                .currency(req.getCurrency() != null ? req.getCurrency().toUpperCase() : "INR")
                .status("ACTIVE")
                .currentBalance(BigDecimal.ZERO)
                .build();

        Account saved = accountRepository.save(account);
        return LedgerDtos.AccountResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<LedgerDtos.AccountResponse> getAllAccounts() {
        return accountRepository.findAll().stream()
                .map(LedgerDtos.AccountResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public LedgerDtos.AccountResponse getAccountById(String id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Account not found: " + id));
        return LedgerDtos.AccountResponse.fromEntity(account);
    }

    // --- Double-Entry Journal Management ---

    @Transactional
    public LedgerDtos.JournalEntryResponse createJournalEntry(LedgerDtos.CreateJournalEntryRequest req) {
        if (req.getLines() == null || req.getLines().size() < 2) {
            throw ApiException.badRequest("A double-entry journal must have at least two line items");
        }

        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (LedgerDtos.CreateJournalLineRequest lineReq : req.getLines()) {
            if (lineReq.getDebitAmount().compareTo(BigDecimal.ZERO) > 0 && lineReq.getCreditAmount().compareTo(BigDecimal.ZERO) > 0) {
                throw ApiException.badRequest("A journal line cannot have both a debit and a credit amount");
            }
            totalDebits = totalDebits.add(lineReq.getDebitAmount());
            totalCredits = totalCredits.add(lineReq.getCreditAmount());
        }

        // Enforce Core Invariant 1: Total Debits == Total Credits
        if (totalDebits.compareTo(totalCredits) != 0) {
            log.warn("Rejected unbalanced journal entry. Debits={}, Credits={}", totalDebits, totalCredits);
            throw ApiException.badRequest(String.format(
                    "Unbalanced journal entry rejected. Total debits (%s) must equal total credits (%s). Difference: %s",
                    totalDebits, totalCredits, totalDebits.subtract(totalCredits).abs()
            ));
        }

        if (totalDebits.compareTo(BigDecimal.ZERO) <= 0) {
            throw ApiException.badRequest("Journal entry total amount must be strictly greater than zero");
        }

        String journalId = "jnl-" + UUID.randomUUID().toString().substring(0, 8);
        JournalEntry journalEntry = JournalEntry.builder()
                .id(journalId)
                .referenceId(req.getReferenceId())
                .description(req.getDescription())
                .currency(req.getCurrency() != null ? req.getCurrency().toUpperCase() : "INR")
                .postingStatus(PostingStatus.DRAFT)
                .lines(new ArrayList<>())
                .build();

        int lineNum = 1;
        for (LedgerDtos.CreateJournalLineRequest lineReq : req.getLines()) {
            Account account = accountRepository.findById(lineReq.getAccountId())
                    .orElseThrow(() -> ApiException.notFound("Account not found: " + lineReq.getAccountId()));

            JournalEntryLine line = JournalEntryLine.builder()
                    .id("jel-" + UUID.randomUUID().toString().substring(0, 8))
                    .journalEntry(journalEntry)
                    .account(account)
                    .debitAmount(lineReq.getDebitAmount())
                    .creditAmount(lineReq.getCreditAmount())
                    .description(lineReq.getDescription())
                    .lineNumber(lineNum++)
                    .build();

            journalEntry.getLines().add(line);
        }

        JournalEntry saved = journalEntryRepository.save(journalEntry);

        if (req.isAutoPost()) {
            return postJournalEntry(saved.getId());
        }

        return LedgerDtos.JournalEntryResponse.fromEntity(saved);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public LedgerDtos.JournalEntryResponse postJournalEntry(String journalId) {
        JournalEntry entry = journalEntryRepository.findById(journalId)
                .orElseThrow(() -> ApiException.notFound("Journal entry not found: " + journalId));

        if (entry.getPostingStatus() == PostingStatus.POSTED) {
            throw ApiException.conflict("Journal entry is already posted and immutable");
        }
        if (entry.getPostingStatus() == PostingStatus.REVERSED) {
            throw ApiException.conflict("Cannot post an already reversed journal entry");
        }

        // Verify balance invariant again before committing posting
        BigDecimal totalDebits = entry.getLines().stream()
                .map(JournalEntryLine::getDebitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredits = entry.getLines().stream()
                .map(JournalEntryLine::getCreditAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalDebits.compareTo(totalCredits) != 0) {
            throw ApiException.badRequest("Journal entry is not balanced. Debits: " + totalDebits + ", Credits: " + totalCredits);
        }

        // Atomically update account balances
        for (JournalEntryLine line : entry.getLines()) {
            Account account = line.getAccount();
            BigDecimal netChange;

            if (account.getAccountType().isDebitNormal()) {
                // ASSET, EXPENSE: Debits increase, Credits decrease
                netChange = line.getDebitAmount().subtract(line.getCreditAmount());
            } else {
                // LIABILITY, EQUITY, REVENUE: Credits increase, Debits decrease
                netChange = line.getCreditAmount().subtract(line.getDebitAmount());
            }

            account.setCurrentBalance(account.getCurrentBalance().add(netChange));
            accountRepository.save(account);
        }

        entry.setPostingStatus(PostingStatus.POSTED);
        entry.setPostedTimestamp(Instant.now());
        JournalEntry updated = journalEntryRepository.save(entry);

        log.info("Posted journal entry id={}, ref={}, debits={}, credits={}",
                entry.getId(), entry.getReferenceId(), totalDebits, totalCredits);

        return LedgerDtos.JournalEntryResponse.fromEntity(updated);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public LedgerDtos.JournalEntryResponse reverseJournalEntry(String journalId, String reason) {
        JournalEntry original = journalEntryRepository.findById(journalId)
                .orElseThrow(() -> ApiException.notFound("Journal entry not found: " + journalId));

        if (original.getPostingStatus() != PostingStatus.POSTED) {
            throw ApiException.badRequest("Only POSTED journal entries can be reversed. Current status: " + original.getPostingStatus());
        }

        // Build opposite reversal lines (debit becomes credit, credit becomes debit)
        List<LedgerDtos.CreateJournalLineRequest> reversalLines = new ArrayList<>();
        for (JournalEntryLine origLine : original.getLines()) {
            reversalLines.add(LedgerDtos.CreateJournalLineRequest.builder()
                    .accountId(origLine.getAccount().getId())
                    .debitAmount(origLine.getCreditAmount())   // swap
                    .creditAmount(origLine.getDebitAmount())  // swap
                    .description("Reversal of line #" + origLine.getLineNumber() + " (" + origLine.getDescription() + ")")
                    .build());
        }

        String revRef = "REV-" + original.getReferenceId();
        String revDesc = "Reversal of " + original.getId() + ". Reason: " + (reason != null ? reason : "Operational correction");

        LedgerDtos.CreateJournalEntryRequest revReq = LedgerDtos.CreateJournalEntryRequest.builder()
                .referenceId(revRef)
                .description(revDesc)
                .currency(original.getCurrency())
                .lines(reversalLines)
                .autoPost(true)
                .build();

        LedgerDtos.JournalEntryResponse reversalResponse = createJournalEntry(revReq);

        // Mark original as REVERSED
        original.setPostingStatus(PostingStatus.REVERSED);
        original.setReversedTimestamp(Instant.now());
        journalEntryRepository.save(original);

        log.info("Reversed journal entry id={} with reversal entry id={}", original.getId(), reversalResponse.getId());
        return reversalResponse;
    }

    @Transactional(readOnly = true)
    public Page<LedgerDtos.JournalEntryResponse> getJournals(PostingStatus status, Pageable pageable) {
        Page<JournalEntry> page = status != null
                ? journalEntryRepository.findByPostingStatus(status, pageable)
                : journalEntryRepository.findAll(pageable);
        return page.map(LedgerDtos.JournalEntryResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public LedgerDtos.JournalEntryResponse getJournalById(String id) {
        JournalEntry entry = journalEntryRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Journal entry not found: " + id));
        return LedgerDtos.JournalEntryResponse.fromEntity(entry);
    }

    // --- Trial Balance & Reporting ---

    @Transactional(readOnly = true)
    public LedgerDtos.TrialBalanceReport getTrialBalance() {
        List<Account> accounts = accountRepository.findAll();
        List<LedgerDtos.TrialBalanceItem> items = new ArrayList<>();

        BigDecimal sumTotalDebits = BigDecimal.ZERO;
        BigDecimal sumTotalCredits = BigDecimal.ZERO;

        for (Account acc : accounts) {
            BigDecimal debits = journalEntryLineRepository.sumDebitsByAccountId(acc.getId());
            BigDecimal credits = journalEntryLineRepository.sumCreditsByAccountId(acc.getId());

            BigDecimal netDebit = BigDecimal.ZERO;
            BigDecimal netCredit = BigDecimal.ZERO;

            if (debits.compareTo(credits) > 0) {
                netDebit = debits.subtract(credits);
            } else if (credits.compareTo(debits) > 0) {
                netCredit = credits.subtract(debits);
            }

            sumTotalDebits = sumTotalDebits.add(netDebit);
            sumTotalCredits = sumTotalCredits.add(netCredit);

            items.add(LedgerDtos.TrialBalanceItem.builder()
                    .accountId(acc.getId())
                    .accountNumber(acc.getAccountNumber())
                    .accountName(acc.getAccountName())
                    .accountType(acc.getAccountType())
                    .debitTotal(debits)
                    .creditTotal(credits)
                    .netDebitBalance(netDebit)
                    .netCreditBalance(netCredit)
                    .build());
        }

        boolean balanced = sumTotalDebits.compareTo(sumTotalCredits) == 0;

        return LedgerDtos.TrialBalanceReport.builder()
                .generatedAt(Instant.now())
                .totalDebits(sumTotalDebits)
                .totalCredits(sumTotalCredits)
                .balanced(balanced)
                .accounts(items)
                .build();
    }
}
