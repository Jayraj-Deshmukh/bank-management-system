package com.bank.service;

import com.bank.dto.*;
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

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
    private Account accountA;
    private Account accountB;
    private Account frozenAccount;

    @BeforeEach
    void setUp() {
        user1 = new User("Alice", "alice@example.com", "111", "pass", UserRole.CUSTOMER);
        user1.setId(1L);

        user2 = new User("Bob", "bob@example.com", "222", "pass", UserRole.CUSTOMER);
        user2.setId(2L);

        accountA = new Account("101111111111", AccountType.SAVINGS, new BigDecimal("10000.00"), AccountStatus.ACTIVE, user1);
        accountA.setId(100L);

        accountB = new Account("102222222222", AccountType.CHECKING, new BigDecimal("2000.00"), AccountStatus.ACTIVE, user2);
        accountB.setId(200L);

        frozenAccount = new Account("103333333333", AccountType.SAVINGS, new BigDecimal("500.00"), AccountStatus.FROZEN, user1);
    }

    @Test
    @DisplayName("Successful deposit increases balance and creates DEPOSIT transaction")
    void testDepositSuccess() {
        when(accountRepository.findByAccountNumber("101111111111")).thenReturn(Optional.of(accountA));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DepositRequest request = new DepositRequest(new BigDecimal("5000.00"), "Bonus deposit");
        TransactionResponse response = transactionService.deposit("101111111111", request, "alice@example.com", false);

        assertThat(response).isNotNull();
        assertThat(response.getTransactionType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(response.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("15000.00"));
    }

    @Test
    @DisplayName("Successful withdrawal decreases balance and creates WITHDRAWAL transaction")
    void testWithdrawSuccess() {
        when(accountRepository.findByAccountNumber("101111111111")).thenReturn(Optional.of(accountA));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WithdrawalRequest request = new WithdrawalRequest(new BigDecimal("3000.00"), "ATM withdrawal");
        TransactionResponse response = transactionService.withdraw("101111111111", request, "alice@example.com", false);

        assertThat(response).isNotNull();
        assertThat(response.getTransactionType()).isEqualTo(TransactionType.WITHDRAWAL);
        assertThat(response.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("7000.00"));
    }

    @Test
    @DisplayName("Successful transfer debits sender, credits receiver, and generates dual transaction records")
    void testTransferSuccess() {
        when(accountRepository.findByAccountNumberForUpdate("101111111111")).thenReturn(Optional.of(accountA));
        when(accountRepository.findByAccountNumberForUpdate("102222222222")).thenReturn(Optional.of(accountB));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransferRequest request = new TransferRequest("101111111111", "102222222222", new BigDecimal("2000.00"), "Rent payment");
        TransferResponse response = transactionService.transfer(request, "alice@example.com", false);

        assertThat(response).isNotNull();
        assertThat(response.getSenderBalanceAfter()).isEqualByComparingTo(new BigDecimal("8000.00"));
        assertThat(accountB.getBalance()).isEqualByComparingTo(new BigDecimal("4000.00"));

        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Self-transfer throws IllegalArgumentException")
    void testSelfTransferRejected() {
        TransferRequest request = new TransferRequest("101111111111", "101111111111", new BigDecimal("500.00"));

        assertThrows(IllegalArgumentException.class, () ->
                transactionService.transfer(request, "alice@example.com", false)
        );
    }

    @Test
    @DisplayName("Transfer with insufficient sender balance throws InsufficientBalanceException")
    void testTransferInsufficientBalance() {
        when(accountRepository.findByAccountNumberForUpdate("101111111111")).thenReturn(Optional.of(accountA));
        when(accountRepository.findByAccountNumberForUpdate("102222222222")).thenReturn(Optional.of(accountB));

        TransferRequest request = new TransferRequest("101111111111", "102222222222", new BigDecimal("15000.00"));

        assertThrows(InsufficientBalanceException.class, () ->
                transactionService.transfer(request, "alice@example.com", false)
        );

        assertThat(accountA.getBalance()).isEqualByComparingTo(new BigDecimal("10000.00"));
        assertThat(accountB.getBalance()).isEqualByComparingTo(new BigDecimal("2000.00"));
    }
}
