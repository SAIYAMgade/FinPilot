package com.finpilot.core.ai;

import com.finpilot.core.auth.User;
import com.finpilot.core.exception.FinancialException;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "investigations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Investigation {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "exception_id", nullable = false)
    private FinancialException financialException;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String status = "RUNNING"; // RUNNING, COMPLETED, FAILED

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "initiated_by", nullable = false)
    private User initiatedBy;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "root_cause_analysis", columnDefinition = "TEXT")
    private String rootCauseAnalysis;

    @Column(name = "recommended_action", columnDefinition = "TEXT")
    private String recommendedAction;

    @Column(name = "confidence_score", precision = 5, scale = 2)
    private BigDecimal confidenceScore;

    @Column(name = "evidence_payload", columnDefinition = "TEXT")
    private String evidencePayload;

    @Column(name = "policy_validation_notes", columnDefinition = "TEXT")
    private String policyValidationNotes;

    @OneToMany(mappedBy = "investigation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<AgentExecutionEvent> events = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    public void onPrePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = "RUNNING";
    }
}
