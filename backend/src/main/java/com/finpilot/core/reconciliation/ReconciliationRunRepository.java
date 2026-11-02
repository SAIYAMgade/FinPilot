package com.finpilot.core.reconciliation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReconciliationRunRepository extends JpaRepository<ReconciliationRun, String> {
    Optional<ReconciliationRun> findByRunNumber(String runNumber);
    List<ReconciliationRun> findAllByOrderByCreatedAtDesc();

    @Query("SELECT COUNT(r) FROM ReconciliationRun r")
    long countTotalRuns();

    @Query("SELECT COALESCE(SUM(r.matchedCount), 0) FROM ReconciliationRun r")
    long sumMatchedCount();

    @Query("SELECT COALESCE(SUM(r.discrepancyCount), 0) FROM ReconciliationRun r")
    long sumDiscrepancyCount();
}
