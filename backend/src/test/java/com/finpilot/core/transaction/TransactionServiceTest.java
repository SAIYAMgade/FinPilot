package com.finpilot.core.transaction;

import com.finpilot.core.common.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionEventRepository transactionEventRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    @DisplayName("Should return existing transaction on identical idempotent replay")
    void shouldReturnExistingOnIdempotentReplay() {
        String key = "idemp-key-123";
        Transaction existing = Transaction.builder()
                .id("txn-existing")
                .amount(new BigDecimal("1500.0000"))
                .currency("INR")
                .sourceSystem("PAYGATE")
                .transactionType(TransactionType.PAYMENT)
                .status(TransactionStatus.POSTED)
                .idempotencyKey(key)
                .transactionTimestamp(Instant.now())
                .receivedTimestamp(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(transactionRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(existing));

        TransactionDtos.CreateTransactionRequest req = TransactionDtos.CreateTransactionRequest.builder()
                .idempotencyKey(key)
                .amount(new BigDecimal("1500.0000"))
                .currency("INR")
                .sourceSystem("PAYGATE")
                .transactionType(TransactionType.PAYMENT)
                .build();

        TransactionDtos.TransactionResponse response = transactionService.createTransaction(req, "TEST_USER");

        assertEquals("txn-existing", response.getId());
        assertEquals(TransactionStatus.POSTED, response.getStatus());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject and throw Conflict when idempotency key is reused with mismatched payload")
    void shouldRejectConflictingPayloadForReusedKey() {
        String key = "idemp-key-conflict";
        Transaction existing = Transaction.builder()
                .id("txn-existing")
                .amount(new BigDecimal("1500.0000"))
                .currency("INR")
                .sourceSystem("PAYGATE")
                .transactionType(TransactionType.PAYMENT)
                .status(TransactionStatus.POSTED)
                .idempotencyKey(key)
                .build();

        when(transactionRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(existing));

        // Different amount: 2000 vs 1500!
        TransactionDtos.CreateTransactionRequest req = TransactionDtos.CreateTransactionRequest.builder()
                .idempotencyKey(key)
                .amount(new BigDecimal("2000.0000"))
                .currency("INR")
                .sourceSystem("PAYGATE")
                .transactionType(TransactionType.PAYMENT)
                .build();

        ApiException ex = assertThrows(ApiException.class, () -> transactionService.createTransaction(req, "TEST_USER"));
        assertTrue(ex.getMessage().contains("Idempotency key reused with different transaction parameters"));
    }
}
