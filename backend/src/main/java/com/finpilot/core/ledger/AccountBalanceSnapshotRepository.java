package com.finpilot.core.ledger;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountBalanceSnapshotRepository extends JpaRepository<AccountBalanceSnapshot, String> {
    List<AccountBalanceSnapshot> findByAccountIdOrderBySnapshotDateDesc(String accountId);
}
