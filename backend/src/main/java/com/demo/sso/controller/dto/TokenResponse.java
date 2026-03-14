package com.demo.sso.controller.dto;

public record TokenResponse(String token) implements AuthApiResponse {
}