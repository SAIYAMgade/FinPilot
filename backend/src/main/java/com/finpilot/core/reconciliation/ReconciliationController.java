package com.finpilot.core.reconciliation;

import com.finpilot.core.common.ApiResponse;
import com.finpilot.core.common.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reconciliations")
@RequiredArgsConstructor
@Tag(name = "Reconciliation Engine", description = "4-tier deterministic transaction matching, external statement processing, and discrepancy reporting")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_ANALYST')")
    @Operation(summary = "Upload external statement CSV and create a reconciliation run")
    public ResponseEntity<ApiResponse<ReconciliationDtos.ReconciliationRunResponse>> uploadStatement(
            @RequestParam("file") MultipartFile file
    ) {
        ReconciliationDtos.ReconciliationRunResponse run = reconciliationService.createRun(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Statement uploaded and registered", run));
    }

    @PostMapping("/{id}/execute")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_ANALYST')")
    @Operation(summary = "Execute 4-tier deterministic matching algorithm for reconciliation run")
    public ResponseEntity<ApiResponse<ReconciliationDtos.ReconciliationRunResponse>> executeReconciliation(
            @PathVariable String id,
            @RequestParam(defaultValue = "2") int dateToleranceDays
    ) {
        ReconciliationDtos.ReconciliationRunResponse run = reconciliationService.executeReconciliation(id, dateToleranceDays);
        return ResponseEntity.ok(ApiResponse.ok("Reconciliation completed", run));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_ANALYST', 'FINANCE_APPROVER', 'AUDITOR')")
    @Operation(summary = "List all historical reconciliation runs")
    public ResponseEntity<ApiResponse<List<ReconciliationDtos.ReconciliationRunResponse>>> getAllRuns() {
        List<ReconciliationDtos.ReconciliationRunResponse> runs = reconciliationService.getAllRuns();
        return ResponseEntity.ok(ApiResponse.ok(runs));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_ANALYST', 'FINANCE_APPROVER', 'AUDITOR')")
    @Operation(summary = "Get reconciliation run summary and status by ID")
    public ResponseEntity<ApiResponse<ReconciliationDtos.ReconciliationRunResponse>> getRunById(@PathVariable String id) {
        ReconciliationDtos.ReconciliationRunResponse run = reconciliationService.getRunById(id);
        return ResponseEntity.ok(ApiResponse.ok(run));
    }

    @GetMapping("/{id}/results")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_ANALYST', 'FINANCE_APPROVER', 'AUDITOR')")
    @Operation(summary = "Get match results and discrepancies for a reconciliation run")
    public ResponseEntity<ApiResponse<PagedResponse<ReconciliationDtos.ReconciliationMatchResponse>>> getResults(
            @PathVariable String id,
            @RequestParam(required = false) MatchStatus matchStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PagedResponse<ReconciliationDtos.ReconciliationMatchResponse> matches = reconciliationService.getRunMatches(id, matchStatus, page, size);
        return ResponseEntity.ok(ApiResponse.ok(matches));
    }

    @GetMapping("/{id}/report")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_ANALYST', 'FINANCE_APPROVER', 'AUDITOR')")
    @Operation(summary = "Generate comprehensive audit report for reconciliation run")
    public ResponseEntity<ApiResponse<ReconciliationDtos.ReconciliationReport>> getReport(@PathVariable String id) {
        ReconciliationDtos.ReconciliationReport report = reconciliationService.getReport(id);
        return ResponseEntity.ok(ApiResponse.ok(report));
    }

    @GetMapping(value = "/{id}/export", produces = "text/csv")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_ANALYST', 'FINANCE_APPROVER', 'AUDITOR')")
    @Operation(summary = "Export reconciliation match results to CSV")
    public ResponseEntity<String> exportCsv(@PathVariable String id) {
        ReconciliationDtos.ReconciliationReport report = reconciliationService.getReport(id);

        StringBuilder csv = new StringBuilder();
        csv.append("Match ID,Run ID,Txn ID,External Ref,Match Type,Match Status,Amount Diff,Score,Notes\n");
        for (ReconciliationDtos.ReconciliationMatchResponse m : report.getMatches()) {
            String txnId = m.getTransaction() != null ? m.getTransaction().getId() : "";
            String extRef = m.getExternalRecord() != null ? m.getExternalRecord().getTransactionReference() : "";
            csv.append(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%s,%d,\"%s\"\n",
                    m.getId(), m.getReconciliationRunId(), txnId, extRef,
                    m.getMatchType(), m.getMatchStatus(), m.getAmountDifference(),
                    m.getMatchScore(), m.getNotes() != null ? m.getNotes().replace("\"", "\"\"") : ""));
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDispositionFormData("attachment", "recon-" + report.getRunSummary().getRunNumber() + "-report.csv");
        return new ResponseEntity<>(csv.toString(), headers, HttpStatus.OK);
    }
}
