package com.demo.sso.service;

/**
 * Stores short-lived, single-use authorization codes that map to JWTs.
 * Codes are generated during OAuth2 success and exchanged by the frontend for a JWT.
 */
public interface AuthCodeStore {

    /**
     * Store a JWT and return a single-use authorization code.
     */
    String storeJwt(String jwt);

    /**
     * Exchange a code for the stored JWT. Returns null if invalid/expired.
     * The code is consumed and cannot be reused.
     */
    String exchangeCode(String code);
}
