package com.bank.controller;

import com.bank.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class TestController {

    @GetMapping("/api/customer/test")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> customerTestEndpoint() {
        ApiResponse<Map<String, String>> response = new ApiResponse<>(
                true,
                "Customer access granted",
                Map.of("message", "Welcome to Customer Protected Area")
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/admin/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> adminTestEndpoint() {
        ApiResponse<Map<String, String>> response = new ApiResponse<>(
                true,
                "Admin access granted",
                Map.of("message", "Welcome to Admin Protected Area")
        );
        return ResponseEntity.ok(response);
    }
}
