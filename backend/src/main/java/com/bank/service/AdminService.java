package com.bank.service;

import com.bank.dto.*;
import com.bank.entity.enums.TransactionType;

import java.time.LocalDate;

public interface AdminService {

    PageResponse<UserDTO> getAllCustomers(int page, int size);

    PageResponse<AccountResponse> getAllAccounts(int page, int size);

    AccountResponse getAccountDetails(String accountNumber);

    AccountResponse updateAccountStatus(String accountNumber, UpdateAccountStatusRequest request);

    PageResponse<TransactionResponse> getAllTransactions(
            String accountNumber,
            TransactionType type,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size);

    TransactionResponse getTransactionDetails(String transactionReference);
}
