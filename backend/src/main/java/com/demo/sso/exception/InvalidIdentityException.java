package com.demo.sso.exception;

/** Thrown when provider identity claims fail normalization (missing fields, guest accounts, etc.). */
public class InvalidIdentityException extends SsoException {

    public InvalidIdentityException(String message) {
        super(message);
    }

    public InvalidIdentityException(String message, Throwable cause) {
        super(message, cause);
    }
}
