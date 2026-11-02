package com.finpilot.core.reconciliation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExternalStatementRecordRepository extends JpaRepository<ExternalStatementRecord, String> {
    List<ExternalStatementRecord> findByReconciliationRunId(String reconciliationRunId);
}
