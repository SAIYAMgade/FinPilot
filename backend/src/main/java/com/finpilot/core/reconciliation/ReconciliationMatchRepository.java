package com.finpilot.core.reconciliation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ReconciliationMatchRepository extends JpaRepository<ReconciliationMatch, String> {

    List<ReconciliationMatch> findByReconciliationRunId(String reconciliationRunId);

    Page<ReconciliationMatch> findByReconciliationRunId(String reconciliationRunId, Pageable pageable);

    Page<ReconciliationMatch> findByReconciliationRunIdAndMatchStatus(String reconciliationRunId, MatchStatus matchStatus, Pageable pageable);

    @Query("SELECT COALESCE(SUM(m.amountDifference), 0) FROM ReconciliationMatch m WHERE m.matchStatus = 'AMOUNT_MISMATCH'")
    BigDecimal sumTotalDiscrepancyAmount();

    @Query("SELECT COUNT(m) FROM ReconciliationMatch m WHERE m.matchStatus = :status")
    long countByMatchStatus(@Param("status") MatchStatus status);
}
