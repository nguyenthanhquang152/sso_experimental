package com.demo.sso.dto;

public record MicrosoftChallengeResponse(String challengeId, String nonce) implements AuthApiResponse {}