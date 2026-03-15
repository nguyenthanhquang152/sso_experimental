package com.demo.sso.controller.dto;

import com.demo.sso.dto.AuthApiResponse;

public record LogoutResponse(String message) implements AuthApiResponse {
}