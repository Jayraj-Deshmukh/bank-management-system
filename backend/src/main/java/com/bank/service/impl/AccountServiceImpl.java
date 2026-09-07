package com.bank.service.impl;

import com.bank.dto.AccountResponse;
import com.bank.dto.CreateAccountRequest;
import com.bank.entity.Account;
import com.bank.entity.User;
import com.bank.entity.enums.AccountStatus;
import com.bank.exception.ResourceNotFoundException;
import com.bank.repository.AccountRepository;
import com.bank.repository.UserRepository;
import com.bank.service.AccountService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final SecureRandom random = new SecureRandom();

    public AccountServiceImpl(AccountRepository accountRepository, UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        String accountNumber = generateUniqueAccountNumber();

        Account account = new Account(
                accountNumber,
                request.getAccountType(),
                BigDecimal.ZERO, // Mandatory starting balance = 0.00
                AccountStatus.ACTIVE,
                user
        );

        Account savedAccount = accountRepository.save(account);
        return AccountResponse.fromEntity(savedAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> getMyAccounts(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        return accountRepository.findByUserId(user.getId())
                .stream()
                .map(AccountResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccountByNumber(String accountNumber, String userEmail, boolean isAdmin) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with account number: " + accountNumber));

        // Ownership enforcement for non-admin users
        if (!isAdmin && !account.getUser().getEmail().equals(userEmail)) {
            throw new AccessDeniedException("Access denied: you do not own this account");
        }

        return AccountResponse.fromEntity(account);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> getAllAccountsForAdmin() {
        return accountRepository.findAll()
                .stream()
                .map(AccountResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private String generateUniqueAccountNumber() {
        String accountNumber;
        int attempts = 0;
        do {
            long randomNumber = Math.abs(random.nextLong()) % 10000000000L;
            accountNumber = "10" + String.format("%010d", randomNumber);
            attempts++;
            if (attempts > 10) {
                throw new IllegalStateException("Unable to generate unique account number");
            }
        } while (accountRepository.existsByAccountNumber(accountNumber));

        return accountNumber;
    }
}
