package me.jianwen.mediask.infra.security;

import java.nio.charset.StandardCharsets;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "mediask.jwt")
public record JwtProperties(
        String secret,
        @DefaultValue("mediask") String issuer,
        @DefaultValue("1800") long accessTokenExpireSeconds,
        @DefaultValue("7") long refreshTokenExpireDays) {

    public JwtProperties {
        secret = requireSecret(secret);
        issuer = requireNonBlank(issuer, "mediask.jwt.issuer");
        if (accessTokenExpireSeconds <= 0L) {
            throw new IllegalArgumentException("mediask.jwt.access-token-expire-seconds must be greater than 0");
        }
        if (refreshTokenExpireDays <= 0L) {
            throw new IllegalArgumentException("mediask.jwt.refresh-token-expire-days must be greater than 0");
        }
    }

    private static String requireSecret(String value) {
        String normalized = requireNonBlank(value, "mediask.jwt.secret");
        if (normalized.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("mediask.jwt.secret must be at least 32 bytes");
        }
        return normalized;
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
