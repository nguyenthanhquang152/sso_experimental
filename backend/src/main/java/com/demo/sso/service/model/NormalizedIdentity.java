package com.demo.sso.service.model;

import com.demo.sso.model.AuthFlow;
import com.demo.sso.model.AuthProvider;

public record NormalizedIdentity(
    AuthProvider provider,
    String providerUserId,
    String email,
    String name,
    String pictureUrl,
    AuthFlow loginFlow
) {
    public static NormalizedIdentity google(
            String providerUserId,
            String email,
            String name,
            String pictureUrl,
            AuthFlow loginFlow) {
        return new NormalizedIdentity(AuthProvider.GOOGLE, providerUserId, email, name, pictureUrl, loginFlow);
    }

    public static NormalizedIdentity microsoft(
            String providerUserId,
            String email,
            String name,
            String pictureUrl,
            AuthFlow loginFlow) {
        return new NormalizedIdentity(AuthProvider.MICROSOFT, providerUserId, email, name, pictureUrl, loginFlow);
    }
}