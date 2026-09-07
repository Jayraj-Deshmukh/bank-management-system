package com.bank.service;

import com.bank.dto.AccountResponse;
import com.bank.dto.CreateAccountRequest;
import com.bank.entity.Account;
import com.bank.entity.User;
import com.bank.entity.enums.AccountStatus;
import com.bank.entity.enums.AccountType;
import com.bank.entity.enums.UserRole;
import com.bank.exception.ResourceNotFoundException;
import com.bank.repository.AccountRepository;
import com.bank.repository.UserRepository;
import com.bank.service.impl.AccountServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AccountServiceImpl accountService;

    private User user1;
    private User user2;
    private Account account1;

    @BeforeEach
    void setUp() {
        user1 = new User("User One", "user1@example.com", "1111111111", "pass", UserRole.CUSTOMER);
        user1.setId(1L);

        user2 = new User("User Two", "user2@example.com", "2222222222", "pass", UserRole.CUSTOMER);
        user2.setId(2L);

        account1 = new Account("101234567890", AccountType.SAVINGS, BigDecimal.ZERO, AccountStatus.ACTIVE, user1);
        account1.setId(10L);
    }

    @Test
    @DisplayName("Should create account with zero balance and server-generated account number")
    void testCreateAccountSuccess() {
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(user1));
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenReturn(account1);

        CreateAccountRequest request = new CreateAccountRequest(AccountType.SAVINGS);
        AccountResponse response = accountService.createAccount(request, "user1@example.com");

        assertThat(response).isNotNull();
        assertThat(response.getAccountType()).isEqualTo(AccountType.SAVINGS);
        assertThat(response.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(response.getOwnerEmail()).isEqualTo("user1@example.com");

        verify(accountRepository).save(argThat(acc ->
                acc.getBalance().compareTo(BigDecimal.ZERO) == 0 &&
                acc.getStatus() == AccountStatus.ACTIVE &&
                acc.getAccountNumber().startsWith("10")
        ));
    }

    @Test
    @DisplayName("Should retrieve only my accounts for authenticated user")
    void testGetMyAccounts() {
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(user1));
        when(accountRepository.findByUserId(1L)).thenReturn(List.of(account1));

        List<AccountResponse> myAccounts = accountService.getMyAccounts("user1@example.com");

        assertThat(myAccounts).hasSize(1);
        assertThat(myAccounts.get(0).getAccountNumber()).isEqualTo("101234567890");
    }

    @Test
    @DisplayName("Should allow customer to retrieve own account by number")
    void testGetAccountByNumberOwn() {
        when(accountRepository.findByAccountNumber("101234567890")).thenReturn(Optional.of(account1));

        AccountResponse response = accountService.getAccountByNumber("101234567890", "user1@example.com", false);

        assertThat(response.getAccountNumber()).isEqualTo("101234567890");
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when customer attempts to access another customer's account")
    void testGetAccountByNumberUnauthorized() {
        when(accountRepository.findByAccountNumber("101234567890")).thenReturn(Optional.of(account1));

        assertThrows(AccessDeniedException.class, () ->
                accountService.getAccountByNumber("101234567890", "user2@example.com", false)
        );
    }

    @Test
    @DisplayName("Should allow admin to access any customer's account")
    void testGetAccountByNumberAdminAccess() {
        when(accountRepository.findByAccountNumber("101234567890")).thenReturn(Optional.of(account1));

        AccountResponse response = accountService.getAccountByNumber("101234567890", "admin@example.com", true);

        assertThat(response.getAccountNumber()).isEqualTo("101234567890");
    }
}
