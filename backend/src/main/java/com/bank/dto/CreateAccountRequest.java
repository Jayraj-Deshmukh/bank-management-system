package com.bank.dto;

import com.bank.entity.enums.AccountType;
import jakarta.validation.constraints.NotNull;

public class CreateAccountRequest {

    @NotNull(message = "Account type is required")
    private AccountType accountType;

    public CreateAccountRequest() {
    }

    public CreateAccountRequest(AccountType accountType) {
        this.accountType = accountType;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }
}
