package com.bank.dto;

import com.bank.entity.enums.AccountStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateAccountStatusRequest {

    @NotNull(message = "Account status is required")
    private AccountStatus status;

    public UpdateAccountStatusRequest() {
    }

    public UpdateAccountStatusRequest(AccountStatus status) {
        this.status = status;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }
}
