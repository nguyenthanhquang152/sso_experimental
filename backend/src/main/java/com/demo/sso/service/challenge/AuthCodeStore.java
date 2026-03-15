package com.demo.sso.service.challenge;

/**
 * Store for single-use authorization codes that bridge OAuth authentication and JWT token exchange.
 *
 * <p>This store uses a throw-on-failure strategy: {@link #exchangeCode} throws
 * {@link com.demo.sso.exception.InvalidAuthCodeException} for invalid or expired codes.
 * This differs from {@link MicrosoftChallengeStore}'s Optional-return strategy
 * because auth code exchange is a critical authentication boundary where failures
 * must be explicit and auditable.</p>
 */
public interface AuthCodeStore {

    /** @throws IllegalArgumentException if jwt is null or blank */
    String createAuthCode(String jwt);

    /**
     * @throws com.demo.sso.exception.InvalidAuthCodeException if the code is invalid, expired, or already consumed
     */
    String exchangeCode(String code);
}
