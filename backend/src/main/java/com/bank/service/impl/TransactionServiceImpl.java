package com.bank.service.impl;

import com.bank.dto.*;
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
    @Transactional(rollbackFor = Exception.class)
    public TransferResponse transfer(TransferRequest request, String userEmail, boolean isAdmin) {
        if (request.getFromAccount().equals(request.getToAccount())) {
            throw new IllegalArgumentException("Self-transfer is not allowed. Source and destination accounts must be different.");
        }

        // Deterministic Lock Ordering (prevents deadlocks on concurrent opposite transfers)
        Account firstLockAccount;
        Account secondLockAccount;

        if (request.getFromAccount().compareTo(request.getToAccount()) < 0) {
            firstLockAccount = accountRepository.findByAccountNumberForUpdate(request.getFromAccount())
                    .orElseThrow(() -> new ResourceNotFoundException("Source account not found: " + request.getFromAccount()));
            secondLockAccount = accountRepository.findByAccountNumberForUpdate(request.getToAccount())
                    .orElseThrow(() -> new ResourceNotFoundException("Destination account not found: " + request.getToAccount()));
        } else {
            firstLockAccount = accountRepository.findByAccountNumberForUpdate(request.getToAccount())
                    .orElseThrow(() -> new ResourceNotFoundException("Destination account not found: " + request.getToAccount()));
            secondLockAccount = accountRepository.findByAccountNumberForUpdate(request.getFromAccount())
                    .orElseThrow(() -> new ResourceNotFoundException("Source account not found: " + request.getFromAccount()));
        }

        Account fromAccount = request.getFromAccount().equals(firstLockAccount.getAccountNumber()) ? firstLockAccount : secondLockAccount;
        Account toAccount = request.getToAccount().equals(firstLockAccount.getAccountNumber()) ? firstLockAccount : secondLockAccount;

        // Sender ownership check
        if (!isAdmin && !fromAccount.getUser().getEmail().equals(userEmail)) {
            throw new AccessDeniedException("Access denied: you do not own the source account");
        }

        // Account status checks
        if (fromAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountInactiveException("Source account is not active: " + fromAccount.getStatus());
        }
        if (toAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountInactiveException("Destination account is not active: " + toAccount.getStatus());
        }

        BigDecimal transferAmount = request.getAmount();

        // Insufficient balance check
        if (fromAccount.getBalance().compareTo(transferAmount) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance for transfer. Current balance: " + fromAccount.getBalance() + ", Requested: " + transferAmount
            );
        }

        // Update balances
        BigDecimal senderNewBalance = fromAccount.getBalance().subtract(transferAmount);
        BigDecimal receiverNewBalance = toAccount.getBalance().add(transferAmount);

        fromAccount.setBalance(senderNewBalance);
        toAccount.setBalance(receiverNewBalance);

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        // Generate atomic transaction records
        String groupId = UUID.randomUUID().toString();
        String senderRef = "TXT-OUT-" + groupId;
        String receiverRef = "TXT-IN-" + groupId;

        String description = request.getDescription() != null && !request.getDescription().isBlank()
                ? request.getDescription()
                : "Fund Transfer";

        Transaction senderTransaction = new Transaction(
                senderRef,
                TransactionType.TRANSFER_OUT,
                transferAmount,
                senderNewBalance,
                "Transfer to " + toAccount.getAccountNumber() + " - " + description,
                fromAccount
        );

        Transaction receiverTransaction = new Transaction(
                receiverRef,
                TransactionType.TRANSFER_IN,
                transferAmount,
                receiverNewBalance,
                "Transfer from " + fromAccount.getAccountNumber() + " - " + description,
                toAccount
        );

        transactionRepository.save(senderTransaction);
        transactionRepository.save(receiverTransaction);

        return new TransferResponse(
                senderRef,
                fromAccount.getAccountNumber(),
                toAccount.getAccountNumber(),
                transferAmount,
                senderNewBalance,
                description,
                LocalDateTime.now()
        );
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
