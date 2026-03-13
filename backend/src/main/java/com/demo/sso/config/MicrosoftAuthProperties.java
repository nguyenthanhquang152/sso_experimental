package com.demo.sso.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.microsoft")
public class MicrosoftAuthProperties {

    private String clientId;
    private String clientSecret;
    private String authority = "https://login.microsoftonline.com/common/v2.0";
    private List<String> scopes = new ArrayList<>(List.of("openid", "profile", "email"));

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes == null ? new ArrayList<>() : new ArrayList<>(scopes);
    }

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank()
            && authority != null && !authority.isBlank();
    }
}