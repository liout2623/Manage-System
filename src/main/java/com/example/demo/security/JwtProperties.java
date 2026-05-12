package com.example.demo.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    private String secret;
    private long expireSeconds;
    private boolean secure = false; // Cookie secure 标志，生产环境应设为 true

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public long getExpireSeconds() { return expireSeconds; }
    public void setExpireSeconds(long expireSeconds) { this.expireSeconds = expireSeconds; }

    public boolean isSecure() { return secure; }
    public void setSecure(boolean secure) { this.secure = secure; }
}
