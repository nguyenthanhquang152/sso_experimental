package com.demo.sso.exception;

/** Thrown when an authorization code is invalid, expired, or already consumed. */
public class InvalidAuthCodeException extends SsoException {

    public InvalidAuthCodeException(String message) {
        super(message);
    }

    public InvalidAuthCodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
