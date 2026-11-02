package com.finpilot.core.transaction;

import com.finpilot.core.common.ApiResponse;
import com.finpilot.core.common.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Transaction ingestion, idempotency, lifecycle events, and CSV bulk import")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_ANALYST')")
    @Operation(summary = "Ingest a new transaction with idempotency guarantees")
    public ResponseEntity<ApiResponse<TransactionDtos.TransactionResponse>> createTransaction(
            @Valid @RequestBody TransactionDtos.CreateTransactionRequest request,
            Authentication authentication
    ) {
        String actor = authentication != null ? authentication.getName() : "API_USER";
        TransactionDtos.TransactionResponse response = transactionService.createTransaction(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Transaction ingested successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_ANALYST', 'FINANCE_APPROVER', 'AUDITOR')")
    @Operation(summary = "Search and filter transactions with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<TransactionDtos.TransactionResponse>>> getTransactions(
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) String sourceSystem,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        PagedResponse<TransactionDtos.TransactionResponse> result = transactionService.getTransactions(
                status, sourceSystem, minAmount, maxAmount, search, page, size, sortBy, sortDir
        );
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_ANALYST', 'FINANCE_APPROVER', 'AUDITOR')")
    @Operation(summary = "Get transaction details by ID")
    public ResponseEntity<ApiResponse<TransactionDtos.TransactionResponse>> getTransactionById(@PathVariable String id) {
        TransactionDtos.TransactionResponse response = transactionService.getTransactionById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}/events")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_ANALYST', 'FINANCE_APPROVER', 'AUDITOR')")
    @Operation(summary = "Get complete lifecycle audit events for a transaction")
    public ResponseEntity<ApiResponse<List<TransactionEvent>>> getTransactionEvents(@PathVariable String id) {
        List<TransactionEvent> events = transactionService.getTransactionEvents(id);
        return ResponseEntity.ok(ApiResponse.ok(events));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_ANALYST')")
    @Operation(summary = "Bulk import transactions from a CSV file")
    public ResponseEntity<ApiResponse<TransactionDtos.TransactionImportResult>> importCsv(
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        String actor = authentication != null ? authentication.getName() : "CSV_IMPORTER";
        TransactionDtos.TransactionImportResult result = transactionService.importCsv(file, actor);
        return ResponseEntity.ok(ApiResponse.ok("CSV import completed", result));
    }
}
