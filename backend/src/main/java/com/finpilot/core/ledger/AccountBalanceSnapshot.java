package com.finpilot.core.ledger;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "account_balance_snapshots")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountBalanceSnapshot {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "snapshot_date", nullable = false)
    private Instant snapshotDate;

    @Column(name = "debit_total", nullable = false, precision = 19, scale = 4)
    private BigDecimal debitTotal;

    @Column(name = "credit_total", nullable = false, precision = 19, scale = 4)
    private BigDecimal creditTotal;

    @Column(name = "net_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal netBalance;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @PrePersist
    public void onPrePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
