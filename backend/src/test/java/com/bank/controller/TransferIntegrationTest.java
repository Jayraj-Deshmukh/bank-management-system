package com.bank.controller;

import com.bank.dto.TransferRequest;
import com.bank.entity.Account;
import com.bank.entity.Transaction;
import com.bank.entity.User;
import com.bank.entity.enums.AccountStatus;
import com.bank.entity.enums.AccountType;
import com.bank.entity.enums.TransactionType;
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
class TransferIntegrationTest {

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

    private User customerA;
    private User customerB;
    private Account accountA;
    private Account accountB;
    private String jwtA;
    private String jwtB;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();

        customerA = userRepository.save(new User("Alice", "alice.tr@example.com", "111", passwordEncoder.encode("pass"), UserRole.CUSTOMER));
        customerB = userRepository.save(new User("Bob", "bob.tr@example.com", "222", passwordEncoder.encode("pass"), UserRole.CUSTOMER));

        accountA = accountRepository.save(new Account("101111111111", AccountType.SAVINGS, new BigDecimal("10000.00"), AccountStatus.ACTIVE, customerA));
        accountB = accountRepository.save(new Account("102222222222", AccountType.CHECKING, new BigDecimal("2000.00"), AccountStatus.ACTIVE, customerB));

        jwtA = tokenProvider.generateToken("alice.tr@example.com", UserRole.CUSTOMER);
        jwtB = tokenProvider.generateToken("bob.tr@example.com", UserRole.CUSTOMER);
    }

    @Test
    @DisplayName("1. CUSTOMER can transfer from their own account")
    void testSuccessfulTransfer() throws Exception {
        TransferRequest request = new TransferRequest(accountA.getAccountNumber(), accountB.getAccountNumber(), new BigDecimal("2000.00"), "Fund Transfer");

        mockMvc.perform(post("/api/accounts/transfer")
                .header("Authorization", "Bearer " + jwtA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fromAccount").value("101111111111"))
                .andExpect(jsonPath("$.data.toAccount").value("102222222222"))
                .andExpect(jsonPath("$.data.amount").value(2000.00))
                .andExpect(jsonPath("$.data.senderBalanceAfter").value(8000.00));

        Account updatedA = accountRepository.findById(accountA.getId()).orElseThrow();
        Account updatedB = accountRepository.findById(accountB.getId()).orElseThrow();

        assertThat(updatedA.getBalance()).isEqualByComparingTo(new BigDecimal("8000.00"));
        assertThat(updatedB.getBalance()).isEqualByComparingTo(new BigDecimal("4000.00"));

        List<Transaction> txA = transactionRepository.findByAccountId(accountA.getId());
        List<Transaction> txB = transactionRepository.findByAccountId(accountB.getId());

        assertThat(txA).hasSize(1);
        assertThat(txA.get(0).getTransactionType()).isEqualTo(TransactionType.TRANSFER_OUT);

        assertThat(txB).hasSize(1);
        assertThat(txB.get(0).getTransactionType()).isEqualTo(TransactionType.TRANSFER_IN);
    }

    @Test
    @DisplayName("2. ADMIN attempting to initiate transfer returns 403 Forbidden")
    void testAdminCannotInitiateTransfer() throws Exception {
        userRepository.save(new User("Admin", "admin.tr@example.com", "999", passwordEncoder.encode("pass"), UserRole.ADMIN));
        String adminJwt = tokenProvider.generateToken("admin.tr@example.com", UserRole.ADMIN);

        TransferRequest request = new TransferRequest(accountA.getAccountNumber(), accountB.getAccountNumber(), new BigDecimal("1000.00"));

        mockMvc.perform(post("/api/accounts/transfer")
                .header("Authorization", "Bearer " + adminJwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("3. Unauthenticated transfer request returns 401 Unauthorized")
    void testUnauthenticatedTransfer() throws Exception {
        TransferRequest request = new TransferRequest(accountA.getAccountNumber(), accountB.getAccountNumber(), new BigDecimal("1000.00"));

        mockMvc.perform(post("/api/accounts/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("4. Customer attempting to transfer from another customer's source account returns 403 Forbidden")
    void testUnauthorizedSenderTransfer() throws Exception {
        TransferRequest request = new TransferRequest(accountA.getAccountNumber(), accountB.getAccountNumber(), new BigDecimal("1000.00"));

        mockMvc.perform(post("/api/accounts/transfer")
                .header("Authorization", "Bearer " + jwtB)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Self-transfer request is rejected (400 Bad Request)")
    void testSelfTransferRejection() throws Exception {
        TransferRequest request = new TransferRequest(accountA.getAccountNumber(), accountA.getAccountNumber(), new BigDecimal("500.00"));

        mockMvc.perform(post("/api/accounts/transfer")
                .header("Authorization", "Bearer " + jwtA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Transfer with insufficient sender balance is rejected (400 Bad Request)")
    void testInsufficientBalanceTransfer() throws Exception {
        TransferRequest request = new TransferRequest(accountA.getAccountNumber(), accountB.getAccountNumber(), new BigDecimal("15000.00"));

        mockMvc.perform(post("/api/accounts/transfer")
                .header("Authorization", "Bearer " + jwtA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        Account updatedA = accountRepository.findById(accountA.getId()).orElseThrow();
        Account updatedB = accountRepository.findById(accountB.getId()).orElseThrow();
        assertThat(updatedA.getBalance()).isEqualByComparingTo(new BigDecimal("10000.00"));
        assertThat(updatedB.getBalance()).isEqualByComparingTo(new BigDecimal("2000.00"));
    }
}
