package com.bank.controller;

import com.bank.dto.DepositRequest;
import com.bank.dto.WithdrawalRequest;
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
class WithdrawalIntegrationTest {

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

        customer1 = userRepository.save(new User("Alice", "alice.wth@example.com", "111", passwordEncoder.encode("pass"), UserRole.CUSTOMER));
        customer2 = userRepository.save(new User("Bob", "bob.wth@example.com", "222", passwordEncoder.encode("pass"), UserRole.CUSTOMER));

        // Start account with 10,000.00
        account1 = accountRepository.save(new Account("101111111111", AccountType.SAVINGS, new BigDecimal("10000.00"), AccountStatus.ACTIVE, customer1));
        account2 = accountRepository.save(new Account("102222222222", AccountType.CHECKING, new BigDecimal("5000.00"), AccountStatus.ACTIVE, customer2));

        jwt1 = tokenProvider.generateToken("alice.wth@example.com", UserRole.CUSTOMER);
        jwt2 = tokenProvider.generateToken("bob.wth@example.com", UserRole.CUSTOMER);
    }

    @Test
    @DisplayName("1-6. Successful withdrawal decreases balance to 7000 and creates WITHDRAWAL transaction")
    void testSuccessfulWithdrawal() throws Exception {
        WithdrawalRequest request = new WithdrawalRequest(new BigDecimal("3000.00"), "ATM Withdrawal");

        mockMvc.perform(post("/api/accounts/" + account1.getAccountNumber() + "/withdraw")
                .header("Authorization", "Bearer " + jwt1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accountNumber").value("101111111111"))
                .andExpect(jsonPath("$.data.transactionType").value("WITHDRAWAL"))
                .andExpect(jsonPath("$.data.amount").value(3000.00))
                .andExpect(jsonPath("$.data.balanceAfter").value(7000.00))
                .andExpect(jsonPath("$.data.transactionReference").exists());

        Account updatedAccount = accountRepository.findById(account1.getId()).orElseThrow();
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo(new BigDecimal("7000.00"));

        List<Transaction> transactions = transactionRepository.findByAccountId(account1.getId());
        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("3000.00"));
        assertThat(transactions.get(0).getBalanceAfter()).isEqualByComparingTo(new BigDecimal("7000.00"));
    }

    @Test
    @DisplayName("Insufficient balance withdrawal request is rejected (400 Bad Request) and balance unchanged")
    void testInsufficientBalanceWithdrawal() throws Exception {
        WithdrawalRequest request = new WithdrawalRequest(new BigDecimal("15000.00"));

        mockMvc.perform(post("/api/accounts/" + account1.getAccountNumber() + "/withdraw")
                .header("Authorization", "Bearer " + jwt1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Insufficient balance for withdrawal. Current balance: 10000.00, Requested: 15000.00"));

        // Verify balance remains 10000.00 and no transaction was logged
        Account updatedAccount = accountRepository.findById(account1.getId()).orElseThrow();
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo(new BigDecimal("10000.00"));

        List<Transaction> transactions = transactionRepository.findByAccountId(account1.getId());
        assertThat(transactions).isEmpty();
    }

    @Test
    @DisplayName("Zero and negative withdrawal amounts are rejected (400 Bad Request)")
    void testZeroAndNegativeWithdrawalAmounts() throws Exception {
        // Zero amount -> 400
        mockMvc.perform(post("/api/accounts/" + account1.getAccountNumber() + "/withdraw")
                .header("Authorization", "Bearer " + jwt1)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        // Negative amount -> 400
        mockMvc.perform(post("/api/accounts/" + account1.getAccountNumber() + "/withdraw")
                .header("Authorization", "Bearer " + jwt1)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\": -100}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Customer attempting to withdraw from another customer's account returns 403 Forbidden")
    void testUnauthorizedWithdrawal() throws Exception {
        WithdrawalRequest request = new WithdrawalRequest(new BigDecimal("1000.00"));

        // Customer 1 attempting to withdraw from Customer 2's account -> 403 Forbidden
        mockMvc.perform(post("/api/accounts/" + account2.getAccountNumber() + "/withdraw")
                .header("Authorization", "Bearer " + jwt1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated withdrawal request returns 401 Unauthorized")
    void testUnauthenticatedWithdrawal() throws Exception {
        WithdrawalRequest request = new WithdrawalRequest(new BigDecimal("1000.00"));

        mockMvc.perform(post("/api/accounts/" + account1.getAccountNumber() + "/withdraw")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
