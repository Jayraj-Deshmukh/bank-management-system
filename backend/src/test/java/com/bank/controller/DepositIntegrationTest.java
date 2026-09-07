package com.bank.controller;

import com.bank.dto.DepositRequest;
import com.bank.entity.Account;
import com.bank.entity.Transaction;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DepositIntegrationTest {

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
    private Account account1;
    private Account account2;
    private String jwt1;
    private String jwt2;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();

        customer1 = userRepository.save(new User("Alice", "alice.dep@example.com", "111", passwordEncoder.encode("pass"), UserRole.CUSTOMER));
        customer2 = userRepository.save(new User("Bob", "bob.dep@example.com", "222", passwordEncoder.encode("pass"), UserRole.CUSTOMER));

        account1 = accountRepository.save(new Account("101111111111", AccountType.SAVINGS, BigDecimal.ZERO, AccountStatus.ACTIVE, customer1));
        account2 = accountRepository.save(new Account("102222222222", AccountType.CHECKING, BigDecimal.ZERO, AccountStatus.ACTIVE, customer2));

        jwt1 = tokenProvider.generateToken("alice.dep@example.com", UserRole.CUSTOMER);
        jwt2 = tokenProvider.generateToken("bob.dep@example.com", UserRole.CUSTOMER);
    }

    @Test
    @DisplayName("1-6. Deposit 5000 increases balance to 5000 and creates DEPOSIT transaction")
    void testSuccessfulDeposit() throws Exception {
        DepositRequest request = new DepositRequest(new BigDecimal("5000.00"), "Initial deposit");

        mockMvc.perform(post("/api/accounts/" + account1.getAccountNumber() + "/deposit")
                .header("Authorization", "Bearer " + jwt1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accountNumber").value("101111111111"))
                .andExpect(jsonPath("$.data.transactionType").value("DEPOSIT"))
                .andExpect(jsonPath("$.data.amount").value(5000.00))
                .andExpect(jsonPath("$.data.balanceAfter").value(5000.00))
                .andExpect(jsonPath("$.data.transactionReference").exists());

        Account updatedAccount = accountRepository.findById(account1.getId()).orElseThrow();
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo(new BigDecimal("5000.00"));

        List<Transaction> transactions = transactionRepository.findByAccountId(account1.getId());
        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(transactions.get(0).getBalanceAfter()).isEqualByComparingTo(new BigDecimal("5000.00"));
    }

    @Test
    @DisplayName("Sequential deposits accumulate balance (5000 + 2000 = 7000)")
    void testSequentialDeposits() throws Exception {
        DepositRequest req1 = new DepositRequest(new BigDecimal("5000.00"));
        DepositRequest req2 = new DepositRequest(new BigDecimal("2000.00"));

        mockMvc.perform(post("/api/accounts/" + account1.getAccountNumber() + "/deposit")
                .header("Authorization", "Bearer " + jwt1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/accounts/" + account1.getAccountNumber() + "/deposit")
                .header("Authorization", "Bearer " + jwt1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balanceAfter").value(7000.00));

        Account updatedAccount = accountRepository.findById(account1.getId()).orElseThrow();
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo(new BigDecimal("7000.00"));
    }

    @Test
    @DisplayName("7 & 8. Zero and negative deposit amounts are rejected (400 Bad Request)")
    void testZeroAndNegativeAmounts() throws Exception {
        // Zero amount -> 400
        mockMvc.perform(post("/api/accounts/" + account1.getAccountNumber() + "/deposit")
                .header("Authorization", "Bearer " + jwt1)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        // Negative amount -> 400
        mockMvc.perform(post("/api/accounts/" + account1.getAccountNumber() + "/deposit")
                .header("Authorization", "Bearer " + jwt1)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\": -500}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("9. Missing deposit amount is rejected (400 Bad Request)")
    void testMissingAmount() throws Exception {
        mockMvc.perform(post("/api/accounts/" + account1.getAccountNumber() + "/deposit")
                .header("Authorization", "Bearer " + jwt1)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("10. Deposit to non-existent account returns 404 Not Found")
    void testNonExistentAccountDeposit() throws Exception {
        DepositRequest req = new DepositRequest(new BigDecimal("100.00"));

        mockMvc.perform(post("/api/accounts/109999999999/deposit")
                .header("Authorization", "Bearer " + jwt1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("11. Customer attempting to deposit into another customer's account returns 403 Forbidden")
    void testUnauthorizedDeposit() throws Exception {
        DepositRequest req = new DepositRequest(new BigDecimal("100.00"));

        // Customer 1 attempting to deposit into Customer 2's account -> 403 Forbidden
        mockMvc.perform(post("/api/accounts/" + account2.getAccountNumber() + "/deposit")
                .header("Authorization", "Bearer " + jwt1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("12. Unauthenticated deposit request returns 401 Unauthorized")
    void testUnauthenticatedDeposit() throws Exception {
        DepositRequest req = new DepositRequest(new BigDecimal("100.00"));

        mockMvc.perform(post("/api/accounts/" + account1.getAccountNumber() + "/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("13. Deposit into frozen account returns 400 Bad Request")
    void testFrozenAccountDeposit() throws Exception {
        Account frozenAccount = accountRepository.save(new Account("103333333333", AccountType.SAVINGS, BigDecimal.ZERO, AccountStatus.FROZEN, customer1));
        DepositRequest req = new DepositRequest(new BigDecimal("100.00"));

        mockMvc.perform(post("/api/accounts/" + frozenAccount.getAccountNumber() + "/deposit")
                .header("Authorization", "Bearer " + jwt1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot deposit to an account with status: FROZEN"));
    }
}
