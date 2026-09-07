package com.bank.service.impl;

import com.bank.dto.*;
import com.bank.entity.Account;
import com.bank.entity.Transaction;
import com.bank.entity.User;
import com.bank.entity.enums.TransactionType;
import com.bank.entity.enums.UserRole;
import com.bank.exception.ResourceNotFoundException;
import com.bank.repository.AccountRepository;
import com.bank.repository.TransactionRepository;
import com.bank.repository.UserRepository;
import com.bank.service.AdminService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public AdminServiceImpl(UserRepository userRepository, AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserDTO> getAllCustomers(int page, int size) {
        int validatedPage = Math.max(0, page);
        int validatedSize = Math.min(Math.max(1, size), 100);
        Pageable pageable = PageRequest.of(validatedPage, validatedSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<User> userPage = userRepository.findByRole(UserRole.CUSTOMER, pageable);
        List<UserDTO> content = userPage.getContent().stream()
                .map(u -> new UserDTO(u.getId(), u.getName(), u.getEmail(), u.getPhone(), u.getRole(), u.getCreatedAt()))
                .collect(Collectors.toList());

        return PageResponse.fromPage(userPage, content);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AccountResponse> getAllAccounts(int page, int size) {
        int validatedPage = Math.max(0, page);
        int validatedSize = Math.min(Math.max(1, size), 100);
        Pageable pageable = PageRequest.of(validatedPage, validatedSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Account> accountPage = accountRepository.findAll(pageable);
        List<AccountResponse> content = accountPage.getContent().stream()
                .map(AccountResponse::fromEntity)
                .collect(Collectors.toList());

        return PageResponse.fromPage(accountPage, content);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccountDetails(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with account number: " + accountNumber));
        return AccountResponse.fromEntity(account);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccountResponse updateAccountStatus(String accountNumber, UpdateAccountStatusRequest request) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with account number: " + accountNumber));

        account.setStatus(request.getStatus());
        Account savedAccount = accountRepository.save(account);
        return AccountResponse.fromEntity(savedAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> getAllTransactions(
            String accountNumber,
            TransactionType type,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size) {

        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("From date cannot be after To date");
        }

        int validatedPage = Math.max(0, page);
        int validatedSize = Math.min(Math.max(1, size), 100);
        Pageable pageable = PageRequest.of(validatedPage, validatedSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        LocalDateTime startDate = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime endDate = toDate != null ? toDate.atTime(LocalTime.MAX) : null;

        Page<Transaction> transactionPage = transactionRepository.findAllTransactionsFiltered(
                accountNumber, type, startDate, endDate, pageable
        );

        List<TransactionResponse> content = transactionPage.getContent().stream()
                .map(TransactionResponse::fromEntity)
                .collect(Collectors.toList());

        return PageResponse.fromPage(transactionPage, content);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getTransactionDetails(String transactionReference) {
        Transaction transaction = transactionRepository.findByTransactionReference(transactionReference)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with reference: " + transactionReference));
        return TransactionResponse.fromEntity(transaction);
    }
}
