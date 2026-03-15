package com.demo.sso.exception;

/** Thrown when an authorization code is invalid or has expired. */
public class ExpiredAuthCodeException extends SsoException {

    public ExpiredAuthCodeException(String message) {
        super(message);
    }

    public ExpiredAuthCodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
