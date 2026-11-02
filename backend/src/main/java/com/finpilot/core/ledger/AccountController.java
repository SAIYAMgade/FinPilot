package com.finpilot.core.ledger;

import com.finpilot.core.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "Ledger Accounts", description = "Chart of accounts and balance management")
public class AccountController {

    private final LedgerService ledgerService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_ANALYST', 'FINANCE_APPROVER', 'AUDITOR')")
    @Operation(summary = "List all accounts in the general ledger")
    public ResponseEntity<ApiResponse<List<LedgerDtos.AccountResponse>>> getAllAccounts() {
        List<LedgerDtos.AccountResponse> accounts = ledgerService.getAllAccounts();
        return ResponseEntity.ok(ApiResponse.ok(accounts));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new general ledger account")
    public ResponseEntity<ApiResponse<LedgerDtos.AccountResponse>> createAccount(
            @Valid @RequestBody LedgerDtos.CreateAccountRequest request
    ) {
        LedgerDtos.AccountResponse response = ledgerService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Account created successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_ANALYST', 'FINANCE_APPROVER', 'AUDITOR')")
    @Operation(summary = "Get account details and current balance by ID")
    public ResponseEntity<ApiResponse<LedgerDtos.AccountResponse>> getAccountById(@PathVariable String id) {
        LedgerDtos.AccountResponse response = ledgerService.getAccountById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
