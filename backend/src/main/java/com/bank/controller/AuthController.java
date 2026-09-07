package com.bank.controller;

import com.bank.dto.ApiResponse;
import com.bank.dto.AuthResponse;
import com.bank.dto.LoginRequest;
import com.bank.dto.RegisterRequest;
import com.bank.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        ApiResponse<AuthResponse> apiResponse = new ApiResponse<>(true, "User registered successfully", response);
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        ApiResponse<AuthResponse> apiResponse = new ApiResponse<>(true, "User authenticated successfully", response);
        return ResponseEntity.ok(apiResponse);
    }
}
