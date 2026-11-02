package com.finpilot.core.approval;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResolutionProposalRepository extends JpaRepository<ResolutionProposal, String> {
    List<ResolutionProposal> findByFinancialExceptionId(String exceptionId);
}
