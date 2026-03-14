package com.demo.sso.controller.dto;

public record ErrorResponse(String error) implements AuthApiResponse {
}