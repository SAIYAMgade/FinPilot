package com.finpilot.core.reconciliation;

import com.finpilot.core.transaction.Transaction;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "reconciliation_matches")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationMatch {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "reconciliation_run_id", nullable = false, length = 36)
    private String reconciliationRunId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "external_record_id")
    private ExternalStatementRecord externalRecord;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_type", nullable = false, length = 50)
    private MatchType matchType;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_status", nullable = false, length = 50)
    private MatchStatus matchStatus;

    @Column(name = "amount_difference", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal amountDifference = BigDecimal.ZERO;

    @Column(name = "match_score", nullable = false)
    @Builder.Default
    private int matchScore = 100;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @PrePersist
    public void onPrePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
