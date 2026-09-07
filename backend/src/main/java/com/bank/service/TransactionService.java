package com.bank.service;

import com.bank.dto.DepositRequest;
import com.bank.dto.PageResponse;
import com.bank.dto.TransactionResponse;
import com.bank.dto.WithdrawalRequest;
import com.bank.entity.enums.TransactionType;

import java.time.LocalDate;

public interface TransactionService {

    TransactionResponse deposit(String accountNumber, DepositRequest request, String userEmail, boolean isAdmin);

    TransactionResponse withdraw(String accountNumber, WithdrawalRequest request, String userEmail, boolean isAdmin);

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
