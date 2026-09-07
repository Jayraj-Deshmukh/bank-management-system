package com.bank.controller;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionHistoryIntegrationTest {

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

        customer1 = userRepository.save(new User("Alice", "alice.hist@example.com", "111", passwordEncoder.encode("pass"), UserRole.CUSTOMER));
        customer2 = userRepository.save(new User("Bob", "bob.hist@example.com", "222", passwordEncoder.encode("pass"), UserRole.CUSTOMER));

        account1 = accountRepository.save(new Account("101111111111", AccountType.SAVINGS, new BigDecimal("5000.00"), AccountStatus.ACTIVE, customer1));
        account2 = accountRepository.save(new Account("102222222222", AccountType.CHECKING, new BigDecimal("2000.00"), AccountStatus.ACTIVE, customer2));

        // Create transactions for account1
        transactionRepository.save(new Transaction("TXD-100", TransactionType.DEPOSIT, new BigDecimal("10000.00"), new BigDecimal("10000.00"), "Deposit 10k", account1));
        transactionRepository.save(new Transaction("TXW-101", TransactionType.WITHDRAWAL, new BigDecimal("3000.00"), new BigDecimal("7000.00"), "Withdraw 3k", account1));
        transactionRepository.save(new Transaction("TXW-102", TransactionType.WITHDRAWAL, new BigDecimal("2000.00"), new BigDecimal("5000.00"), "Withdraw 2k", account1));

        jwt1 = tokenProvider.generateToken("alice.hist@example.com", UserRole.CUSTOMER);
        jwt2 = tokenProvider.generateToken("bob.hist@example.com", UserRole.CUSTOMER);
    }

    @Test
    @DisplayName("1-8. Customer retrieves own transaction history (newest first, 3 items)")
    void testGetTransactionHistorySuccess() throws Exception {
        mockMvc.perform(get("/api/accounts/" + account1.getAccountNumber() + "/transactions")
                .header("Authorization", "Bearer " + jwt1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.content[0].transactionReference").value("TXW-102"))
                .andExpect(jsonPath("$.data.content[1].transactionReference").value("TXW-101"))
                .andExpect(jsonPath("$.data.content[2].transactionReference").value("TXD-100"));
    }

    @Test
    @DisplayName("3 & 9. Pagination works (page=0, size=2 returns first 2 newest items)")
    void testTransactionHistoryPagination() throws Exception {
        mockMvc.perform(get("/api/accounts/" + account1.getAccountNumber() + "/transactions?page=0&size=2")
                .header("Authorization", "Bearer " + jwt1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2));
    }

    @Test
    @DisplayName("9 & 10. Type filtering works for DEPOSIT and WITHDRAWAL")
    void testTransactionHistoryTypeFiltering() throws Exception {
        // DEPOSIT filter -> 1 result
        mockMvc.perform(get("/api/accounts/" + account1.getAccountNumber() + "/transactions?type=DEPOSIT")
                .header("Authorization", "Bearer " + jwt1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].transactionType").value("DEPOSIT"));

        // WITHDRAWAL filter -> 2 results
        mockMvc.perform(get("/api/accounts/" + account1.getAccountNumber() + "/transactions?type=WITHDRAWAL")
                .header("Authorization", "Bearer " + jwt1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].transactionType").value("WITHDRAWAL"));
    }

    @Test
    @DisplayName("10. Invalid transaction type string returns 400 Bad Request")
    void testInvalidTransactionTypeFilter() throws Exception {
        mockMvc.perform(get("/api/accounts/" + account1.getAccountNumber() + "/transactions?type=INVALID_TYPE")
                .header("Authorization", "Bearer " + jwt1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("6 & 12. Empty transaction history returns 200 OK with empty content array")
    void testEmptyTransactionHistory() throws Exception {
        mockMvc.perform(get("/api/accounts/" + account2.getAccountNumber() + "/transactions")
                .header("Authorization", "Bearer " + jwt2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0))
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    @DisplayName("11. Customer attempting to view another customer's transaction history returns 403 Forbidden")
    void testUnauthorizedTransactionHistoryAccess() throws Exception {
        // Customer 2 trying to access Customer 1's history -> 403 Forbidden
        mockMvc.perform(get("/api/accounts/" + account1.getAccountNumber() + "/transactions")
                .header("Authorization", "Bearer " + jwt2))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("12. Unauthenticated request returns 401 Unauthorized")
    void testUnauthenticatedTransactionHistoryAccess() throws Exception {
        mockMvc.perform(get("/api/accounts/" + account1.getAccountNumber() + "/transactions"))
                .andExpect(status().isUnauthorized());
    }
}
