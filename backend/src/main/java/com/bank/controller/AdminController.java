package com.bank.controller;

import com.bank.dto.*;
import com.bank.entity.enums.TransactionType;
import com.bank.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/customers")
    public ResponseEntity<ApiResponse<PageResponse<UserDTO>>> getAllCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResponse<UserDTO> response = adminService.getAllCustomers(page, size);
        ApiResponse<PageResponse<UserDTO>> apiResponse = new ApiResponse<>(true, "Customers retrieved successfully", response);
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/accounts")
    public ResponseEntity<ApiResponse<PageResponse<AccountResponse>>> getAllAccounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResponse<AccountResponse> response = adminService.getAllAccounts(page, size);
        ApiResponse<PageResponse<AccountResponse>> apiResponse = new ApiResponse<>(true, "Accounts retrieved successfully", response);
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/accounts/{accountNumber}")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccountDetails(@PathVariable String accountNumber) {
        AccountResponse response = adminService.getAccountDetails(accountNumber);
        ApiResponse<AccountResponse> apiResponse = new ApiResponse<>(true, "Account details retrieved successfully", response);
        return ResponseEntity.ok(apiResponse);
    }

    @PatchMapping("/accounts/{accountNumber}/status")
    public ResponseEntity<ApiResponse<AccountResponse>> updateAccountStatus(
            @PathVariable String accountNumber,
            @Valid @RequestBody UpdateAccountStatusRequest request) {

        AccountResponse response = adminService.updateAccountStatus(accountNumber, request);
        ApiResponse<AccountResponse> apiResponse = new ApiResponse<>(true, "Account status updated successfully", response);
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<PageResponse<TransactionResponse>>> getAllTransactions(
            @RequestParam(required = false) String accountNumber,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageResponse<TransactionResponse> response = adminService.getAllTransactions(accountNumber, type, from, to, page, size);
        ApiResponse<PageResponse<TransactionResponse>> apiResponse = new ApiResponse<>(true, "Transactions retrieved successfully", response);
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/transactions/{transactionReference}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransactionDetails(@PathVariable String transactionReference) {
        TransactionResponse response = adminService.getTransactionDetails(transactionReference);
        ApiResponse<TransactionResponse> apiResponse = new ApiResponse<>(true, "Transaction details retrieved successfully", response);
        return ResponseEntity.ok(apiResponse);
    }
}
