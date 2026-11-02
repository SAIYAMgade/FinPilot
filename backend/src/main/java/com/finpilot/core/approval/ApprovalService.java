package com.finpilot.core.approval;

import com.finpilot.core.audit.AuditLogService;
import com.finpilot.core.auth.User;
import com.finpilot.core.auth.UserRepository;
import com.finpilot.core.common.ApiException;
import com.finpilot.core.exception.ExceptionStatus;
import com.finpilot.core.exception.FinancialException;
import com.finpilot.core.exception.FinancialExceptionRepository;
import com.finpilot.core.ledger.Account;
import com.finpilot.core.ledger.AccountRepository;
import com.finpilot.core.ledger.LedgerDtos;
import com.finpilot.core.ledger.LedgerService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApprovalService {

    private static final Logger log = LoggerFactory.getLogger(ApprovalService.class);

    private final ApprovalRequestRepository approvalRequestRepository;
    private final ResolutionProposalRepository proposalRepository;
    private final ApprovalDecisionRepository decisionRepository;
    private final FinancialExceptionRepository exceptionRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final LedgerService ledgerService;
    private final AuditLogService auditLogService;

    @Transactional
    public ApprovalDtos.ApprovalRequestResponse submitProposal(ApprovalDtos.CreateProposalRequest req, String requestedByUsername) {
        FinancialException exception = exceptionRepository.findById(req.getExceptionId())
                .orElseThrow(() -> ApiException.notFound("Financial exception not found: " + req.getExceptionId()));

        User submitter = userRepository.findByUsername(requestedByUsername)
                .orElseThrow(() -> ApiException.notFound("Submitter user not found: " + requestedByUsername));

        Account debitAccount = null;
        if (req.getDebitAccountId() != null) {
            debitAccount = accountRepository.findById(req.getDebitAccountId())
                    .orElseThrow(() -> ApiException.notFound("Debit account not found: " + req.getDebitAccountId()));
        }

        Account creditAccount = null;
        if (req.getCreditAccountId() != null) {
            creditAccount = accountRepository.findById(req.getCreditAccountId())
                    .orElseThrow(() -> ApiException.notFound("Credit account not found: " + req.getCreditAccountId()));
        }

        String propId = "prop-" + UUID.randomUUID().toString().substring(0, 8);
        ResolutionProposal proposal = ResolutionProposal.builder()
                .id(propId)
                .investigationId(req.getInvestigationId())
                .financialException(exception)
                .proposedAction(req.getProposedAction())
                .adjustmentAmount(req.getAdjustmentAmount())
                .debitAccount(debitAccount)
                .creditAccount(creditAccount)
                .rationale(req.getRationale())
                .status(ProposalStatus.SUBMITTED)
                .createdBy(submitter)
                .createdAt(Instant.now())
                .build();

        ResolutionProposal savedProposal = proposalRepository.save(proposal);

        String reqId = "appr-" + UUID.randomUUID().toString().substring(0, 8);
        String reqNum = "REQ-" + System.currentTimeMillis() % 1000000;

        ApprovalRequest request = ApprovalRequest.builder()
                .id(reqId)
                .requestNumber(reqNum)
                .resolutionProposal(savedProposal)
                .requestedBy(submitter)
                .status(ApprovalStatus.PENDING)
                .requestedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(7 * 24 * 3600))
                .build();

        ApprovalRequest savedRequest = approvalRequestRepository.save(request);

        // Update Exception status to PENDING_APPROVAL
        exception.setStatus(ExceptionStatus.PENDING_APPROVAL);
        exceptionRepository.save(exception);

        auditLogService.record("APPROVAL_REQUEST", savedRequest.getId(), "SUBMIT", submitter.getUsername(),
                "Exception: " + exception.getExceptionNumber() + " (OPEN)",
                "Submitted for approval. Amount: " + req.getAdjustmentAmount());

        log.info("Adjustment proposal submitted: request={}, exception={}, amount={}",
                savedRequest.getRequestNumber(), exception.getExceptionNumber(), req.getAdjustmentAmount());

        return ApprovalDtos.ApprovalRequestResponse.fromEntity(savedRequest);
    }

    @Transactional
    public ApprovalDtos.ApprovalRequestResponse processDecision(String requestId, ApprovalDtos.SubmitDecisionRequest req, String decidedByUsername) {
        ApprovalRequest request = approvalRequestRepository.findById(requestId)
                .orElseThrow(() -> ApiException.notFound("Approval request not found: " + requestId));

        if (request.getStatus() != ApprovalStatus.PENDING) {
            throw ApiException.conflict("Approval request is already " + request.getStatus());
        }

        if (Instant.now().isAfter(request.getExpiresAt())) {
            request.setStatus(ApprovalStatus.EXPIRED);
            approvalRequestRepository.save(request);
            throw ApiException.badRequest("This approval request has expired and can no longer be processed");
        }

        User approver = userRepository.findByUsername(decidedByUsername)
                .orElseThrow(() -> ApiException.notFound("Approver user not found: " + decidedByUsername));

        // Core Invariant: Separation of duties (No self-approval)
        if (request.getRequestedBy().getId().equals(approver.getId())) {
            log.warn("Blocked self-approval attempt: User {} attempted to approve their own request {}",
                    approver.getUsername(), request.getRequestNumber());
            throw ApiException.forbidden("Separation of Duties Violation: You cannot approve or reject an adjustment request you initiated");
        }

        String decisionType = req.getDecision().toUpperCase();
        if (!"APPROVED".equals(decisionType) && !"REJECTED".equals(decisionType)) {
            throw ApiException.badRequest("Decision must be either APPROVED or REJECTED");
        }

        ApprovalDecision decision = ApprovalDecision.builder()
                .id("dec-" + UUID.randomUUID().toString().substring(0, 8))
                .approvalRequest(request)
                .decidedBy(approver)
                .decision(decisionType)
                .comments(req.getComments())
                .decidedAt(Instant.now())
                .build();

        decisionRepository.save(decision);
        request.getDecisions().add(decision);

        ResolutionProposal proposal = request.getResolutionProposal();
        FinancialException exception = proposal.getFinancialException();

        if ("APPROVED".equals(decisionType)) {
            request.setStatus(ApprovalStatus.APPROVED);
            proposal.setStatus(ProposalStatus.APPROVED);

            // Execute deterministic financial operation if ledger accounts are specified
            if (proposal.getDebitAccount() != null && proposal.getCreditAccount() != null && proposal.getAdjustmentAmount().signum() > 0) {
                log.info("Executing deterministic ledger adjustment for proposal {}", proposal.getId());

                LedgerDtos.CreateJournalEntryRequest journalReq = LedgerDtos.CreateJournalEntryRequest.builder()
                        .referenceId("ADJ-" + exception.getExceptionNumber())
                        .description("Correction for exception " + exception.getExceptionNumber() + ": " + proposal.getRationale())
                        .currency("INR")
                        .lines(List.of(
                                LedgerDtos.CreateJournalLineRequest.builder()
                                        .accountId(proposal.getDebitAccount().getId())
                                        .debitAmount(proposal.getAdjustmentAmount())
                                        .creditAmount(java.math.BigDecimal.ZERO)
                                        .description("Adjustment debit for " + exception.getExceptionNumber())
                                        .build(),
                                LedgerDtos.CreateJournalLineRequest.builder()
                                        .accountId(proposal.getCreditAccount().getId())
                                        .debitAmount(java.math.BigDecimal.ZERO)
                                        .creditAmount(proposal.getAdjustmentAmount())
                                        .description("Adjustment credit for " + exception.getExceptionNumber())
                                        .build()
                        ))
                        .autoPost(true)
                        .build();

                ledgerService.createJournalEntry(journalReq);
                proposal.setStatus(ProposalStatus.EXECUTED);
            }

            exception.setStatus(ExceptionStatus.RESOLVED);
            exception.setResolutionNotes("Resolved via approved adjustment: " + proposal.getProposedAction() + ". " + (req.getComments() != null ? req.getComments() : ""));
            exceptionRepository.save(exception);
            proposalRepository.save(proposal);

            auditLogService.record("APPROVAL_REQUEST", request.getId(), "APPROVE", approver.getUsername(),
                    "PENDING", "APPROVED. Financial adjustment executed.");
        } else {
            request.setStatus(ApprovalStatus.REJECTED);
            proposal.setStatus(ProposalStatus.REJECTED);
            exception.setStatus(ExceptionStatus.OPEN);
            exception.setResolutionNotes("Proposal rejected by " + approver.getUsername() + ": " + req.getComments());
            exceptionRepository.save(exception);
            proposalRepository.save(proposal);

            auditLogService.record("APPROVAL_REQUEST", request.getId(), "REJECT", approver.getUsername(),
                    "PENDING", "REJECTED. Comments: " + req.getComments());
        }

        ApprovalRequest updated = approvalRequestRepository.save(request);
        return ApprovalDtos.ApprovalRequestResponse.fromEntity(updated);
    }

    @Transactional(readOnly = true)
    public List<ApprovalDtos.ApprovalRequestResponse> getAllRequests() {
        return approvalRequestRepository.findAllByOrderByRequestedAtDesc().stream()
                .map(ApprovalDtos.ApprovalRequestResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ApprovalDtos.ApprovalRequestResponse getRequestById(String id) {
        ApprovalRequest req = approvalRequestRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Approval request not found: " + id));
        return ApprovalDtos.ApprovalRequestResponse.fromEntity(req);
    }
}
