package com.finpilot.core.exception;

import com.finpilot.core.auth.AuthDtos;
import com.finpilot.core.transaction.TransactionDtos;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class ExceptionDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExceptionResponse {
        private String id;
        private String exceptionNumber;
        private TransactionDtos.TransactionResponse transaction;
        private String reconciliationRunId;
        private ExceptionCategory category;
        private ExceptionSeverity severity;
        private String description;
        private BigDecimal financialImpact;
        private ExceptionStatus status;
        private AuthDtos.UserDto assignedTo;
        private String rootCause;
        private String resolutionNotes;
        private List<CommentResponse> comments;
        private Instant createdAt;
        private Instant updatedAt;

        public static ExceptionResponse fromEntity(FinancialException e) {
            TransactionDtos.TransactionResponse txnDto = null;
            if (e.getTransaction() != null) {
                txnDto = TransactionDtos.TransactionResponse.fromEntity(e.getTransaction());
            }

            AuthDtos.UserDto userDto = null;
            if (e.getAssignedTo() != null) {
                userDto = AuthDtos.UserDto.fromEntity(e.getAssignedTo());
            }

            List<CommentResponse> commentList = e.getComments() != null
                    ? e.getComments().stream().map(CommentResponse::fromEntity).toList()
                    : List.of();

            return ExceptionResponse.builder()
                    .id(e.getId())
                    .exceptionNumber(e.getExceptionNumber())
                    .transaction(txnDto)
                    .reconciliationRunId(e.getReconciliationRunId())
                    .category(e.getCategory())
                    .severity(e.getSeverity())
                    .description(e.getDescription())
                    .financialImpact(e.getFinancialImpact())
                    .status(e.getStatus())
                    .assignedTo(userDto)
                    .rootCause(e.getRootCause())
                    .resolutionNotes(e.getResolutionNotes())
                    .comments(commentList)
                    .createdAt(e.getCreatedAt())
                    .updatedAt(e.getUpdatedAt())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommentResponse {
        private String id;
        private String exceptionId;
        private AuthDtos.UserDto author;
        private String commentText;
        private Instant createdAt;

        public static CommentResponse fromEntity(ExceptionComment c) {
            return CommentResponse.builder()
                    .id(c.getId())
                    .exceptionId(c.getFinancialException().getId())
                    .author(AuthDtos.UserDto.fromEntity(c.getAuthor()))
                    .commentText(c.getCommentText())
                    .createdAt(c.getCreatedAt())
                    .build();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddCommentRequest {
        @NotBlank(message = "Comment text is required")
        private String comment;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignExceptionRequest {
        @NotBlank(message = "userId is required")
        private String userId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateStatusRequest {
        private ExceptionStatus status;
        private String rootCause;
        private String resolutionNotes;
    }
}
