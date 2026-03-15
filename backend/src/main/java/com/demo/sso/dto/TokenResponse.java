package com.demo.sso.dto;

public record TokenResponse(String token) implements AuthApiResponse {
}