package com.bank.dto;

import com.bank.entity.Transaction;
import com.bank.entity.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionResponse {

    private String accountNumber;
    private String transactionReference;
    private TransactionType transactionType;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String description;
    private LocalDateTime timestamp;

    public TransactionResponse() {
    }

    public TransactionResponse(String accountNumber, String transactionReference, TransactionType transactionType,
                               BigDecimal amount, BigDecimal balanceAfter, String description, LocalDateTime timestamp) {
        this.accountNumber = accountNumber;
        this.transactionReference = transactionReference;
        this.transactionType = transactionType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.timestamp = timestamp;
    }

    public static TransactionResponse fromEntity(Transaction transaction) {
        return new TransactionResponse(
                transaction.getAccount() != null ? transaction.getAccount().getAccountNumber() : null,
                transaction.getTransactionReference(),
                transaction.getTransactionType(),
                transaction.getAmount(),
                transaction.getBalanceAfter(),
                transaction.getDescription(),
                transaction.getCreatedAt()
        );
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
