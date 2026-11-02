package com.finpilot.core.approval;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.finpilot.core.auth.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "approval_decisions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalDecision {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_request_id", nullable = false)
    @JsonIgnore
    private ApprovalRequest approvalRequest;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "decided_by", nullable = false)
    private User decidedBy;

    @Column(nullable = false, length = 50)
    private String decision; // APPROVED, REJECTED

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Column(name = "decided_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant decidedAt = Instant.now();

    @PrePersist
    public void onPrePersist() {
        if (decidedAt == null) decidedAt = Instant.now();
    }
}
