package com.bank.controller;

import com.bank.dto.CreateAccountRequest;
import com.bank.entity.Account;
import com.bank.entity.User;
import com.bank.entity.enums.AccountStatus;
import com.bank.entity.enums.AccountType;
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

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccountIntegrationTest {

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

    private User customer1;
    private User customer2;
    private User admin;
    private String jwt1;
    private String jwt2;
    private String adminJwt;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();

        customer1 = userRepository.save(new User("Cust One", "cust1@example.com", "111", passwordEncoder.encode("pass"), UserRole.CUSTOMER));
        customer2 = userRepository.save(new User("Cust Two", "cust2@example.com", "222", passwordEncoder.encode("pass"), UserRole.CUSTOMER));
        admin = userRepository.save(new User("Admin User", "admin@example.com", "999", passwordEncoder.encode("pass"), UserRole.ADMIN));

        jwt1 = tokenProvider.generateToken("cust1@example.com", UserRole.CUSTOMER);
        jwt2 = tokenProvider.generateToken("cust2@example.com", UserRole.CUSTOMER);
        adminJwt = tokenProvider.generateToken("admin@example.com", UserRole.ADMIN);
    }

    @Test
    @DisplayName("1. Customer creates SAVINGS account successfully (0 balance, server-generated account number)")
    void testCreateAccountSuccess() throws Exception {
        CreateAccountRequest request = new CreateAccountRequest(AccountType.SAVINGS);

        mockMvc.perform(post("/api/accounts")
                .header("Authorization", "Bearer " + jwt1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accountType").value("SAVINGS"))
                .andExpect(jsonPath("$.data.balance").value(0))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.accountNumber").exists());

        Account account = accountRepository.findByUserId(customer1.getId()).get(0);
        assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(account.getAccountNumber()).hasSize(12);
    }

    @Test
    @DisplayName("6. Customer can retrieve their own accounts GET /api/accounts/my")
    void testGetMyAccounts() throws Exception {
        accountRepository.save(new Account("101111111111", AccountType.SAVINGS, BigDecimal.ZERO, AccountStatus.ACTIVE, customer1));

        mockMvc.perform(get("/api/accounts/my")
                .header("Authorization", "Bearer " + jwt1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].accountNumber").value("101111111111"));
    }

    @Test
    @DisplayName("8 & 9. Customer accessing own account (200) vs another customer's account (403)")
    void testGetAccountByNumberOwnership() throws Exception {
        Account acc2 = accountRepository.save(new Account("102222222222", AccountType.CHECKING, BigDecimal.ZERO, AccountStatus.ACTIVE, customer2));

        mockMvc.perform(get("/api/accounts/" + acc2.getAccountNumber())
                .header("Authorization", "Bearer " + jwt1))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/accounts/" + acc2.getAccountNumber())
                .header("Authorization", "Bearer " + jwt2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountNumber").value("102222222222"));
    }

    @Test
    @DisplayName("10. Unauthenticated account request should return 401 Unauthorized")
    void testUnauthenticatedAccountRequest() throws Exception {
        mockMvc.perform(get("/api/accounts/my"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("11 & 12. Admin accessing GET /api/admin/accounts (200) vs Customer attempting admin endpoint (403)")
    void testAdminAccountListingAuthorization() throws Exception {
        accountRepository.save(new Account("101111111111", AccountType.SAVINGS, BigDecimal.ZERO, AccountStatus.ACTIVE, customer1));
        accountRepository.save(new Account("102222222222", AccountType.CHECKING, BigDecimal.ZERO, AccountStatus.ACTIVE, customer2));

        mockMvc.perform(get("/api/admin/accounts")
                .header("Authorization", "Bearer " + jwt1))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/accounts")
                .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2));
    }

    @Test
    @DisplayName("13. Request with invalid/null account type is rejected (400 Bad Request)")
    void testInvalidAccountType() throws Exception {
        mockMvc.perform(post("/api/accounts")
                .header("Authorization", "Bearer " + jwt1)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
