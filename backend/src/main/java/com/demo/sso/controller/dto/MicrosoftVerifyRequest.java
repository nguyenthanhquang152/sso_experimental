package com.demo.sso.controller.dto;

public record MicrosoftVerifyRequest(String credential, String challengeId) {}