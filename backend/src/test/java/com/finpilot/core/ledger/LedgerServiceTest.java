package com.finpilot.core.ledger;

import com.finpilot.core.common.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private JournalEntryRepository journalEntryRepository;

    @Mock
    private JournalEntryLineRepository journalEntryLineRepository;

    @Mock
    private AccountBalanceSnapshotRepository snapshotRepository;

    @InjectMocks
    private LedgerService ledgerService;

    private Account cashAccount;
    private Account arAccount;

    @BeforeEach
    void setUp() {
        cashAccount = Account.builder()
                .id("acc-1010")
                .accountNumber("1010")
                .accountName("Cash Clearing")
                .accountType(AccountType.ASSET)
                .currentBalance(new BigDecimal("10000.0000"))
                .currency("INR")
                .status("ACTIVE")
                .build();

        arAccount = Account.builder()
                .id("acc-1020")
                .accountNumber("1020")
                .accountName("Accounts Receivable")
                .accountType(AccountType.ASSET)
                .currentBalance(new BigDecimal("5000.0000"))
                .currency("INR")
                .status("ACTIVE")
                .build();
    }

    @Test
    @DisplayName("Should strictly REJECT unbalanced journal entries where Debits != Credits")
    void shouldRejectUnbalancedJournalEntry() {
        LedgerDtos.CreateJournalEntryRequest request = LedgerDtos.CreateJournalEntryRequest.builder()
                .referenceId("TXN-UNBALANCED")
                .description("Invalid unbalanced test entry")
                .currency("INR")
                .lines(List.of(
                        LedgerDtos.CreateJournalLineRequest.builder()
                                .accountId("acc-1010")
                                .debitAmount(new BigDecimal("5000.0000"))
                                .creditAmount(BigDecimal.ZERO)
                                .description("Debit 5000")
                                .build(),
                        LedgerDtos.CreateJournalLineRequest.builder()
                                .accountId("acc-1020")
                                .debitAmount(BigDecimal.ZERO)
                                .creditAmount(new BigDecimal("4500.0000")) // Discrepancy of 500!
                                .description("Credit 4500")
                                .build()
                ))
                .autoPost(false)
                .build();

        ApiException exception = assertThrows(ApiException.class, () -> ledgerService.createJournalEntry(request));
        assertTrue(exception.getMessage().contains("Unbalanced journal entry rejected"));
        verify(journalEntryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully create and balance a valid double-entry journal entry")
    void shouldCreateBalancedJournalEntry() {
        when(accountRepository.findById("acc-1010")).thenReturn(Optional.of(cashAccount));
        when(accountRepository.findById("acc-1020")).thenReturn(Optional.of(arAccount));
        when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LedgerDtos.CreateJournalEntryRequest request = LedgerDtos.CreateJournalEntryRequest.builder()
                .referenceId("TXN-BALANCED")
                .description("Valid balanced entry")
                .currency("INR")
                .lines(List.of(
                        LedgerDtos.CreateJournalLineRequest.builder()
                                .accountId("acc-1010")
                                .debitAmount(new BigDecimal("5000.0000"))
                                .creditAmount(BigDecimal.ZERO)
                                .description("Debit Cash 5000")
                                .build(),
                        LedgerDtos.CreateJournalLineRequest.builder()
                                .accountId("acc-1020")
                                .debitAmount(BigDecimal.ZERO)
                                .creditAmount(new BigDecimal("5000.0000"))
                                .description("Credit AR 5000")
                                .build()
                ))
                .autoPost(false)
                .build();

        LedgerDtos.JournalEntryResponse response = ledgerService.createJournalEntry(request);

        assertNotNull(response);
        assertEquals(PostingStatus.DRAFT, response.getPostingStatus());
        assertEquals(new BigDecimal("5000.0000"), response.getTotalDebits());
        assertEquals(new BigDecimal("5000.0000"), response.getTotalCredits());
    }

    @Test
    @DisplayName("Should post journal entry and update account balances correctly")
    void shouldPostJournalEntryAndUpdateBalances() {
        JournalEntry entry = JournalEntry.builder()
                .id("jnl-001")
                .referenceId("TXN-999")
                .description("Posting test")
                .currency("INR")
                .postingStatus(PostingStatus.DRAFT)
                .lines(new ArrayList<>())
                .build();

        JournalEntryLine debitLine = JournalEntryLine.builder()
                .id("line-1")
                .journalEntry(entry)
                .account(cashAccount)
                .debitAmount(new BigDecimal("2000.0000"))
                .creditAmount(BigDecimal.ZERO)
                .lineNumber(1)
                .build();

        JournalEntryLine creditLine = JournalEntryLine.builder()
                .id("line-2")
                .journalEntry(entry)
                .account(arAccount)
                .debitAmount(BigDecimal.ZERO)
                .creditAmount(new BigDecimal("2000.0000"))
                .lineNumber(2)
                .build();

        entry.getLines().add(debitLine);
        entry.getLines().add(creditLine);

        when(journalEntryRepository.findById("jnl-001")).thenReturn(Optional.of(entry));
        when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LedgerDtos.JournalEntryResponse response = ledgerService.postJournalEntry("jnl-001");

        assertEquals(PostingStatus.POSTED, response.getPostingStatus());
        assertNotNull(response.getPostedTimestamp());

        // Cash (Asset): 10,000 + 2,000 = 12,000
        assertEquals(new BigDecimal("12000.0000"), cashAccount.getCurrentBalance());
        // AR (Asset): 5,000 - 2,000 = 3,000
        assertEquals(new BigDecimal("3000.0000"), arAccount.getCurrentBalance());
    }
}
