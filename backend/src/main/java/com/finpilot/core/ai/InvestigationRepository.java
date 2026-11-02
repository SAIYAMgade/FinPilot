package com.finpilot.core.ai;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvestigationRepository extends JpaRepository<Investigation, String> {
    List<Investigation> findByFinancialExceptionId(String exceptionId);
    List<Investigation> findAllByOrderByCreatedAtDesc();
}
