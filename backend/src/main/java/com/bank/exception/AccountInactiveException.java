package com.bank.exception;

public class AccountInactiveException extends RuntimeException {

    public AccountInactiveException(String message) {
        super(message);
    }
}
