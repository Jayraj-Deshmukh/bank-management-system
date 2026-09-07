package com.bank.service;

import com.bank.dto.AccountResponse;
import com.bank.dto.CreateAccountRequest;

import java.util.List;

public interface AccountService {

    AccountResponse createAccount(CreateAccountRequest request, String userEmail);

    List<AccountResponse> getMyAccounts(String userEmail);

    AccountResponse getAccountByNumber(String accountNumber, String userEmail, boolean isAdmin);

    List<AccountResponse> getAllAccountsForAdmin();
}
