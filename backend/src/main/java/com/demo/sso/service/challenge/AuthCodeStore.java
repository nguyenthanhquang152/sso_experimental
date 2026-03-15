package com.demo.sso.service.challenge;

/**
 * Stores short-lived, single-use authorization codes that map to JWTs.
 * Codes are generated during OAuth2 success and exchanged by the frontend for a JWT.
 */
public interface AuthCodeStore {

    /** @throws IllegalArgumentException if jwt is null or blank */
    String createAuthCode(String jwt);

    /**
     * @throws com.demo.sso.exception.InvalidAuthCodeException if the code is invalid, expired, or already consumed
     */
    String exchangeCode(String code);
}
