package com.demo.sso.exception;

/** Base exception for SSO-specific failure modes. */
public class SsoException extends RuntimeException {

    public SsoException(String message) {
        super(message);
    }

    public SsoException(String message, Throwable cause) {
        super(message, cause);
    }
}
