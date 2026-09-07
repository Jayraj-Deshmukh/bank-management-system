package com.bank.repository;

import com.bank.entity.Account;
import com.bank.entity.Transaction;
import com.bank.entity.User;
import com.bank.entity.enums.AccountStatus;
import com.bank.entity.enums.AccountType;
import com.bank.entity.enums.TransactionType;
import com.bank.entity.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should persist Transaction entity linked to Account with exact BigDecimal amounts")
    void testSaveTransaction() {
        User user = userRepository.save(new User("Eve", "eve@example.com", "5550001111", "pass", UserRole.CUSTOMER));
        Account account = accountRepository.save(new Account("ACC999888777", AccountType.SAVINGS, new BigDecimal("5000.00"), AccountStatus.ACTIVE, user));

        String txRef = UUID.randomUUID().toString();
        BigDecimal depositAmount = new BigDecimal("500.25");
        BigDecimal newBalance = new BigDecimal("5500.25");

        Transaction tx = new Transaction(txRef, TransactionType.DEPOSIT, depositAmount, newBalance, "Initial deposit test", account);
        Transaction savedTx = transactionRepository.save(tx);

        assertThat(savedTx.getId()).isNotNull();
        assertThat(savedTx.getTransactionReference()).isEqualTo(txRef);
        assertThat(savedTx.getAmount()).isEqualByComparingTo(new BigDecimal("500.25"));
        assertThat(savedTx.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("5500.25"));
        assertThat(savedTx.getAccount().getId()).isEqualTo(account.getId());
    }

    @Test
    @DisplayName("Should enforce unique transaction reference constraint")
    void testUniqueTransactionReferenceConstraint() {
        User user = userRepository.save(new User("Frank", "frank@example.com", "5552223333", "pass", UserRole.CUSTOMER));
        Account account = accountRepository.save(new Account("ACC111222333", AccountType.CHECKING, new BigDecimal("1000.00"), AccountStatus.ACTIVE, user));

        String duplicateRef = "TX-DUPLICATE-REF-123";
        Transaction tx1 = new Transaction(duplicateRef, TransactionType.WITHDRAWAL, new BigDecimal("100.00"), new BigDecimal("900.00"), "Withdrawal 1", account);
        transactionRepository.saveAndFlush(tx1);

        Transaction tx2 = new Transaction(duplicateRef, TransactionType.WITHDRAWAL, new BigDecimal("50.00"), new BigDecimal("850.00"), "Withdrawal 2", account);

        assertThrows(DataIntegrityViolationException.class, () -> {
            transactionRepository.saveAndFlush(tx2);
        });
    }
}
