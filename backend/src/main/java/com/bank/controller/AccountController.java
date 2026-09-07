package com.bank.controller;

import com.bank.dto.AccountResponse;
import com.bank.dto.ApiResponse;
import com.bank.dto.CreateAccountRequest;
import com.bank.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        AccountResponse response = accountService.createAccount(request, userEmail);
        ApiResponse<AccountResponse> apiResponse = new ApiResponse<>(true, "Account created successfully", response);
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getMyAccounts() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        List<AccountResponse> response = accountService.getMyAccounts(userEmail);
        ApiResponse<List<AccountResponse>> apiResponse = new ApiResponse<>(true, "Accounts retrieved successfully", response);
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/{accountNumber}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccountByNumber(@PathVariable String accountNumber) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        AccountResponse response = accountService.getAccountByNumber(accountNumber, userEmail, isAdmin);
        ApiResponse<AccountResponse> apiResponse = new ApiResponse<>(true, "Account details retrieved successfully", response);
        return ResponseEntity.ok(apiResponse);
    }
}
