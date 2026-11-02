package com.finpilot.core.reconciliation;

import com.finpilot.core.transaction.TransactionDtos;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class ReconciliationDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReconciliationRunResponse {
        private String id;
        private String runNumber;
        private String fileName;
        private String fileChecksum;
        private String matchingRulesVersion;
        private String status;
        private int totalInternalRecords;
        private int totalExternalRecords;
        private int matchedCount;
        private int discrepancyCount;
        private long executionDurationMs;
        private String errorDetails;
        private Instant createdAt;
        private Instant completedAt;

        public static ReconciliationRunResponse fromEntity(ReconciliationRun run) {
            return ReconciliationRunResponse.builder()
                    .id(run.getId())
                    .runNumber(run.getRunNumber())
                    .fileName(run.getFileName())
                    .fileChecksum(run.getFileChecksum())
                    .matchingRulesVersion(run.getMatchingRulesVersion())
                    .status(run.getStatus())
                    .totalInternalRecords(run.getTotalInternalRecords())
                    .totalExternalRecords(run.getTotalExternalRecords())
                    .matchedCount(run.getMatchedCount())
                    .discrepancyCount(run.getDiscrepancyCount())
                    .executionDurationMs(run.getExecutionDurationMs())
                    .errorDetails(run.getErrorDetails())
                    .createdAt(run.getCreatedAt())
                    .completedAt(run.getCompletedAt())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExternalRecordDto {
        private String id;
        private String transactionReference;
        private BigDecimal amount;
        private String currency;
        private Instant recordDate;
        private String rawStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReconciliationMatchResponse {
        private String id;
        private String reconciliationRunId;
        private TransactionDtos.TransactionResponse transaction;
        private ExternalRecordDto externalRecord;
        private MatchType matchType;
        private MatchStatus matchStatus;
        private BigDecimal amountDifference;
        private int matchScore;
        private String notes;
        private Instant createdAt;

        public static ReconciliationMatchResponse fromEntity(ReconciliationMatch m) {
            ExternalRecordDto extDto = null;
            if (m.getExternalRecord() != null) {
                extDto = ExternalRecordDto.builder()
                        .id(m.getExternalRecord().getId())
                        .transactionReference(m.getExternalRecord().getTransactionReference())
                        .amount(m.getExternalRecord().getAmount())
                        .currency(m.getExternalRecord().getCurrency())
                        .recordDate(m.getExternalRecord().getRecordDate())
                        .rawStatus(m.getExternalRecord().getRawStatus())
                        .build();
            }

            TransactionDtos.TransactionResponse txnDto = null;
            if (m.getTransaction() != null) {
                txnDto = TransactionDtos.TransactionResponse.fromEntity(m.getTransaction());
            }

            return ReconciliationMatchResponse.builder()
                    .id(m.getId())
                    .reconciliationRunId(m.getReconciliationRunId())
                    .transaction(txnDto)
                    .externalRecord(extDto)
                    .matchType(m.getMatchType())
                    .matchStatus(m.getMatchStatus())
                    .amountDifference(m.getAmountDifference())
                    .matchScore(m.getMatchScore())
                    .notes(m.getNotes())
                    .createdAt(m.getCreatedAt())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReconciliationReport {
        private ReconciliationRunResponse runSummary;
        private double matchRatePercent;
        private BigDecimal totalDiscrepancyAmount;
        private List<ReconciliationMatchResponse> matches;
    }
}
