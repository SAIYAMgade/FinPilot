package com.finpilot.core.exception;

import com.finpilot.core.common.ApiResponse;
import com.finpilot.core.common.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/exceptions")
@RequiredArgsConstructor
@Tag(name = "Exception Management", description = "Financial discrepancies, investigation workflow, and root cause resolution")
public class FinancialExceptionController {

    private final FinancialExceptionService exceptionService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_ANALYST', 'FINANCE_APPROVER', 'AUDITOR')")
    @Operation(summary = "Search and filter exceptions across the organization")
    public ResponseEntity<ApiResponse<PagedResponse<ExceptionDtos.ExceptionResponse>>> getExceptions(
            @RequestParam(required = false) ExceptionStatus status,
            @RequestParam(required = false) ExceptionSeverity severity,
            @RequestParam(required = false) ExceptionCategory category,
            @RequestParam(required = false) String assignedToId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        PagedResponse<ExceptionDtos.ExceptionResponse> result = exceptionService.getExceptions(
                status, severity, category, assignedToId, search, page, size, sortBy, sortDir
        );
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_ANALYST', 'FINANCE_APPROVER', 'AUDITOR')")
    @Operation(summary = "Get exception details, comments, and related financial entities")
    public ResponseEntity<ApiResponse<ExceptionDtos.ExceptionResponse>> getExceptionById(@PathVariable String id) {
        ExceptionDtos.ExceptionResponse response = exceptionService.getExceptionById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_ANALYST')")
    @Operation(summary = "Assign exception case to an analyst")
    public ResponseEntity<ApiResponse<ExceptionDtos.ExceptionResponse>> assignException(
            @PathVariable String id,
            @Valid @RequestBody ExceptionDtos.AssignExceptionRequest request
    ) {
        ExceptionDtos.ExceptionResponse response = exceptionService.assignException(id, request.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Exception assigned successfully", response));
    }

    @PostMapping("/{id}/comments")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_ANALYST', 'FINANCE_APPROVER')")
    @Operation(summary = "Add an investigation comment or evidence note to an exception")
    public ResponseEntity<ApiResponse<ExceptionDtos.CommentResponse>> addComment(
            @PathVariable String id,
            @Valid @RequestBody ExceptionDtos.AddCommentRequest request,
            Authentication authentication
    ) {
        String author = authentication != null ? authentication.getName() : "ANONYMOUS";
        ExceptionDtos.CommentResponse response = exceptionService.addComment(id, request.getComment(), author);
        return ResponseEntity.ok(ApiResponse.ok("Comment added", response));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_ANALYST', 'FINANCE_APPROVER')")
    @Operation(summary = "Update exception resolution status and notes")
    public ResponseEntity<ApiResponse<ExceptionDtos.ExceptionResponse>> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody ExceptionDtos.UpdateStatusRequest request
    ) {
        ExceptionDtos.ExceptionResponse response = exceptionService.updateStatus(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Exception updated", response));
    }
}
