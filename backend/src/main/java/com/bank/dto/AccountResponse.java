package com.bank.dto;

import com.bank.entity.Account;
import com.bank.entity.enums.AccountStatus;
import com.bank.entity.enums.AccountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AccountResponse {

    private String accountNumber;
    private AccountType accountType;
    private BigDecimal balance;
    private AccountStatus status;
    private String ownerName;
    private String ownerEmail;
    private LocalDateTime createdAt;

    public AccountResponse() {
    }

    public AccountResponse(String accountNumber, AccountType accountType, BigDecimal balance,
                           AccountStatus status, String ownerName, String ownerEmail, LocalDateTime createdAt) {
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.balance = balance;
        this.status = status;
        this.ownerName = ownerName;
        this.ownerEmail = ownerEmail;
        this.createdAt = createdAt;
    }

    public static AccountResponse fromEntity(Account account) {
        return new AccountResponse(
                account.getAccountNumber(),
                account.getAccountType(),
                account.getBalance(),
                account.getStatus(),
                account.getUser() != null ? account.getUser().getName() : null,
                account.getUser() != null ? account.getUser().getEmail() : null,
                account.getCreatedAt()
        );
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
