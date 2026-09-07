package com.bank.repository;

import com.bank.entity.Account;
import com.bank.entity.User;
import com.bank.entity.enums.AccountStatus;
import com.bank.entity.enums.AccountType;
import com.bank.entity.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AccountRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should persist Account entity linked to User with exact BigDecimal balance")
    void testSaveAccount() {
        User user = userRepository.save(new User("Charlie", "charlie@example.com", "5551234567", "pass", UserRole.CUSTOMER));

        BigDecimal initialBalance = new BigDecimal("2500.75");
        Account account = new Account("ACC1000200030", AccountType.SAVINGS, initialBalance, AccountStatus.ACTIVE, user);
        Account savedAccount = accountRepository.save(account);

        assertThat(savedAccount.getId()).isNotNull();
        assertThat(savedAccount.getAccountNumber()).isEqualTo("ACC1000200030");
        assertThat(savedAccount.getBalance()).isEqualByComparingTo(new BigDecimal("2500.75"));
        assertThat(savedAccount.getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("Should enforce unique account number constraint")
    void testUniqueAccountNumberConstraint() {
        User user = userRepository.save(new User("David", "david@example.com", "5559876543", "pass", UserRole.CUSTOMER));

        Account account1 = new Account("ACC_UNIQUE_001", AccountType.CHECKING, new BigDecimal("1000.00"), AccountStatus.ACTIVE, user);
        accountRepository.saveAndFlush(account1);

        Account account2 = new Account("ACC_UNIQUE_001", AccountType.SAVINGS, new BigDecimal("500.00"), AccountStatus.ACTIVE, user);

        assertThrows(DataIntegrityViolationException.class, () -> {
            accountRepository.saveAndFlush(account2);
        });
    }
}
