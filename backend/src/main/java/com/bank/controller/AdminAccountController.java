package com.bank.controller;

import com.bank.dto.AccountResponse;
import com.bank.dto.ApiResponse;
import com.bank.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/accounts")
public class AdminAccountController {

    private final AccountService accountService;

    public AdminAccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getAllAccounts() {
        List<AccountResponse> response = accountService.getAllAccountsForAdmin();
        ApiResponse<List<AccountResponse>> apiResponse = new ApiResponse<>(true, "System-wide accounts retrieved", response);
        return ResponseEntity.ok(apiResponse);
    }
}
