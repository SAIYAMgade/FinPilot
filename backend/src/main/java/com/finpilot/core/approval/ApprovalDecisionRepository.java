package com.finpilot.core.approval;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalDecisionRepository extends JpaRepository<ApprovalDecision, String> {
    List<ApprovalDecision> findByApprovalRequestId(String approvalRequestId);
}
