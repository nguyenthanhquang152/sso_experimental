package com.demo.sso.controller.dto;

import com.demo.sso.dto.AuthApiResponse;

public record MicrosoftChallengeResponse(String challengeId, String nonce) implements AuthApiResponse {}