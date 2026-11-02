package com.finpilot.core.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class DashboardDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricsSummary {
        private long totalTransactions;
        private BigDecimal totalTransactionValue;
        private double reconciliationSuccessRate;
        private long unmatchedTransactions;
        private long openExceptions;
        private long pendingApprovals;
        private BigDecimal totalDiscrepancyAmount;
        private long failedTransactions;
        private double averageInvestigationTimeMinutes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChartDataPoint {
        private String label;
        private double value;
        private String secondaryLabel;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardAnalyticsResponse {
        private MetricsSummary metrics;
        private List<ChartDataPoint> statusDistribution;
        private List<ChartDataPoint> reconciliationOutcomes;
        private List<ChartDataPoint> exceptionSeverityDistribution;
        private List<ChartDataPoint> dailyVolumeTrend;
        private List<ChartDataPoint> discrepancyTrend;
    }
}
