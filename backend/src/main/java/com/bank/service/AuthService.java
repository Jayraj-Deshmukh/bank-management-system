package com.bank.service;

import com.bank.dto.AuthResponse;
import com.bank.dto.LoginRequest;
import com.bank.dto.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
