package com.demo.sso.service;

public interface MicrosoftChallengeStore {

    MicrosoftChallenge issueChallenge(String sessionId);

    String consumeNonce(String sessionId, String challengeId);

    record MicrosoftChallenge(String challengeId, String nonce) {}
}