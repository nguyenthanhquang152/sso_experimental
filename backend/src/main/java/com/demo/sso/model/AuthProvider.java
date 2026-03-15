package com.demo.sso.model;

public enum AuthProvider {
    GOOGLE,
    MICROSOFT;

    /**
     * Resolves an OAuth2 client registration ID (e.g. {@code "google"}) to the
     * corresponding enum constant (case-insensitive).
     *
     * @throws IllegalArgumentException if no constant matches
     */
    public static AuthProvider fromRegistrationId(String registrationId) {
        for (AuthProvider provider : values()) {
            if (provider.name().equalsIgnoreCase(registrationId)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("Unsupported provider: " + registrationId);
    }
}