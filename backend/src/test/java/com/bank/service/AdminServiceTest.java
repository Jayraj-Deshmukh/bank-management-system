package com.bank.service;

import com.bank.dto.*;
import com.bank.entity.Account;
import com.bank.entity.Transaction;
import com.bank.entity.User;
import com.bank.entity.enums.AccountStatus;
import com.bank.entity.enums.AccountType;
import com.bank.entity.enums.TransactionType;
import com.bank.entity.enums.UserRole;
import com.bank.exception.ResourceNotFoundException;
import com.bank.repository.AccountRepository;
import com.bank.repository.TransactionRepository;
import com.bank.repository.UserRepository;
import com.bank.service.impl.AdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AdminServiceImpl adminService;

    private User customer;
    private Account account;
    private Transaction transaction;

    @BeforeEach
    void setUp() {
        customer = new User("Alice", "alice@example.com", "111", "pass", UserRole.CUSTOMER);
        customer.setId(1L);

        account = new Account("101111111111", AccountType.SAVINGS, new BigDecimal("5000.00"), AccountStatus.ACTIVE, customer);
        account.setId(10L);

        transaction = new Transaction("TXD-100", TransactionType.DEPOSIT, new BigDecimal("5000.00"), new BigDecimal("5000.00"), "Initial Deposit", account);
    }

    @Test
    @DisplayName("Admin lists all customers with pagination")
    void testGetAllCustomers() {
        Page<User> page = new PageImpl<>(List.of(customer));
        when(userRepository.findByRole(eq(UserRole.CUSTOMER), any(Pageable.class))).thenReturn(page);

        PageResponse<UserDTO> response = adminService.getAllCustomers(0, 10);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("Admin lists all accounts with pagination")
    void testGetAllAccounts() {
        Page<Account> page = new PageImpl<>(List.of(account));
        when(accountRepository.findAll(any(Pageable.class))).thenReturn(page);

        PageResponse<AccountResponse> response = adminService.getAllAccounts(0, 10);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getAccountNumber()).isEqualTo("101111111111");
    }

    @Test
    @DisplayName("Admin gets account details by account number")
    void testGetAccountDetailsSuccess() {
        when(accountRepository.findByAccountNumber("101111111111")).thenReturn(Optional.of(account));

        AccountResponse response = adminService.getAccountDetails("101111111111");

        assertThat(response.getAccountNumber()).isEqualTo("101111111111");
    }

    @Test
    @DisplayName("Admin updates account status from ACTIVE to INACTIVE")
    void testUpdateAccountStatus() {
        when(accountRepository.findByAccountNumber("101111111111")).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateAccountStatusRequest request = new UpdateAccountStatusRequest(AccountStatus.INACTIVE);
        AccountResponse response = adminService.updateAccountStatus("101111111111", request);

        assertThat(response.getStatus()).isEqualTo(AccountStatus.INACTIVE);
        verify(accountRepository).save(argThat(acc -> acc.getStatus() == AccountStatus.INACTIVE));
    }

    @Test
    @DisplayName("Admin lists all system transactions with filtering")
    void testGetAllTransactions() {
        Page<Transaction> page = new PageImpl<>(List.of(transaction));
        when(transactionRepository.findAllTransactionsFiltered(any(), any(), any(), any(), any(Pageable.class))).thenReturn(page);

        PageResponse<TransactionResponse> response = adminService.getAllTransactions(null, null, null, null, 0, 20);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getTransactionReference()).isEqualTo("TXD-100");
    }

    @Test
    @DisplayName("Admin views transaction details by reference")
    void testGetTransactionDetails() {
        when(transactionRepository.findByTransactionReference("TXD-100")).thenReturn(Optional.of(transaction));

        TransactionResponse response = adminService.getTransactionDetails("TXD-100");

        assertThat(response.getTransactionReference()).isEqualTo("TXD-100");
    }

    @Test
    @DisplayName("Get nonexistent transaction details throws ResourceNotFoundException")
    void testGetTransactionDetailsNotFound() {
        when(transactionRepository.findByTransactionReference("INVALID")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                adminService.getTransactionDetails("INVALID")
        );
    }
}
