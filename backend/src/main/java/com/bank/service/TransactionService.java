package com.bank.service;

import com.bank.dto.*;
import com.bank.entity.enums.TransactionType;

import java.time.LocalDate;

public interface TransactionService {

    TransactionResponse deposit(String accountNumber, DepositRequest request, String userEmail, boolean isAdmin);

    TransactionResponse withdraw(String accountNumber, WithdrawalRequest request, String userEmail, boolean isAdmin);

    TransferResponse transfer(TransferRequest request, String userEmail, boolean isAdmin);

    PageResponse<TransactionResponse> getTransactionHistory(
            String accountNumber,
            TransactionType type,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size,
            String userEmail,
            boolean isAdmin);
}
