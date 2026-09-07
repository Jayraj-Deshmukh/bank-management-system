package com.bank.dto;

import com.bank.entity.enums.UserRole;

public class AuthResponse {

    private String accessToken;
    private String tokenType = "Bearer";
    private Long userId;
    private String name;
    private String email;
    private UserRole role;

    public AuthResponse() {
    }

    public AuthResponse(String accessToken, Long userId, String name, String email, UserRole role) {
        this.accessToken = accessToken;
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.role = role;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }
}
