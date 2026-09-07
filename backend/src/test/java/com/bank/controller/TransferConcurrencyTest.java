package com.bank.controller;

import com.bank.dto.TransferRequest;
import com.bank.entity.Account;
import com.bank.entity.User;
import com.bank.entity.enums.AccountStatus;
import com.bank.entity.enums.AccountType;
import com.bank.entity.enums.UserRole;
import com.bank.repository.AccountRepository;
import com.bank.repository.TransactionRepository;
import com.bank.repository.UserRepository;
import com.bank.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TransferConcurrencyTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User customerA;
    private User customerB;
    private Account accountA;
    private Account accountB;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();

        customerA = userRepository.save(new User("Alice", "alice.conc@example.com", "111", passwordEncoder.encode("pass"), UserRole.CUSTOMER));
        customerB = userRepository.save(new User("Bob", "bob.conc@example.com", "222", passwordEncoder.encode("pass"), UserRole.CUSTOMER));

        accountA = accountRepository.save(new Account("101111111111", AccountType.SAVINGS, new BigDecimal("10000.00"), AccountStatus.ACTIVE, customerA));
        accountB = accountRepository.save(new Account("102222222222", AccountType.CHECKING, new BigDecimal("2000.00"), AccountStatus.ACTIVE, customerB));
    }

    @Test
    @DisplayName("Concurrent transfers exceeding balance: Exactly 1 succeeds, 1 fails, balance never negative")
    void testConcurrentTransfersExceedingBalance() throws InterruptedException {
        int numberOfThreads = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        TransferRequest request = new TransferRequest(accountA.getAccountNumber(), accountB.getAccountNumber(), new BigDecimal("7000.00"), "Concurrent Transfer");

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    latch.await(); // Synchronize thread startup
                    transactionService.transfer(request, "alice.conc@example.com", false);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        latch.countDown(); // Release threads simultaneously
        doneLatch.await(); // Wait for completion
        executorService.shutdown();

        Account finalA = accountRepository.findById(accountA.getId()).orElseThrow();
        Account finalB = accountRepository.findById(accountB.getId()).orElseThrow();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(1);
        assertThat(finalA.getBalance()).isEqualByComparingTo(new BigDecimal("3000.00"));
        assertThat(finalB.getBalance()).isEqualByComparingTo(new BigDecimal("9000.00"));
    }

    @Test
    @DisplayName("Opposite-direction concurrent transfers: No deadlocks, both succeed cleanly")
    void testOppositeDirectionConcurrentTransfersNoDeadlock() throws InterruptedException {
        // Set both account balances to 10,000.00
        accountA.setBalance(new BigDecimal("10000.00"));
        accountB.setBalance(new BigDecimal("10000.00"));
        accountRepository.save(accountA);
        accountRepository.save(accountB);

        int numberOfThreads = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        TransferRequest reqAtoB = new TransferRequest(accountA.getAccountNumber(), accountB.getAccountNumber(), new BigDecimal("1000.00"), "A to B");
        TransferRequest reqBtoA = new TransferRequest(accountB.getAccountNumber(), accountA.getAccountNumber(), new BigDecimal("1000.00"), "B to A");

        // Thread 1: A -> B
        executorService.submit(() -> {
            try {
                latch.await();
                transactionService.transfer(reqAtoB, "alice.conc@example.com", false);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        });

        // Thread 2: B -> A
        executorService.submit(() -> {
            try {
                latch.await();
                transactionService.transfer(reqBtoA, "bob.conc@example.com", false);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        });

        latch.countDown();
        doneLatch.await();
        executorService.shutdown();

        Account finalA = accountRepository.findById(accountA.getId()).orElseThrow();
        Account finalB = accountRepository.findById(accountB.getId()).orElseThrow();

        assertThat(successCount.get()).isEqualTo(2);
        assertThat(failureCount.get()).isEqualTo(0);
        assertThat(finalA.getBalance()).isEqualByComparingTo(new BigDecimal("10000.00"));
        assertThat(finalB.getBalance()).isEqualByComparingTo(new BigDecimal("10000.00"));
    }
}
