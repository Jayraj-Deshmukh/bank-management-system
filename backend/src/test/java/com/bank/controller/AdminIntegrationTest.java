package com.bank.controller;

import com.bank.dto.DepositRequest;
import com.bank.dto.TransferRequest;
import com.bank.dto.UpdateAccountStatusRequest;
import com.bank.dto.WithdrawalRequest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminIntegrationTest {

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
    private Account account1;
    private Account account2;
    private Transaction tx1;
    private String customerJwt;
    private String adminJwt;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();

        customer1 = userRepository.save(new User("Alice Cust", "alice.adm@example.com", "111", passwordEncoder.encode("pass"), UserRole.CUSTOMER));
        customer2 = userRepository.save(new User("Bob Cust", "bob.adm@example.com", "222", passwordEncoder.encode("pass"), UserRole.CUSTOMER));
        admin = userRepository.save(new User("Admin Chief", "admin.chief@example.com", "999", passwordEncoder.encode("pass"), UserRole.ADMIN));

        account1 = accountRepository.save(new Account("101111111111", AccountType.SAVINGS, new BigDecimal("10000.00"), AccountStatus.ACTIVE, customer1));
        account2 = accountRepository.save(new Account("102222222222", AccountType.CHECKING, new BigDecimal("5000.00"), AccountStatus.ACTIVE, customer2));

        tx1 = transactionRepository.save(new Transaction("TXD-ADMIN-100", TransactionType.DEPOSIT, new BigDecimal("10000.00"), new BigDecimal("10000.00"), "Deposit", account1));

        customerJwt = tokenProvider.generateToken("alice.adm@example.com", UserRole.CUSTOMER);
        adminJwt = tokenProvider.generateToken("admin.chief@example.com", UserRole.ADMIN);
    }

    @Test
    @DisplayName("1. Admin can list all customers GET /api/admin/customers")
    void testAdminListCustomers() throws Exception {
        mockMvc.perform(get("/api/admin/customers")
                .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(2));
    }

    @Test
    @DisplayName("2. Admin can list all accounts GET /api/admin/accounts")
    void testAdminListAccounts() throws Exception {
        mockMvc.perform(get("/api/admin/accounts")
                .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(2));
    }

    @Test
    @DisplayName("3. Admin can view account details GET /api/admin/accounts/{accountNumber}")
    void testAdminGetAccountDetails() throws Exception {
        mockMvc.perform(get("/api/admin/accounts/" + account1.getAccountNumber())
                .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountNumber").value("101111111111"));
    }

    @Test
    @DisplayName("4 & 5. Admin can deactivate and activate account status PATCH /api/admin/accounts/{accountNumber}/status")
    void testAdminUpdateAccountStatus() throws Exception {
        UpdateAccountStatusRequest request = new UpdateAccountStatusRequest(AccountStatus.INACTIVE);

        mockMvc.perform(patch("/api/admin/accounts/" + account1.getAccountNumber() + "/status")
                .header("Authorization", "Bearer " + adminJwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        Account updated = accountRepository.findById(account1.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(AccountStatus.INACTIVE);

        // Reactivate
        request.setStatus(AccountStatus.ACTIVE);
        mockMvc.perform(patch("/api/admin/accounts/" + account1.getAccountNumber() + "/status")
                .header("Authorization", "Bearer " + adminJwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("6, 7, 8, 9. Admin can list, paginate, and filter transactions")
    void testAdminTransactionsListingAndFiltering() throws Exception {
        mockMvc.perform(get("/api/admin/transactions?type=DEPOSIT")
                .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].transactionReference").value("TXD-ADMIN-100"));
    }

    @Test
    @DisplayName("9. Admin can view transaction details by reference")
    void testAdminGetTransactionDetails() throws Exception {
        mockMvc.perform(get("/api/admin/transactions/" + tx1.getTransactionReference())
                .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transactionReference").value("TXD-ADMIN-100"));
    }

    @Test
    @DisplayName("10-15. CUSTOMER user receives 403 Forbidden for all ADMIN endpoints")
    void testCustomerForbiddenOnAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/customers").header("Authorization", "Bearer " + customerJwt)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/accounts").header("Authorization", "Bearer " + customerJwt)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/accounts/" + account1.getAccountNumber()).header("Authorization", "Bearer " + customerJwt)).andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/admin/accounts/" + account1.getAccountNumber() + "/status")
                .header("Authorization", "Bearer " + customerJwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"INACTIVE\"}")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/transactions").header("Authorization", "Bearer " + customerJwt)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/transactions/" + tx1.getTransactionReference()).header("Authorization", "Bearer " + customerJwt)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("16. Unauthenticated request to admin endpoint returns 401 Unauthorized")
    void testUnauthenticatedAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/customers")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("21-24. Inactive account regression: Deposit, Withdraw, Sender Transfer, and Receiver Transfer fail")
    void testInactiveAccountOperationsFail() throws Exception {
        // Mark account 1 as INACTIVE
        account1.setStatus(AccountStatus.INACTIVE);
        accountRepository.save(account1);

        // 21. Deposit to INACTIVE account fails
        mockMvc.perform(post("/api/accounts/" + account1.getAccountNumber() + "/deposit")
                .header("Authorization", "Bearer " + customerJwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DepositRequest(new BigDecimal("100.00")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot deposit to an account with status: INACTIVE"));

        // 22. Withdraw from INACTIVE account fails
        mockMvc.perform(post("/api/accounts/" + account1.getAccountNumber() + "/withdraw")
                .header("Authorization", "Bearer " + customerJwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new WithdrawalRequest(new BigDecimal("100.00")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot withdraw from an account with status: INACTIVE"));

        // 23. INACTIVE sender transfer fails
        mockMvc.perform(post("/api/accounts/transfer")
                .header("Authorization", "Bearer " + customerJwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TransferRequest(account1.getAccountNumber(), account2.getAccountNumber(), new BigDecimal("100.00")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Source account is not active: INACTIVE"));

        // 24. INACTIVE receiver transfer fails
        String customer2Jwt = tokenProvider.generateToken("bob.adm@example.com", UserRole.CUSTOMER);
        mockMvc.perform(post("/api/accounts/transfer")
                .header("Authorization", "Bearer " + customer2Jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TransferRequest(account2.getAccountNumber(), account1.getAccountNumber(), new BigDecimal("100.00")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Destination account is not active: INACTIVE"));
    }
}
