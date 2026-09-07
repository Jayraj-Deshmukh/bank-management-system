package com.bank.controller;

import com.bank.dto.LoginRequest;
import com.bank.dto.RegisterRequest;
import com.bank.entity.User;
import com.bank.entity.enums.UserRole;
import com.bank.repository.AccountRepository;
import com.bank.repository.TransactionRepository;
import com.bank.repository.UserRepository;
import com.bank.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("1. Successful Registration POST /api/auth/register")
    void testSuccessfulRegistration() throws Exception {
        RegisterRequest request = new RegisterRequest("Customer One", "customer1@example.com", "9998887776", "Secret123!");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("customer1@example.com"))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.data.accessToken").exists());

        User user = userRepository.findByEmail("customer1@example.com").orElse(null);
        assertThat(user).isNotNull();
        assertThat(user.getPassword()).isNotEqualTo("Secret123!");
        assertThat(passwordEncoder.matches("Secret123!", user.getPassword())).isTrue();
    }

    @Test
    @DisplayName("2. Duplicate email registration should return 400 Bad Request")
    void testDuplicateEmailRegistration() throws Exception {
        userRepository.save(new User("Existing User", "existing@example.com", "1112223333", passwordEncoder.encode("pass123"), UserRole.CUSTOMER));

        RegisterRequest request = new RegisterRequest("New User", "existing@example.com", "9998887776", "pass123");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email is already registered: existing@example.com"));
    }

    @Test
    @DisplayName("4 & 5. Login with valid and invalid password")
    void testLoginValidAndInvalidPassword() throws Exception {
        userRepository.save(new User("Login User", "login@example.com", "1112223333", passwordEncoder.encode("correctPass123"), UserRole.CUSTOMER));

        LoginRequest invalidRequest = new LoginRequest("login@example.com", "wrongPass");
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));

        LoginRequest validRequest = new LoginRequest("login@example.com", "correctPass123");
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists());
    }

    @Test
    @DisplayName("7. Protected endpoint without JWT should return 401 Unauthorized")
    void testProtectedEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/customer/test"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("9 & 10. CUSTOMER token accessing customer endpoint (200) vs admin endpoint (403)")
    void testCustomerRoleAuthorization() throws Exception {
        userRepository.save(new User("Customer User", "cust@example.com", "12345", passwordEncoder.encode("pass"), UserRole.CUSTOMER));
        String customerJwt = tokenProvider.generateToken("cust@example.com", UserRole.CUSTOMER);

        mockMvc.perform(get("/api/customer/test")
                .header("Authorization", "Bearer " + customerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/admin/test")
                .header("Authorization", "Bearer " + customerJwt))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("11. ADMIN token accessing admin endpoint -> 200 OK")
    void testAdminRoleAuthorization() throws Exception {
        userRepository.save(new User("Admin User", "admin@example.com", "12345", passwordEncoder.encode("pass"), UserRole.ADMIN));
        String adminJwt = tokenProvider.generateToken("admin@example.com", UserRole.ADMIN);

        mockMvc.perform(get("/api/admin/test")
                .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("12. Invalid or tampered JWT on protected endpoint -> 401 Unauthorized")
    void testInvalidJwtProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/customer/test")
                .header("Authorization", "Bearer invalid.tampered.token"))
                .andExpect(status().isUnauthorized());
    }
}
