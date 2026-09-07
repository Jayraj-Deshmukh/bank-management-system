package com.bank.security;

import com.bank.entity.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        // Set generic unit-test secret
        String testSecret = System.getenv("JWT_SECRET") != null && !System.getenv("JWT_SECRET").isBlank()
                ? System.getenv("JWT_SECRET")
                : "TestOnlyDummySecretKeyForUnitTestingPurposesOnly1234567890=";

        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", testSecret);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", 3600000L); // 1 hour
    }

    @Test
    @DisplayName("Should generate valid JWT token with correct email and role claims")
    void testGenerateTokenAndParseClaims() {
        String token = jwtTokenProvider.generateToken("user@example.com", UserRole.CUSTOMER);

        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getEmailFromToken(token)).isEqualTo("user@example.com");
        assertThat(jwtTokenProvider.getRoleFromToken(token)).isEqualTo("CUSTOMER");
    }

    @Test
    @DisplayName("Should return false when validating an invalid or tampered JWT token")
    void testValidateInvalidToken() {
        String invalidToken = "invalid.jwt.token.string";
        assertThat(jwtTokenProvider.validateToken(invalidToken)).isFalse();
    }

    @Test
    @DisplayName("Should return false when validating an expired JWT token")
    void testValidateExpiredToken() {
        // Set expiration to 1ms
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", 1L);
        String token = jwtTokenProvider.generateToken("expired@example.com", UserRole.CUSTOMER);

        try {
            Thread.sleep(10);
        } catch (InterruptedException ignored) {}

        assertThat(jwtTokenProvider.validateToken(token)).isFalse();
    }
}
