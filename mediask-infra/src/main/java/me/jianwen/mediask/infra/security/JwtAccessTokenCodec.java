package me.jianwen.mediask.infra.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import javax.crypto.SecretKey;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.exception.ErrorCode;
import me.jianwen.mediask.domain.user.model.AccessToken;
import me.jianwen.mediask.domain.user.model.AccessTokenClaims;
import me.jianwen.mediask.domain.user.model.AuthenticatedUser;
import me.jianwen.mediask.domain.user.port.AccessTokenCodec;

public final class JwtAccessTokenCodec implements AccessTokenCodec {

    private final JwtProperties jwtProperties;
    private final Clock clock;
    private final SecretKey signingKey;

    public JwtAccessTokenCodec(JwtProperties jwtProperties, Clock clock) {
        this.jwtProperties = jwtProperties;
        this.clock = clock;
        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public AccessToken issueAccessToken(AuthenticatedUser authenticatedUser) {
        Instant now = clock.instant();
        Instant expiresAt = now.plusSeconds(jwtProperties.accessTokenExpireSeconds());
        String tokenId = UUID.randomUUID().toString();
        String tokenValue = Jwts.builder()
                .issuer(jwtProperties.issuer())
                .id(tokenId)
                .subject(String.valueOf(authenticatedUser.userId()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
        return new AccessToken(tokenValue, tokenId, expiresAt);
    }

    @Override
    public AccessTokenClaims parseAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw unauthorized();
        }
        try {
            Claims claims = Jwts.parser()
                    .clock(() -> Date.from(clock.instant()))
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(accessToken.trim())
                    .getPayload();
            if (!jwtProperties.issuer().equals(claims.getIssuer())) {
                throw unauthorized();
            }
            return new AccessTokenClaims(
                    parsePositiveLong(claims.getSubject(), "subject"),
                    parseRequiredString(claims.getId(), "jti"),
                    Objects.requireNonNull(claims.getExpiration(), "expiration must not be null").toInstant());
        } catch (BizException exception) {
            throw exception;
        } catch (JwtException | IllegalArgumentException exception) {
            throw unauthorized(exception);
        }
    }

    private Long parsePositiveLong(String value, String fieldName) {
        try {
            long parsed = Long.parseLong(parseRequiredString(value, fieldName));
            if (parsed <= 0L) {
                throw unauthorized();
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw unauthorized(exception);
        }
    }

    private String parseRequiredString(Object value, String fieldName) {
        if (value == null) {
            throw unauthorized();
        }
        String normalized = String.valueOf(value).trim();
        if (normalized.isEmpty()) {
            throw unauthorized();
        }
        return normalized;
    }

    private BizException unauthorized() {
        return new BizException(ErrorCode.UNAUTHORIZED);
    }

    private BizException unauthorized(Exception cause) {
        return new BizException(ErrorCode.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.getMessage(), cause);
    }
}
