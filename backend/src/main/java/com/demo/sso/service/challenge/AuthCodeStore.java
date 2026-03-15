package com.demo.sso.service.challenge;

/**
 * Stores short-lived, single-use authorization codes that map to JWTs.
 * Codes are generated during OAuth2 success and exchanged by the frontend for a JWT.
 */
public interface AuthCodeStore {

    /**
     * Generates a cryptographically random, single-use authorization code
     * bound to the given JWT, valid for a short TTL.
     *
     * @return a single-use code that can be exchanged via {@link #exchangeCode(String)}
     */
    String storeJwt(String jwt);

    /**
     * Exchanges a code for the stored JWT.
     * The code is consumed and cannot be reused (single-use guarantee).
     *
     * @throws IllegalArgumentException if the code is invalid, expired, or already consumed
     */
    String exchangeCode(String code);
}
