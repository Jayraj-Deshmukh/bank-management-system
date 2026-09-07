package com.bank.service;

import com.bank.dto.DepositRequest;
import com.bank.dto.PageResponse;
import com.bank.dto.TransactionResponse;
import com.bank.dto.WithdrawalRequest;
import com.bank.entity.Account;
import com.bank.entity.Transaction;
import com.bank.entity.User;
import com.bank.entity.enums.AccountStatus;
import com.bank.entity.enums.AccountType;
import com.bank.entity.enums.TransactionType;
import com.bank.entity.enums.UserRole;
import com.bank.exception.AccountInactiveException;
import com.bank.exception.InsufficientBalanceException;
import com.bank.exception.ResourceNotFoundException;
import com.bank.repository.AccountRepository;
import com.bank.repository.TransactionRepository;
import com.bank.service.impl.TransactionServiceImpl;
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
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private User user1;
    private User user2;
    private Account activeAccount;
    private Account frozenAccount;
    private Transaction depositTx;
    private Transaction withdrawTx;

    @BeforeEach
    void setUp() {
        user1 = new User("Alice", "alice@example.com", "111", "pass", UserRole.CUSTOMER);
        user1.setId(1L);

        user2 = new User("Bob", "bob@example.com", "222", "pass", UserRole.CUSTOMER);
        user2.setId(2L);

        activeAccount = new Account("101111111111", AccountType.SAVINGS, new BigDecimal("1000.00"), AccountStatus.ACTIVE, user1);
        activeAccount.setId(100L);

        frozenAccount = new Account("102222222222", AccountType.CHECKING, new BigDecimal("500.00"), AccountStatus.FROZEN, user1);

        depositTx = new Transaction("TXD-1", TransactionType.DEPOSIT, new BigDecimal("1000.00"), new BigDecimal("1000.00"), "Deposit", activeAccount);
        withdrawTx = new Transaction("TXW-1", TransactionType.WITHDRAWAL, new BigDecimal("200.00"), new BigDecimal("800.00"), "Withdraw", activeAccount);
    }

    @Test
    @DisplayName("1-6. Successful deposit increases balance, creates DEPOSIT transaction with balanceAfter")
    void testDepositSuccess() {
        when(accountRepository.findByAccountNumber("101111111111")).thenReturn(Optional.of(activeAccount));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DepositRequest request = new DepositRequest(new BigDecimal("5000.00"), "Bonus deposit");
        TransactionResponse response = transactionService.deposit("101111111111", request, "alice@example.com", false);

        assertThat(response).isNotNull();
        assertThat(response.getAccountNumber()).isEqualTo("101111111111");
        assertThat(response.getTransactionType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(response.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("6000.00"));
        assertThat(response.getTransactionReference()).startsWith("TXD-");

        verify(accountRepository).save(argThat(acc -> acc.getBalance().compareTo(new BigDecimal("6000.00")) == 0));
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Successful withdrawal decreases balance, creates WITHDRAWAL transaction with balanceAfter")
    void testWithdrawSuccess() {
        when(accountRepository.findByAccountNumber("101111111111")).thenReturn(Optional.of(activeAccount));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WithdrawalRequest request = new WithdrawalRequest(new BigDecimal("300.00"), "ATM withdrawal");
        TransactionResponse response = transactionService.withdraw("101111111111", request, "alice@example.com", false);

        assertThat(response).isNotNull();
        assertThat(response.getTransactionType()).isEqualTo(TransactionType.WITHDRAWAL);
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(response.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("700.00"));
        assertThat(response.getTransactionReference()).startsWith("TXW-");

        verify(accountRepository).save(argThat(acc -> acc.getBalance().compareTo(new BigDecimal("700.00")) == 0));
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Withdrawal with insufficient balance throws InsufficientBalanceException and preserves balance")
    void testWithdrawInsufficientBalance() {
        when(accountRepository.findByAccountNumber("101111111111")).thenReturn(Optional.of(activeAccount));

        WithdrawalRequest request = new WithdrawalRequest(new BigDecimal("1500.00"));

        assertThrows(InsufficientBalanceException.class, () ->
                transactionService.withdraw("101111111111", request, "alice@example.com", false)
        );

        assertThat(activeAccount.getBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Get transaction history returns paginated response")
    void testGetTransactionHistorySuccess() {
        when(accountRepository.findByAccountNumber("101111111111")).thenReturn(Optional.of(activeAccount));
        Page<Transaction> page = new PageImpl<>(List.of(withdrawTx, depositTx));
        when(transactionRepository.findByAccountIdFiltered(eq(100L), any(), any(), any(), any(Pageable.class))).thenReturn(page);

        PageResponse<TransactionResponse> result = transactionService.getTransactionHistory(
                "101111111111", null, null, null, 0, 10, "alice@example.com", false
        );

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getPage()).isEqualTo(0);
    }

    @Test
    @DisplayName("Get transaction history empty returns 200 OK with empty content")
    void testGetTransactionHistoryEmpty() {
        when(accountRepository.findByAccountNumber("101111111111")).thenReturn(Optional.of(activeAccount));
        Page<Transaction> emptyPage = new PageImpl<>(Collections.emptyList());
        when(transactionRepository.findByAccountIdFiltered(eq(100L), any(), any(), any(), any(Pageable.class))).thenReturn(emptyPage);

        PageResponse<TransactionResponse> result = transactionService.getTransactionHistory(
                "101111111111", null, null, null, 0, 10, "alice@example.com", false
        );

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("Invalid date range (from > to) throws IllegalArgumentException")
    void testGetTransactionHistoryInvalidDateRange() {
        when(accountRepository.findByAccountNumber("101111111111")).thenReturn(Optional.of(activeAccount));

        LocalDate from = LocalDate.of(2026, 9, 10);
        LocalDate to = LocalDate.of(2026, 9, 1);

        assertThrows(IllegalArgumentException.class, () ->
                transactionService.getTransactionHistory("101111111111", null, from, to, 0, 10, "alice@example.com", false)
        );
    }
}
