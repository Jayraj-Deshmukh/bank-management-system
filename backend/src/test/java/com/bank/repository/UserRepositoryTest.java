package com.bank.repository;

import com.bank.entity.User;
import com.bank.entity.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should persist user entity and generate ID and timestamps")
    void testSaveUser() {
        User user = new User("Alice Smith", "alice@example.com", "1234567890", "rawPassword123", UserRole.CUSTOMER);
        User savedUser = userRepository.save(user);

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getName()).isEqualTo("Alice Smith");
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should find user by email")
    void testFindByEmail() {
        User user = new User("Bob Johnson", "bob@example.com", "9876543210", "rawPassword123", UserRole.CUSTOMER);
        userRepository.save(user);

        Optional<User> foundUser = userRepository.findByEmail("bob@example.com");

        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getName()).isEqualTo("Bob Johnson");
    }

    @Test
    @DisplayName("Should throw DataIntegrityViolationException when duplicate email is saved")
    void testUniqueEmailConstraint() {
        User user1 = new User("User One", "duplicate@example.com", "1111111111", "password123", UserRole.CUSTOMER);
        userRepository.saveAndFlush(user1);

        User user2 = new User("User Two", "duplicate@example.com", "2222222222", "password123", UserRole.ADMIN);

        assertThrows(DataIntegrityViolationException.class, () -> {
            userRepository.saveAndFlush(user2);
        });
    }
}
