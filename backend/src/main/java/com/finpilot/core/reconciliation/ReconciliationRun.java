package com.finpilot.core.reconciliation;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "reconciliation_runs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationRun {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "run_number", nullable = false, unique = true, length = 50)
    private String runNumber;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_checksum", nullable = false, length = 64)
    private String fileChecksum;

    @Column(name = "matching_rules_version", nullable = false, length = 20)
    @Builder.Default
    private String matchingRulesVersion = "v1.0";

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String status = "PENDING"; // PENDING, PROCESSING, COMPLETED, FAILED

    @Column(name = "total_internal_records", nullable = false)
    @Builder.Default
    private int totalInternalRecords = 0;

    @Column(name = "total_external_records", nullable = false)
    @Builder.Default
    private int totalExternalRecords = 0;

    @Column(name = "matched_count", nullable = false)
    @Builder.Default
    private int matchedCount = 0;

    @Column(name = "discrepancy_count", nullable = false)
    @Builder.Default
    private int discrepancyCount = 0;

    @Column(name = "execution_duration_ms")
    @Builder.Default
    private long executionDurationMs = 0;

    @Column(name = "error_details", columnDefinition = "TEXT")
    private String errorDetails;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    public void onPrePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
