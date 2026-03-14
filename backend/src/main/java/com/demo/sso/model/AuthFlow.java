package com.demo.sso.model;

public enum AuthFlow {
    SERVER_SIDE,
    CLIENT_SIDE;

    /**
     * Parses a string into an AuthFlow enum value.
     *
     * @param value case-insensitive enum name (e.g. "server_side", "CLIENT_SIDE")
     * @return the matching AuthFlow value
     * @throws IllegalArgumentException if value is null, blank, or not a valid AuthFlow
     */
    public static AuthFlow fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("value must not be null or blank");
        }

        try {
            return AuthFlow.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown auth flow: " + value.trim(), e);
        }
    }
}