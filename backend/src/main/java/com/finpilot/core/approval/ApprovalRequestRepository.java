package com.finpilot.core.approval;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, String> {

    Optional<ApprovalRequest> findByRequestNumber(String requestNumber);

    List<ApprovalRequest> findByStatusOrderByRequestedAtDesc(ApprovalStatus status);

    List<ApprovalRequest> findAllByOrderByRequestedAtDesc();

    @Query("SELECT COUNT(a) FROM ApprovalRequest a WHERE a.status = 'PENDING'")
    long countPendingApprovals();
}
