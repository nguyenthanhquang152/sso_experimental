package com.demo.sso.exception;

/** Thrown when a JWT fails validation (wrong version, missing claims, etc.). */
public class InvalidTokenException extends SsoException {

    public InvalidTokenException(String message) {
        super(message);
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
