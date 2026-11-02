package com.finpilot.core.config;

import com.finpilot.core.approval.*;
import com.finpilot.core.auth.Role;
import com.finpilot.core.auth.RoleRepository;
import com.finpilot.core.auth.User;
import com.finpilot.core.auth.UserRepository;
import com.finpilot.core.exception.*;
import com.finpilot.core.ledger.*;
import com.finpilot.core.reconciliation.*;
import com.finpilot.core.transaction.Transaction;
import com.finpilot.core.transaction.TransactionRepository;
import com.finpilot.core.transaction.TransactionStatus;
import com.finpilot.core.transaction.TransactionType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final ReconciliationRunRepository runRepository;
    private final ExternalStatementRecordRepository externalRecordRepository;
    private final ReconciliationMatchRepository matchRepository;
    private final FinancialExceptionRepository exceptionRepository;
    private final ResolutionProposalRepository proposalRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Checking and seeding FinPilot demo data...");

        // 1. Seed Roles
        Role adminRole = getOrCreateRole("ADMIN", "Full platform administrator");
        Role analystRole = getOrCreateRole("FINANCE_ANALYST", "Finance operations analyst");
        Role approverRole = getOrCreateRole("FINANCE_APPROVER", "Financial adjustments approver");
        Role auditorRole = getOrCreateRole("AUDITOR", "Read-only auditor");

        // 2. Seed Users with BCrypt hash for "Password@123"
        String defaultPasswordHash = passwordEncoder.encode("Password@123");

        User admin = getOrCreateUser("admin", "admin@finpilot.io", "System Administrator", defaultPasswordHash, Set.of(adminRole));
        User analyst = getOrCreateUser("analyst", "analyst@finpilot.io", "Alex Chen (Analyst)", defaultPasswordHash, Set.of(analystRole));
        User approver = getOrCreateUser("approver", "approver@finpilot.io", "Sarah Jenkins (Approver)", defaultPasswordHash, Set.of(approverRole));
        User auditor = getOrCreateUser("auditor", "auditor@finpilot.io", "Marcus Vance (Auditor)", defaultPasswordHash, Set.of(auditorRole));

        // 3. Seed Accounts
        Account accCash = getOrCreateAccount("1010", "Cash & Settlement Clearing", AccountType.ASSET, new BigDecimal("5000000.0000"));
        Account accAr = getOrCreateAccount("1020", "Accounts Receivable - Settlements", AccountType.ASSET, new BigDecimal("2500000.0000"));
        Account accSuspense = getOrCreateAccount("1030", "Reconciliation Suspense Account", AccountType.ASSET, BigDecimal.ZERO);
        Account accAp = getOrCreateAccount("2010", "Accounts Payable - Merchant Payouts", AccountType.LIABILITY, new BigDecimal("3500000.0000"));
        Account accEquity = getOrCreateAccount("3010", "Retained Earnings", AccountType.EQUITY, new BigDecimal("4000000.0000"));
        Account accRevenue = getOrCreateAccount("4010", "Processing Fee Revenue", AccountType.REVENUE, new BigDecimal("125000.0000"));
        Account accExpense = getOrCreateAccount("5010", "Interchange & Gateway Fee Expense", AccountType.EXPENSE, new BigDecimal("35000.0000"));
        Account accWriteOff = getOrCreateAccount("5020", "Reconciliation Discrepancy Write-Off", AccountType.EXPENSE, new BigDecimal("10000.0000"));

        // 4. Seed Sample Transactions if empty
        if (transactionRepository.count() < 10) {
            log.info("Seeding initial transactions...");
            Instant now = Instant.now();

            Transaction txn1 = createTxn("TXN-1001", "PAYGATE-IN", "ACC-7701", "ACC-1010", new BigDecimal("50000.0000"),
                    TransactionStatus.POSTED, TransactionType.PAYMENT, "Customer subscription checkout", now.minus(2, ChronoUnit.DAYS));
            Transaction txn2 = createTxn("TXN-1002", "PAYGATE-IN", "ACC-7702", "ACC-1010", new BigDecimal("80000.0000"),
                    TransactionStatus.POSTED, TransactionType.PAYMENT, "Merchant bulk settlement", now.minus(2, ChronoUnit.DAYS));
            Transaction txn3 = createTxn("TXN-1003", "PAYGATE-IN", "ACC-7703", "ACC-1010", new BigDecimal("20000.0000"),
                    TransactionStatus.PENDING, TransactionType.PAYMENT, "B2B invoice clearance", now.minus(1, ChronoUnit.DAYS));
            Transaction txn4 = createTxn("TXN-1004", "PAYGATE-IN", "ACC-7704", "ACC-1010", new BigDecimal("15000.0000"),
                    TransactionStatus.REQUIRES_REVIEW, TransactionType.PAYMENT, "E-commerce marketplace order", now.minus(1, ChronoUnit.DAYS));
            Transaction txn5 = createTxn("TXN-1005", "PAYGATE-IN", "ACC-7705", "ACC-1010", new BigDecimal("4500.0000"),
                    TransactionStatus.FAILED, TransactionType.REFUND, "Failed chargeback reversal", now.minus(3, ChronoUnit.DAYS));

            // Seed a sample posted double-entry journal entry
            if (journalEntryRepository.count() == 0) {
                JournalEntry jnl = JournalEntry.builder()
                        .id("jnl-seed-001")
                        .referenceId("TXN-1001")
                        .description("Initial customer settlement posting for TXN-1001")
                        .currency("INR")
                        .postingStatus(PostingStatus.POSTED)
                        .postedTimestamp(now.minus(2, ChronoUnit.DAYS))
                        .lines(new ArrayList<>())
                        .build();

                JournalEntryLine lineDebit = JournalEntryLine.builder()
                        .id("jel-seed-001")
                        .journalEntry(jnl)
                        .account(accCash)
                        .debitAmount(new BigDecimal("50000.0000"))
                        .creditAmount(BigDecimal.ZERO)
                        .description("Debit cash clearing")
                        .lineNumber(1)
                        .build();

                JournalEntryLine lineCredit = JournalEntryLine.builder()
                        .id("jel-seed-002")
                        .journalEntry(jnl)
                        .account(accAr)
                        .debitAmount(BigDecimal.ZERO)
                        .creditAmount(new BigDecimal("50000.0000"))
                        .description("Credit accounts receivable")
                        .lineNumber(2)
                        .build();

                jnl.getLines().add(lineDebit);
                jnl.getLines().add(lineCredit);
                journalEntryRepository.save(jnl);
            }

            // Seed a sample reconciliation run with matched and discrepancy records
            if (runRepository.count() == 0) {
                ReconciliationRun reconRun = ReconciliationRun.builder()
                        .id("rec-seed-001")
                        .runNumber("REC-8801")
                        .fileName("bank_settlement_october.csv")
                        .fileChecksum("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
                        .matchingRulesVersion("v1.0")
                        .status("COMPLETED")
                        .totalInternalRecords(5)
                        .totalExternalRecords(3)
                        .matchedCount(2)
                        .discrepancyCount(1)
                        .executionDurationMs(342)
                        .createdAt(now.minus(1, ChronoUnit.DAYS))
                        .completedAt(now.minus(1, ChronoUnit.DAYS))
                        .build();

                runRepository.save(reconRun);

                ExternalStatementRecord ext1 = ExternalStatementRecord.builder()
                        .id("esr-seed-001")
                        .reconciliationRunId(reconRun.getId())
                        .transactionReference("TXN-1001")
                        .amount(new BigDecimal("50000.0000"))
                        .currency("INR")
                        .recordDate(now.minus(2, ChronoUnit.DAYS))
                        .rawStatus("SETTLED")
                        .build();

                ExternalStatementRecord ext2 = ExternalStatementRecord.builder()
                        .id("esr-seed-002")
                        .reconciliationRunId(reconRun.getId())
                        .transactionReference("TXN-1002")
                        .amount(new BigDecimal("78500.0000")) // INR 1,500 discrepancy
                        .currency("INR")
                        .recordDate(now.minus(2, ChronoUnit.DAYS))
                        .rawStatus("SETTLED")
                        .build();

                externalRecordRepository.saveAll(List.of(ext1, ext2));

                ReconciliationMatch m1 = ReconciliationMatch.builder()
                        .id("match-seed-001")
                        .reconciliationRunId(reconRun.getId())
                        .transaction(txn1)
                        .externalRecord(ext1)
                        .matchType(MatchType.LEVEL_1_EXACT)
                        .matchStatus(MatchStatus.MATCHED)
                        .amountDifference(BigDecimal.ZERO)
                        .matchScore(100)
                        .notes("Exact reference, amount, and date match")
                        .build();

                ReconciliationMatch m2 = ReconciliationMatch.builder()
                        .id("match-seed-002")
                        .reconciliationRunId(reconRun.getId())
                        .transaction(txn2)
                        .externalRecord(ext2)
                        .matchType(MatchType.LEVEL_2_AMOUNT_MISMATCH)
                        .matchStatus(MatchStatus.AMOUNT_MISMATCH)
                        .amountDifference(new BigDecimal("1500.0000"))
                        .matchScore(70)
                        .notes("Amount mismatch: Internal INR 80,000.00 vs External INR 78,500.00")
                        .build();

                matchRepository.saveAll(List.of(m1, m2));

                // Seed exception for this discrepancy
                FinancialException exc = FinancialException.builder()
                        .id("exc-seed-001")
                        .exceptionNumber("EXP-1044")
                        .transaction(txn2)
                        .reconciliationRunId(reconRun.getId())
                        .category(ExceptionCategory.AMOUNT_DISCREPANCY)
                        .severity(ExceptionSeverity.HIGH)
                        .description("Bank settlement statement for TXN-1002 short by INR 1,500.00")
                        .financialImpact(new BigDecimal("1500.0000"))
                        .status(ExceptionStatus.PENDING_APPROVAL)
                        .assignedTo(analyst)
                        .rootCause("Payment gateway merchant interchange fee of INR 1,500 was withheld at source")
                        .resolutionNotes("Proposed adjustment to debit Interchange Fees (5010) and credit Suspense (1030)")
                        .build();

                FinancialException savedExc = exceptionRepository.save(exc);

                // Seed resolution proposal & approval request
                ResolutionProposal prop = ResolutionProposal.builder()
                        .id("prop-seed-001")
                        .financialException(savedExc)
                        .proposedAction("POST_LEDGER_ADJUSTMENT")
                        .adjustmentAmount(new BigDecimal("1500.0000"))
                        .debitAccount(accExpense)
                        .creditAccount(accSuspense)
                        .rationale("Reconcile bank interchange fee withholding of INR 1,500.00 for TXN-1002")
                        .status(ProposalStatus.SUBMITTED)
                        .createdBy(analyst)
                        .createdAt(now.minus(4, ChronoUnit.HOURS))
                        .build();

                ResolutionProposal savedProp = proposalRepository.save(prop);

                ApprovalRequest appr = ApprovalRequest.builder()
                        .id("appr-seed-001")
                        .requestNumber("REQ-5021")
                        .resolutionProposal(savedProp)
                        .requestedBy(analyst)
                        .status(ApprovalStatus.PENDING)
                        .requestedAt(now.minus(4, ChronoUnit.HOURS))
                        .expiresAt(now.plus(7, ChronoUnit.DAYS))
                        .build();

                approvalRequestRepository.save(appr);
            }
        }

        log.info("FinPilot demo data seeding complete. Demo credentials available: admin / analyst / approver / auditor (Password: Password@123)");
    }

    private Role getOrCreateRole(String name, String description) {
        return roleRepository.findByName(name).orElseGet(() ->
                roleRepository.save(Role.builder()
                        .id("role-" + UUID.randomUUID().toString().substring(0, 8))
                        .name(name)
                        .description(description)
                        .build()));
    }

    private User getOrCreateUser(String username, String email, String fullName, String passwordHash, Set<Role> roles) {
        return userRepository.findByUsername(username).orElseGet(() ->
                userRepository.save(User.builder()
                        .id("usr-" + UUID.randomUUID().toString().substring(0, 8))
                        .username(username)
                        .email(email)
                        .fullName(fullName)
                        .passwordHash(passwordHash)
                        .status("ACTIVE")
                        .roles(roles)
                        .build()));
    }

    private Account getOrCreateAccount(String accNo, String name, AccountType type, BigDecimal balance) {
        return accountRepository.findByAccountNumber(accNo).orElseGet(() ->
                accountRepository.save(Account.builder()
                        .id("acc-" + accNo)
                        .accountNumber(accNo)
                        .accountName(name)
                        .accountType(type)
                        .currency("INR")
                        .status("ACTIVE")
                        .currentBalance(balance)
                        .build()));
    }

    private Transaction createTxn(String extRef, String source, String srcAcc, String dstAcc, BigDecimal amount,
                                  TransactionStatus status, TransactionType type, String desc, Instant ts) {
        Transaction t = Transaction.builder()
                .id("txn-" + UUID.randomUUID().toString().substring(0, 8))
                .externalReference(extRef)
                .sourceSystem(source)
                .sourceAccountNumber(srcAcc)
                .destinationAccountNumber(dstAcc)
                .amount(amount)
                .currency("INR")
                .status(status)
                .transactionType(type)
                .idempotencyKey("idemp-" + extRef)
                .description(desc)
                .transactionTimestamp(ts)
                .receivedTimestamp(ts)
                .build();
        return transactionRepository.save(t);
    }
}
