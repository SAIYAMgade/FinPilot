package com.finpilot.core.ai;

import com.finpilot.core.auth.AuthDtos;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class AiInvestigationDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TriggerInvestigationRequest {
        @NotBlank(message = "exceptionId is required")
        private String exceptionId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgentExecutionEventDto {
        private String id;
        private String agentName;
        private String stepName;
        private String toolCalled;
        private String toolInput;
        private String toolOutput;
        private long executionDurationMs;
        private Instant createdAt;

        public static AgentExecutionEventDto fromEntity(AgentExecutionEvent e) {
            return AgentExecutionEventDto.builder()
                    .id(e.getId())
                    .agentName(e.getAgentName())
                    .stepName(e.getStepName())
                    .toolCalled(e.getToolCalled())
                    .toolInput(e.getToolInput())
                    .toolOutput(e.getToolOutput())
                    .executionDurationMs(e.getExecutionDurationMs())
                    .createdAt(e.getCreatedAt())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvestigationResponse {
        private String id;
        private String exceptionId;
        private String exceptionNumber;
        private String status;
        private AuthDtos.UserDto initiatedBy;
        private String summary;
        private String rootCauseAnalysis;
        private String recommendedAction;
        private BigDecimal confidenceScore;
        private String evidencePayload;
        private String policyValidationNotes;
        private List<AgentExecutionEventDto> events;
        private Instant createdAt;
        private Instant completedAt;

        public static InvestigationResponse fromEntity(Investigation inv) {
            List<AgentExecutionEventDto> eventDtos = inv.getEvents() != null
                    ? inv.getEvents().stream().map(AgentExecutionEventDto::fromEntity).toList()
                    : List.of();

            return InvestigationResponse.builder()
                    .id(inv.getId())
                    .exceptionId(inv.getFinancialException().getId())
                    .exceptionNumber(inv.getFinancialException().getExceptionNumber())
                    .status(inv.getStatus())
                    .initiatedBy(AuthDtos.UserDto.fromEntity(inv.getInitiatedBy()))
                    .summary(inv.getSummary())
                    .rootCauseAnalysis(inv.getRootCauseAnalysis())
                    .recommendedAction(inv.getRecommendedAction())
                    .confidenceScore(inv.getConfidenceScore())
                    .evidencePayload(inv.getEvidencePayload())
                    .policyValidationNotes(inv.getPolicyValidationNotes())
                    .events(eventDtos)
                    .createdAt(inv.getCreatedAt())
                    .completedAt(inv.getCompletedAt())
                    .build();
        }
    }
}
