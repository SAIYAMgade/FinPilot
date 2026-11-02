package com.finpilot.core.transaction;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class TransactionDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateTransactionRequest {
        private String externalReference;

        @NotBlank(message = "sourceSystem is required")
        private String sourceSystem;

        private String sourceAccountNumber;
        private String destinationAccountNumber;

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.0001", message = "amount must be greater than zero")
        private BigDecimal amount;

        @NotBlank(message = "currency is required")
        @Builder.Default
        private String currency = "INR";

        @NotNull(message = "transactionType is required")
        private TransactionType transactionType;

        private String idempotencyKey;
        private String description;
        private String metadata;
        private Instant transactionTimestamp;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionResponse {
        private String id;
        private String externalReference;
        private String sourceSystem;
        private String sourceAccountNumber;
        private String destinationAccountNumber;
        private BigDecimal amount;
        private String currency;
        private TransactionType transactionType;
        private TransactionStatus status;
        private String idempotencyKey;
        private String description;
        private String metadata;
        private Instant transactionTimestamp;
        private Instant receivedTimestamp;
        private Instant createdAt;
        private Instant updatedAt;

        public static TransactionResponse fromEntity(Transaction t) {
            return TransactionResponse.builder()
                    .id(t.getId())
                    .externalReference(t.getExternalReference())
                    .sourceSystem(t.getSourceSystem())
                    .sourceAccountNumber(t.getSourceAccountNumber())
                    .destinationAccountNumber(t.getDestinationAccountNumber())
                    .amount(t.getAmount())
                    .currency(t.getCurrency())
                    .transactionType(t.getTransactionType())
                    .status(t.getStatus())
                    .idempotencyKey(t.getIdempotencyKey())
                    .description(t.getDescription())
                    .metadata(t.getMetadata())
                    .transactionTimestamp(t.getTransactionTimestamp())
                    .receivedTimestamp(t.getReceivedTimestamp())
                    .createdAt(t.getCreatedAt())
                    .updatedAt(t.getUpdatedAt())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionImportResult {
        private int totalRecords;
        private int importedCount;
        private int duplicateCount;
        private int failedCount;
        private List<String> errorMessages;
    }
}
