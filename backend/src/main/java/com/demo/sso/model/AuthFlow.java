package com.demo.sso.model;

public enum AuthFlow {
    SERVER_SIDE,
    CLIENT_SIDE;

    public static AuthFlow fromLoginMethod(String loginMethod) {
        if (loginMethod == null || loginMethod.isBlank()) {
            return null;
        }

        try {
            return AuthFlow.valueOf(loginMethod.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}