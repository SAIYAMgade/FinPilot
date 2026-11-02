package com.finpilot.core.approval;

import com.finpilot.core.auth.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "approval_requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalRequest {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "request_number", nullable = false, unique = true, length = 50)
    private String requestNumber;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "resolution_proposal_id", nullable = false)
    private ResolutionProposal resolutionProposal;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "requested_by", nullable = false)
    private User requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private ApprovalStatus status = ApprovalStatus.PENDING;

    @OneToMany(mappedBy = "approvalRequest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<ApprovalDecision> decisions = new ArrayList<>();

    @Column(name = "requested_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant requestedAt = Instant.now();

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @PrePersist
    public void onPrePersist() {
        if (requestedAt == null) requestedAt = Instant.now();
        if (expiresAt == null) expiresAt = Instant.now().plusSeconds(7 * 24 * 3600); // 7 days default expiry
        if (status == null) status = ApprovalStatus.PENDING;
    }
}
