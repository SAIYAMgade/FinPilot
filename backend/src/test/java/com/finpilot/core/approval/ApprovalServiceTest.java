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
import com.finpilot.core.ledger.LedgerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApprovalServiceTest {

    @Mock
    private ApprovalRequestRepository approvalRequestRepository;

    @Mock
    private ResolutionProposalRepository proposalRepository;

    @Mock
    private ApprovalDecisionRepository decisionRepository;

    @Mock
    private FinancialExceptionRepository exceptionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LedgerService ledgerService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private ApprovalService approvalService;

    @Test
    @DisplayName("Should strictly PREVENT self-approval by submitter (Separation of Duties)")
    void shouldPreventSelfApproval() {
        User analyst = User.builder()
                .id("user-analyst")
                .username("analyst_bob")
                .build();

        ResolutionProposal proposal = ResolutionProposal.builder()
                .id("prop-1")
                .adjustmentAmount(new BigDecimal("5000.0000"))
                .financialException(FinancialException.builder().id("exc-1").build())
                .status(ProposalStatus.SUBMITTED)
                .build();

        ApprovalRequest request = ApprovalRequest.builder()
                .id("appr-1")
                .requestNumber("REQ-100")
                .requestedBy(analyst)
                .resolutionProposal(proposal)
                .status(ApprovalStatus.PENDING)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(approvalRequestRepository.findById("appr-1")).thenReturn(Optional.of(request));
        when(userRepository.findByUsername("analyst_bob")).thenReturn(Optional.of(analyst));

        ApprovalDtos.SubmitDecisionRequest decisionReq = new ApprovalDtos.SubmitDecisionRequest("APPROVED", "Self-approving");

        ApiException ex = assertThrows(ApiException.class, () ->
                approvalService.processDecision("appr-1", decisionReq, "analyst_bob"));

        assertTrue(ex.getMessage().contains("Separation of Duties Violation"));
        assertEquals(ApprovalStatus.PENDING, request.getStatus());
        verify(ledgerService, never()).createJournalEntry(any());
    }

    @Test
    @DisplayName("Should approve request when decided by an authorized independent approver")
    void shouldApproveWhenDecidedByIndependentApprover() {
        User submitter = User.builder().id("user-1").username("analyst").build();
        User approver = User.builder().id("user-2").username("approver_sarah").build();

        Account debitAcc = Account.builder().id("acc-5010").build();
        Account creditAcc = Account.builder().id("acc-1030").build();

        FinancialException exc = FinancialException.builder()
                .id("exc-1")
                .exceptionNumber("EXP-555")
                .status(ExceptionStatus.PENDING_APPROVAL)
                .build();

        ResolutionProposal proposal = ResolutionProposal.builder()
                .id("prop-1")
                .financialException(exc)
                .proposedAction("POST_LEDGER_ADJUSTMENT")
                .adjustmentAmount(new BigDecimal("1200.0000"))
                .debitAccount(debitAcc)
                .creditAccount(creditAcc)
                .rationale("Adjustment test")
                .status(ProposalStatus.SUBMITTED)
                .createdBy(submitter)
                .build();

        ApprovalRequest request = ApprovalRequest.builder()
                .id("appr-1")
                .requestNumber("REQ-101")
                .requestedBy(submitter)
                .resolutionProposal(proposal)
                .status(ApprovalStatus.PENDING)
                .decisions(new ArrayList<>())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(approvalRequestRepository.findById("appr-1")).thenReturn(Optional.of(request));
        when(userRepository.findByUsername("approver_sarah")).thenReturn(Optional.of(approver));
        when(approvalRequestRepository.save(any(ApprovalRequest.class))).thenAnswer(i -> i.getArgument(0));

        ApprovalDtos.SubmitDecisionRequest decisionReq = new ApprovalDtos.SubmitDecisionRequest("APPROVED", "Verified and approved");

        ApprovalDtos.ApprovalRequestResponse response = approvalService.processDecision("appr-1", decisionReq, "approver_sarah");

        assertEquals(ApprovalStatus.APPROVED, response.getStatus());
        verify(ledgerService, times(1)).createJournalEntry(any());
        verify(auditLogService, times(1)).record(any(), any(), eq("APPROVE"), eq("approver_sarah"), any(), any());
    }
}
