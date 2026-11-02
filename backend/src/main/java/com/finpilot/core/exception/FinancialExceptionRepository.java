package com.finpilot.core.exception;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface FinancialExceptionRepository extends JpaRepository<FinancialException, String>, JpaSpecificationExecutor<FinancialException> {

    Optional<FinancialException> findByExceptionNumber(String exceptionNumber);

    List<FinancialException> findByTransactionId(String transactionId);

    List<FinancialException> findByReconciliationRunId(String reconciliationRunId);

    @Query("SELECT COUNT(e) FROM FinancialException e WHERE e.status IN ('OPEN', 'INVESTIGATING', 'PENDING_APPROVAL')")
    long countOpenExceptions();

    @Query("SELECT COUNT(e) FROM FinancialException e WHERE e.severity = :severity")
    long countBySeverity(@Param("severity") ExceptionSeverity severity);

    @Query("SELECT COALESCE(SUM(e.financialImpact), 0) FROM FinancialException e WHERE e.status NOT IN ('RESOLVED', 'CLOSED')")
    BigDecimal sumOpenDiscrepancyImpact();
}
