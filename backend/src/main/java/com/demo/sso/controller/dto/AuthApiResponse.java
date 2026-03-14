package com.demo.sso.controller.dto;

public sealed interface AuthApiResponse permits ErrorResponse, LogoutResponse, MicrosoftChallengeResponse, TokenResponse {
}