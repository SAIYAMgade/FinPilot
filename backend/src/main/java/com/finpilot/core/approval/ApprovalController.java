package com.finpilot.core.approval;

import com.finpilot.core.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/approvals")
@RequiredArgsConstructor
@Tag(name = "Approval Center", description = "Maker-checker approval workflow for financial adjustments and exception resolutions")
public class ApprovalController {

    private final ApprovalService approvalService;

    @PostMapping("/proposals")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_ANALYST')")
    @Operation(summary = "Submit a financial adjustment proposal for approver review")
    public ResponseEntity<ApiResponse<ApprovalDtos.ApprovalRequestResponse>> submitProposal(
            @Valid @RequestBody ApprovalDtos.CreateProposalRequest request,
            Authentication authentication
    ) {
        String actor = authentication != null ? authentication.getName() : "ANALYST";
        ApprovalDtos.ApprovalRequestResponse response = approvalService.submitProposal(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Adjustment proposal submitted", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_ANALYST', 'FINANCE_APPROVER', 'AUDITOR')")
    @Operation(summary = "List all pending and historical approval requests")
    public ResponseEntity<ApiResponse<List<ApprovalDtos.ApprovalRequestResponse>>> getAllRequests() {
        List<ApprovalDtos.ApprovalRequestResponse> requests = approvalService.getAllRequests();
        return ResponseEntity.ok(ApiResponse.ok(requests));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_ANALYST', 'FINANCE_APPROVER', 'AUDITOR')")
    @Operation(summary = "Get approval request details by ID")
    public ResponseEntity<ApiResponse<ApprovalDtos.ApprovalRequestResponse>> getRequestById(@PathVariable String id) {
        ApprovalDtos.ApprovalRequestResponse response = approvalService.getRequestById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{id}/decide")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_APPROVER')")
    @Operation(summary = "Approve or reject adjustment request (Separation of duties enforced)")
    public ResponseEntity<ApiResponse<ApprovalDtos.ApprovalRequestResponse>> decide(
            @PathVariable String id,
            @Valid @RequestBody ApprovalDtos.SubmitDecisionRequest request,
            Authentication authentication
    ) {
        String actor = authentication != null ? authentication.getName() : "APPROVER";
        ApprovalDtos.ApprovalRequestResponse response = approvalService.processDecision(id, request, actor);
        return ResponseEntity.ok(ApiResponse.ok("Decision processed successfully", response));
    }
}
