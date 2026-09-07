package com.bank.controller;

import com.bank.dto.*;
import com.bank.entity.enums.TransactionType;
import com.bank.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/accounts")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/{accountNumber}/deposit")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TransactionResponse>> deposit(
            @PathVariable String accountNumber,
            @Valid @RequestBody DepositRequest request) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        TransactionResponse response = transactionService.deposit(accountNumber, request, userEmail, isAdmin);
        ApiResponse<TransactionResponse> apiResponse = new ApiResponse<>(true, "Deposit completed successfully", response);
        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/{accountNumber}/withdraw")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TransactionResponse>> withdraw(
            @PathVariable String accountNumber,
            @Valid @RequestBody WithdrawalRequest request) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        TransactionResponse response = transactionService.withdraw(accountNumber, request, userEmail, isAdmin);
        ApiResponse<TransactionResponse> apiResponse = new ApiResponse<>(true, "Withdrawal completed successfully", response);
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/{accountNumber}/transactions")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<TransactionResponse>>> getTransactionHistory(
            @PathVariable String accountNumber,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        PageResponse<TransactionResponse> response = transactionService.getTransactionHistory(
                accountNumber, type, from, to, page, size, userEmail, isAdmin
        );

        ApiResponse<PageResponse<TransactionResponse>> apiResponse = new ApiResponse<>(
                true, "Transaction history retrieved successfully", response
        );
        return ResponseEntity.ok(apiResponse);
    }
}
