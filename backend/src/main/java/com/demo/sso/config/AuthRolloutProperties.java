package com.demo.sso.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.auth")
public class AuthRolloutProperties {

    private IdentityContractMode identityContractMode = IdentityContractMode.LEGACY_ONLY;
    private JwtMintMode jwtMintMode = JwtMintMode.LEGACY;
    private final Microsoft microsoft = new Microsoft();

    public IdentityContractMode getIdentityContractMode() {
        return identityContractMode;
    }

    public void setIdentityContractMode(IdentityContractMode identityContractMode) {
        this.identityContractMode = identityContractMode;
    }

    public JwtMintMode getJwtMintMode() {
        return jwtMintMode;
    }

    public void setJwtMintMode(JwtMintMode jwtMintMode) {
        this.jwtMintMode = jwtMintMode;
    }

    public Microsoft getMicrosoft() {
        return microsoft;
    }

    public enum IdentityContractMode {
        LEGACY_ONLY,
        COMPATIBILITY,
        V2_ONLY;

        public boolean acceptsLegacy() {
            return this != V2_ONLY;
        }

        public boolean acceptsV2() {
            return this != LEGACY_ONLY;
        }
    }

    public enum JwtMintMode {
        LEGACY,
        V2
    }

    public static class Microsoft {
        private boolean serverSideEnabled;
        private boolean clientSideEnabled;

        public boolean isServerSideEnabled() {
            return serverSideEnabled;
        }

        public void setServerSideEnabled(boolean serverSideEnabled) {
            this.serverSideEnabled = serverSideEnabled;
        }

        public boolean isClientSideEnabled() {
            return clientSideEnabled;
        }

        public void setClientSideEnabled(boolean clientSideEnabled) {
            this.clientSideEnabled = clientSideEnabled;
        }
    }
}