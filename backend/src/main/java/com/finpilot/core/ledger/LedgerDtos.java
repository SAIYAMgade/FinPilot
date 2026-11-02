package com.finpilot.core.ledger;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class LedgerDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateAccountRequest {
        @NotBlank(message = "accountNumber is required")
        private String accountNumber;

        @NotBlank(message = "accountName is required")
        private String accountName;

        @NotNull(message = "accountType is required")
        private AccountType accountType;

        @NotBlank(message = "currency is required")
        @Builder.Default
        private String currency = "INR";
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountResponse {
        private String id;
        private String accountNumber;
        private String accountName;
        private AccountType accountType;
        private String currency;
        private String status;
        private BigDecimal currentBalance;
        private Instant createdAt;

        public static AccountResponse fromEntity(Account a) {
            return AccountResponse.builder()
                    .id(a.getId())
                    .accountNumber(a.getAccountNumber())
                    .accountName(a.getAccountName())
                    .accountType(a.getAccountType())
                    .currency(a.getCurrency())
                    .status(a.getStatus())
                    .currentBalance(a.getCurrentBalance())
                    .createdAt(a.getCreatedAt())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateJournalLineRequest {
        @NotBlank(message = "accountId is required")
        private String accountId;

        @NotNull(message = "debitAmount is required")
        @DecimalMin(value = "0.0000", message = "debitAmount cannot be negative")
        private BigDecimal debitAmount;

        @NotNull(message = "creditAmount is required")
        @DecimalMin(value = "0.0000", message = "creditAmount cannot be negative")
        private BigDecimal creditAmount;

        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateJournalEntryRequest {
        @NotBlank(message = "referenceId is required")
        private String referenceId;

        @NotBlank(message = "description is required")
        private String description;

        @NotBlank(message = "currency is required")
        @Builder.Default
        private String currency = "INR";

        @NotEmpty(message = "Journal entry must contain at least two lines")
        private List<CreateJournalLineRequest> lines;

        private boolean autoPost;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JournalLineResponse {
        private String id;
        private String accountId;
        private String accountNumber;
        private String accountName;
        private AccountType accountType;
        private BigDecimal debitAmount;
        private BigDecimal creditAmount;
        private String description;
        private int lineNumber;

        public static JournalLineResponse fromEntity(JournalEntryLine line) {
            return JournalLineResponse.builder()
                    .id(line.getId())
                    .accountId(line.getAccount().getId())
                    .accountNumber(line.getAccount().getAccountNumber())
                    .accountName(line.getAccount().getAccountName())
                    .accountType(line.getAccount().getAccountType())
                    .debitAmount(line.getDebitAmount())
                    .creditAmount(line.getCreditAmount())
                    .description(line.getDescription())
                    .lineNumber(line.getLineNumber())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JournalEntryResponse {
        private String id;
        private String referenceId;
        private String description;
        private String currency;
        private PostingStatus postingStatus;
        private Instant postedTimestamp;
        private Instant reversedTimestamp;
        private String reversalOfId;
        private BigDecimal totalDebits;
        private BigDecimal totalCredits;
        private List<JournalLineResponse> lines;
        private Instant createdAt;

        public static JournalEntryResponse fromEntity(JournalEntry entry) {
            BigDecimal debits = entry.getLines() != null
                    ? entry.getLines().stream().map(JournalEntryLine::getDebitAmount).reduce(BigDecimal.ZERO, BigDecimal::add)
                    : BigDecimal.ZERO;
            BigDecimal credits = entry.getLines() != null
                    ? entry.getLines().stream().map(JournalEntryLine::getCreditAmount).reduce(BigDecimal.ZERO, BigDecimal::add)
                    : BigDecimal.ZERO;

            List<JournalLineResponse> lineResponses = entry.getLines() != null
                    ? entry.getLines().stream().map(JournalLineResponse::fromEntity).toList()
                    : List.of();

            return JournalEntryResponse.builder()
                    .id(entry.getId())
                    .referenceId(entry.getReferenceId())
                    .description(entry.getDescription())
                    .currency(entry.getCurrency())
                    .postingStatus(entry.getPostingStatus())
                    .postedTimestamp(entry.getPostedTimestamp())
                    .reversedTimestamp(entry.getReversedTimestamp())
                    .reversalOfId(entry.getReversalOfId())
                    .totalDebits(debits)
                    .totalCredits(credits)
                    .lines(lineResponses)
                    .createdAt(entry.getCreatedAt())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrialBalanceItem {
        private String accountId;
        private String accountNumber;
        private String accountName;
        private AccountType accountType;
        private BigDecimal debitTotal;
        private BigDecimal creditTotal;
        private BigDecimal netDebitBalance;
        private BigDecimal netCreditBalance;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrialBalanceReport {
        private Instant generatedAt;
        private BigDecimal totalDebits;
        private BigDecimal totalCredits;
        private boolean balanced;
        private List<TrialBalanceItem> accounts;
    }
}
