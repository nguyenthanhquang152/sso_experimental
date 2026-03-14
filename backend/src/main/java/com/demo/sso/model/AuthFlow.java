package com.demo.sso.model;

public enum AuthFlow {
    SERVER_SIDE,
    CLIENT_SIDE;

    /**
     * Parses a login method string into an AuthFlow enum value.
     *
     * @param loginMethod case-insensitive enum name (e.g. "server_side", "CLIENT_SIDE")
     * @return the matching AuthFlow value
     * @throws IllegalArgumentException if loginMethod is null, blank, or not a valid AuthFlow
     */
    public static AuthFlow fromLoginMethod(String loginMethod) {
        if (loginMethod == null || loginMethod.isBlank()) {
            throw new IllegalArgumentException("loginMethod must not be null or blank");
        }

        try {
            return AuthFlow.valueOf(loginMethod.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown auth flow: " + loginMethod.trim(), e);
        }
    }
}