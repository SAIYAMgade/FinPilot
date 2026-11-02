package com.finpilot.core.dashboard;

import com.finpilot.core.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard & Analytics", description = "Real-time key performance indicators, discrepancy metrics, and trend charts")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_ANALYST', 'FINANCE_APPROVER', 'AUDITOR')")
    @Operation(summary = "Get aggregated real-time financial metrics and analytics")
    public ResponseEntity<ApiResponse<DashboardDtos.DashboardAnalyticsResponse>> getDashboard() {
        DashboardDtos.DashboardAnalyticsResponse data = dashboardService.getDashboardData();
        return ResponseEntity.ok(ApiResponse.ok(data));
    }
}
