package com.demo.sso.controller.dto;

import com.demo.sso.dto.AuthApiResponse;

public record TokenResponse(String token) implements AuthApiResponse {
}