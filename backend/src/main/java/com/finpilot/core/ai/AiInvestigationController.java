package com.finpilot.core.ai;

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
@RequestMapping("/api/v1/ai/investigations")
@RequiredArgsConstructor
@Tag(name = "AI Investigation Engine", description = "Multi-agent automated discrepancy investigation, root cause deduction, and evidence collection")
public class AiInvestigationController {

    private final AiInvestigationService aiInvestigationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_ANALYST')")
    @Operation(summary = "Trigger multi-agent AI investigation for a financial exception")
    public ResponseEntity<ApiResponse<AiInvestigationDtos.InvestigationResponse>> triggerInvestigation(
            @Valid @RequestBody AiInvestigationDtos.TriggerInvestigationRequest request,
            Authentication authentication
    ) {
        String actor = authentication != null ? authentication.getName() : "ANALYST";
        AiInvestigationDtos.InvestigationResponse response = aiInvestigationService.triggerInvestigation(request.getExceptionId(), actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Investigation launched and completed", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_ANALYST', 'FINANCE_APPROVER', 'AUDITOR')")
    @Operation(summary = "List all historical AI investigations")
    public ResponseEntity<ApiResponse<List<AiInvestigationDtos.InvestigationResponse>>> getAllInvestigations() {
        List<AiInvestigationDtos.InvestigationResponse> list = aiInvestigationService.getAllInvestigations();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_ANALYST', 'FINANCE_APPROVER', 'AUDITOR')")
    @Operation(summary = "Get investigation details, root cause, and recommendations by ID")
    public ResponseEntity<ApiResponse<AiInvestigationDtos.InvestigationResponse>> getInvestigationById(@PathVariable String id) {
        AiInvestigationDtos.InvestigationResponse response = aiInvestigationService.getInvestigationById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}/events")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_ANALYST', 'FINANCE_APPROVER', 'AUDITOR')")
    @Operation(summary = "Get execution event trace and tool calls of individual agents")
    public ResponseEntity<ApiResponse<List<AiInvestigationDtos.AgentExecutionEventDto>>> getInvestigationEvents(@PathVariable String id) {
        List<AiInvestigationDtos.AgentExecutionEventDto> events = aiInvestigationService.getInvestigationEvents(id);
        return ResponseEntity.ok(ApiResponse.ok(events));
    }
}
