package com.demo.sso.dto;

/**
 * Sealed marker interface for all authentication API response types.
 *
 * <p>Every concrete response returned by {@code AuthController} must be listed
 * in the {@code permits} clause so the compiler can verify exhaustiveness.
 */
public sealed interface AuthApiResponse
        permits ErrorResponse, TokenResponse, LogoutResponse, MicrosoftChallengeResponse {
}