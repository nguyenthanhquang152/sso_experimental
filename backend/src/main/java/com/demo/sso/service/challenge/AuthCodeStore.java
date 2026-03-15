package com.demo.sso.service.challenge;

/**
 * Store for single-use authorization codes that bridge OAuth authentication and JWT token exchange.
 *
 * <p>Uses a throw-on-failure strategy: {@link #exchangeCode} throws
 * {@link com.demo.sso.exception.InvalidAuthCodeException} for invalid or expired codes.
 */
public interface AuthCodeStore {

    /** @throws IllegalArgumentException if jwt is null or blank */
    String createAuthCode(String jwt);

    /**
     * @throws com.demo.sso.exception.InvalidAuthCodeException if the code is invalid, expired, or already consumed
     */
    String exchangeCode(String code);
}
