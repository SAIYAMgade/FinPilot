package com.finpilot.core.approval;

import com.finpilot.core.auth.User;
import com.finpilot.core.exception.FinancialException;
import com.finpilot.core.ledger.Account;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "resolution_proposals")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolutionProposal {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "investigation_id", length = 36)
    private String investigationId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "exception_id", nullable = false)
    private FinancialException financialException;

    @Column(name = "proposed_action", nullable = false, length = 100)
    private String proposedAction; // POST_LEDGER_ADJUSTMENT, WRITE_OFF, REVERSE_TRANSACTION, MANUAL_MATCH

    @Column(name = "adjustment_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal adjustmentAmount = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "debit_account_id")
    private Account debitAccount;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "credit_account_id")
    private Account creditAccount;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String rationale;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private ProposalStatus status = ProposalStatus.SUBMITTED;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @PrePersist
    public void onPrePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = ProposalStatus.SUBMITTED;
    }
}
