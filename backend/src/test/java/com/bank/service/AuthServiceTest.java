package com.bank.service;

import com.bank.dto.AuthResponse;
import com.bank.dto.LoginRequest;
import com.bank.dto.RegisterRequest;
import com.bank.entity.User;
import com.bank.entity.enums.UserRole;
import com.bank.exception.DuplicateEmailException;
import com.bank.repository.UserRepository;
import com.bank.security.JwtTokenProvider;
import com.bank.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User savedUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest("Test User", "test@example.com", "1234567890", "plainPassword123");
        loginRequest = new LoginRequest("test@example.com", "plainPassword123");
        savedUser = new User("Test User", "test@example.com", "1234567890", "$2a$12$hashedPassword", UserRole.CUSTOMER);
        savedUser.setId(1L);
    }

    @Test
    @DisplayName("Should successfully register a new customer and hash the password")
    void testRegisterSuccess() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plainPassword123")).thenReturn("$2a$12$hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(tokenProvider.generateToken("test@example.com", UserRole.CUSTOMER)).thenReturn("mocked.jwt.token");

        AuthResponse response = authService.register(registerRequest);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("mocked.jwt.token");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getRole()).isEqualTo(UserRole.CUSTOMER);

        verify(passwordEncoder).encode("plainPassword123");
        verify(userRepository).save(argThat(user ->
                user.getPassword().equals("$2a$12$hashedPassword") &&
                user.getRole() == UserRole.CUSTOMER
        ));
    }

    @Test
    @DisplayName("Should throw DuplicateEmailException when registering an existing email")
    void testRegisterDuplicateEmail() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> authService.register(registerRequest));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully authenticate valid credentials and return JWT token")
    void testLoginSuccess() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(savedUser));
        when(tokenProvider.generateToken("test@example.com", UserRole.CUSTOMER)).thenReturn("mocked.jwt.token");

        AuthResponse response = authService.login(loginRequest);

        assertThat(response.getAccessToken()).isEqualTo("mocked.jwt.token");
        assertThat(response.getEmail()).isEqualTo("test@example.com");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when login password is invalid")
    void testLoginInvalidPassword() {
        doThrow(new BadCredentialsException("Invalid password"))
                .when(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));
    }
}
