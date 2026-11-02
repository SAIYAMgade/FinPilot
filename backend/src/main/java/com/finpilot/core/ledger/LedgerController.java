package com.finpilot.core.ledger;

import com.finpilot.core.common.ApiResponse;
import com.finpilot.core.common.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ledger")
@RequiredArgsConstructor
@Tag(name = "Ledger Journals & Trial Balance", description = "Double-entry journal operations, postings, reversals, and trial balance calculations")
public class LedgerController {

    private final LedgerService ledgerService;

    @GetMapping("/journals")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_ANALYST', 'FINANCE_APPROVER', 'AUDITOR')")
    @Operation(summary = "Get journal entries with pagination and status filter")
    public ResponseEntity<ApiResponse<PagedResponse<LedgerDtos.JournalEntryResponse>>> getJournals(
            @RequestParam(required = false) PostingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<LedgerDtos.JournalEntryResponse> journalPage = ledgerService.getJournals(status, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(journalPage)));
    }

    @GetMapping("/journals/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_ANALYST', 'FINANCE_APPROVER', 'AUDITOR')")
    @Operation(summary = "Get single journal entry with its line items")
    public ResponseEntity<ApiResponse<LedgerDtos.JournalEntryResponse>> getJournalById(@PathVariable String id) {
        LedgerDtos.JournalEntryResponse response = ledgerService.getJournalById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/journals")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_ANALYST')")
    @Operation(summary = "Create a balanced double-entry journal entry")
    public ResponseEntity<ApiResponse<LedgerDtos.JournalEntryResponse>> createJournal(
            @Valid @RequestBody LedgerDtos.CreateJournalEntryRequest request
    ) {
        LedgerDtos.JournalEntryResponse response = ledgerService.createJournalEntry(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Journal entry created", response));
    }

    @PostMapping("/journals/{id}/post")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_APPROVER')")
    @Operation(summary = "Post a draft journal entry and update account balances")
    public ResponseEntity<ApiResponse<LedgerDtos.JournalEntryResponse>> postJournal(@PathVariable String id) {
        LedgerDtos.JournalEntryResponse response = ledgerService.postJournalEntry(id);
        return ResponseEntity.ok(ApiResponse.ok("Journal entry posted to ledger", response));
    }

    @PostMapping("/journals/{id}/reverse")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_APPROVER')")
    @Operation(summary = "Reverse a posted journal entry with an immutable offsetting entry")
    public ResponseEntity<ApiResponse<LedgerDtos.JournalEntryResponse>> reverseJournal(
            @PathVariable String id,
            @RequestParam(required = false) String reason
    ) {
        LedgerDtos.JournalEntryResponse response = ledgerService.reverseJournalEntry(id, reason);
        return ResponseEntity.ok(ApiResponse.ok("Reversal entry created and posted", response));
    }

    @GetMapping("/trial-balance")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_ANALYST', 'FINANCE_APPROVER', 'AUDITOR')")
    @Operation(summary = "Generate a real-time trial balance report")
    public ResponseEntity<ApiResponse<LedgerDtos.TrialBalanceReport>> getTrialBalance() {
        LedgerDtos.TrialBalanceReport report = ledgerService.getTrialBalance();
        return ResponseEntity.ok(ApiResponse.ok(report));
    }
}
