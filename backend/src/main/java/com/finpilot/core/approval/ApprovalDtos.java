package com.finpilot.core.approval;

import com.finpilot.core.auth.AuthDtos;
import com.finpilot.core.ledger.LedgerDtos;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class ApprovalDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateProposalRequest {
        @NotBlank(message = "exceptionId is required")
        private String exceptionId;

        private String investigationId;

        @NotBlank(message = "proposedAction is required")
        private String proposedAction;

        @NotNull(message = "adjustmentAmount is required")
        @DecimalMin(value = "0.0001", message = "adjustmentAmount must be greater than zero")
        private BigDecimal adjustmentAmount;

        private String debitAccountId;
        private String creditAccountId;

        @NotBlank(message = "rationale is required")
        private String rationale;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProposalResponse {
        private String id;
        private String investigationId;
        private String exceptionId;
        private String proposedAction;
        private BigDecimal adjustmentAmount;
        private LedgerDtos.AccountResponse debitAccount;
        private LedgerDtos.AccountResponse creditAccount;
        private String rationale;
        private ProposalStatus status;
        private AuthDtos.UserDto createdBy;
        private Instant createdAt;

        public static ProposalResponse fromEntity(ResolutionProposal p) {
            return ProposalResponse.builder()
                    .id(p.getId())
                    .investigationId(p.getInvestigationId())
                    .exceptionId(p.getFinancialException().getId())
                    .proposedAction(p.getProposedAction())
                    .adjustmentAmount(p.getAdjustmentAmount())
                    .debitAccount(p.getDebitAccount() != null ? LedgerDtos.AccountResponse.fromEntity(p.getDebitAccount()) : null)
                    .creditAccount(p.getCreditAccount() != null ? LedgerDtos.AccountResponse.fromEntity(p.getCreditAccount()) : null)
                    .rationale(p.getRationale())
                    .status(p.getStatus())
                    .createdBy(AuthDtos.UserDto.fromEntity(p.getCreatedBy()))
                    .createdAt(p.getCreatedAt())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DecisionResponse {
        private String id;
        private AuthDtos.UserDto decidedBy;
        private String decision;
        private String comments;
        private Instant decidedAt;

        public static DecisionResponse fromEntity(ApprovalDecision d) {
            return DecisionResponse.builder()
                    .id(d.getId())
                    .decidedBy(AuthDtos.UserDto.fromEntity(d.getDecidedBy()))
                    .decision(d.getDecision())
                    .comments(d.getComments())
                    .decidedAt(d.getDecidedAt())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApprovalRequestResponse {
        private String id;
        private String requestNumber;
        private ProposalResponse proposal;
        private AuthDtos.UserDto requestedBy;
        private ApprovalStatus status;
        private List<DecisionResponse> decisions;
        private Instant requestedAt;
        private Instant expiresAt;

        public static ApprovalRequestResponse fromEntity(ApprovalRequest r) {
            List<DecisionResponse> decs = r.getDecisions() != null
                    ? r.getDecisions().stream().map(DecisionResponse::fromEntity).toList()
                    : List.of();

            return ApprovalRequestResponse.builder()
                    .id(r.getId())
                    .requestNumber(r.getRequestNumber())
                    .proposal(ProposalResponse.fromEntity(r.getResolutionProposal()))
                    .requestedBy(AuthDtos.UserDto.fromEntity(r.getRequestedBy()))
                    .status(r.getStatus())
                    .decisions(decs)
                    .requestedAt(r.getRequestedAt())
                    .expiresAt(r.getExpiresAt())
                    .build();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmitDecisionRequest {
        @NotBlank(message = "decision is required (APPROVED or REJECTED)")
        private String decision;
        private String comments;
    }
}
