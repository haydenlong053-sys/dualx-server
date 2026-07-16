package com.app.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "com.app.secure-request", ignoreUnknownFields = true)
public class SecureRequestProperties {

    private boolean enabled = true;
    private String timestampHeader = "X-Timestamp";
    private String nonceHeader = "X-Nonce";
    private String signHeader = "X-Sign";
    private long timestampWindowSeconds = 300;
    private long nonceExpireSeconds = 300;
    private String signSecret = "";
    private String rsaPublicKey = "";
    private String rsaPrivateKey = "";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTimestampHeader() {
        return timestampHeader;
    }

    public void setTimestampHeader(String timestampHeader) {
        this.timestampHeader = timestampHeader;
    }

    public String getNonceHeader() {
        return nonceHeader;
    }

    public void setNonceHeader(String nonceHeader) {
        this.nonceHeader = nonceHeader;
    }

    public String getSignHeader() {
        return signHeader;
    }

    public void setSignHeader(String signHeader) {
        this.signHeader = signHeader;
    }

    public long getTimestampWindowSeconds() {
        return timestampWindowSeconds;
    }

    public void setTimestampWindowSeconds(long timestampWindowSeconds) {
        this.timestampWindowSeconds = timestampWindowSeconds;
    }

    public long getNonceExpireSeconds() {
        return nonceExpireSeconds;
    }

    public void setNonceExpireSeconds(long nonceExpireSeconds) {
        this.nonceExpireSeconds = nonceExpireSeconds;
    }

    public String getSignSecret() {
        return signSecret;
    }

    public void setSignSecret(String signSecret) {
        this.signSecret = signSecret;
    }

    public String getRsaPrivateKey() {
        return rsaPrivateKey;
    }

    public String getRsaPublicKey() {
        return rsaPublicKey;
    }

    public void setRsaPublicKey(String rsaPublicKey) {
        this.rsaPublicKey = rsaPublicKey;
    }

    public void setRsaPrivateKey(String rsaPrivateKey) {
        this.rsaPrivateKey = rsaPrivateKey;
    }
}
