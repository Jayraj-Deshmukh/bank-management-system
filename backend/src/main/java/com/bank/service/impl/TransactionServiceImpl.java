package com.bank.service.impl;

import com.bank.dto.DepositRequest;
import com.bank.dto.PageResponse;
import com.bank.dto.TransactionResponse;
import com.bank.dto.WithdrawalRequest;
import com.bank.entity.Account;
import com.bank.entity.Transaction;
import com.bank.entity.enums.AccountStatus;
import com.bank.entity.enums.TransactionType;
import com.bank.exception.AccountInactiveException;
import com.bank.exception.InsufficientBalanceException;
import com.bank.exception.ResourceNotFoundException;
import com.bank.repository.AccountRepository;
import com.bank.repository.TransactionRepository;
import com.bank.service.TransactionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TransactionServiceImpl implements TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public TransactionServiceImpl(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TransactionResponse deposit(String accountNumber, DepositRequest request, String userEmail, boolean isAdmin) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with account number: " + accountNumber));

        if (!isAdmin && !account.getUser().getEmail().equals(userEmail)) {
            throw new AccessDeniedException("Access denied: you do not own this account");
        }

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountInactiveException("Cannot deposit to an account with status: " + account.getStatus());
        }

        BigDecimal depositAmount = request.getAmount();
        BigDecimal newBalance = account.getBalance().add(depositAmount);

        account.setBalance(newBalance);
        accountRepository.save(account);

        String transactionReference = "TXD-" + UUID.randomUUID().toString();
        String description = request.getDescription() != null && !request.getDescription().isBlank()
                ? request.getDescription()
                : "Cash Deposit";

        Transaction transaction = new Transaction(
                transactionReference,
                TransactionType.DEPOSIT,
                depositAmount,
                newBalance,
                description,
                account
        );

        Transaction savedTransaction = transactionRepository.save(transaction);
        return TransactionResponse.fromEntity(savedTransaction);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TransactionResponse withdraw(String accountNumber, WithdrawalRequest request, String userEmail, boolean isAdmin) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with account number: " + accountNumber));

        if (!isAdmin && !account.getUser().getEmail().equals(userEmail)) {
            throw new AccessDeniedException("Access denied: you do not own this account");
        }

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountInactiveException("Cannot withdraw from an account with status: " + account.getStatus());
        }

        BigDecimal withdrawalAmount = request.getAmount();

        if (account.getBalance().compareTo(withdrawalAmount) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance for withdrawal. Current balance: " + account.getBalance() + ", Requested: " + withdrawalAmount
            );
        }

        BigDecimal newBalance = account.getBalance().subtract(withdrawalAmount);

        account.setBalance(newBalance);
        accountRepository.save(account);

        String transactionReference = "TXW-" + UUID.randomUUID().toString();
        String description = request.getDescription() != null && !request.getDescription().isBlank()
                ? request.getDescription()
                : "Cash Withdrawal";

        Transaction transaction = new Transaction(
                transactionReference,
                TransactionType.WITHDRAWAL,
                withdrawalAmount,
                newBalance,
                description,
                account
        );

        Transaction savedTransaction = transactionRepository.save(transaction);
        return TransactionResponse.fromEntity(savedTransaction);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> getTransactionHistory(
            String accountNumber,
            TransactionType type,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size,
            String userEmail,
            boolean isAdmin) {

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with account number: " + accountNumber));

        if (!isAdmin && !account.getUser().getEmail().equals(userEmail)) {
            throw new AccessDeniedException("Access denied: you do not own this account");
        }

        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("From date cannot be after To date");
        }

        int validatedPage = Math.max(0, page);
        int validatedSize = Math.min(Math.max(1, size), 50);

        Pageable pageable = PageRequest.of(validatedPage, validatedSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        LocalDateTime startDate = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime endDate = toDate != null ? toDate.atTime(LocalTime.MAX) : null;

        Page<Transaction> transactionPage = transactionRepository.findByAccountIdFiltered(
                account.getId(),
                type,
                startDate,
                endDate,
                pageable
        );

        List<TransactionResponse> content = transactionPage.getContent()
                .stream()
                .map(TransactionResponse::fromEntity)
                .collect(Collectors.toList());

        return PageResponse.fromPage(transactionPage, content);
    }
}
