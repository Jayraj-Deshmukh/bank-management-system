package com.bank.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class WithdrawalRequest {

    @NotNull(message = "Withdrawal amount is required")
    @Positive(message = "Withdrawal amount must be greater than zero")
    private BigDecimal amount;

    private String description;

    public WithdrawalRequest() {
    }

    public WithdrawalRequest(BigDecimal amount) {
        this.amount = amount;
    }

    public WithdrawalRequest(BigDecimal amount, String description) {
        this.amount = amount;
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
