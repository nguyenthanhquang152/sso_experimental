package com.demo.sso.service.challenge;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Generates cryptographically secure URL-safe codes.
 * Shared between production and test code-store implementations.
 */
public final class SecureCodeGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private SecureCodeGenerator() {}

    public static String generate() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
