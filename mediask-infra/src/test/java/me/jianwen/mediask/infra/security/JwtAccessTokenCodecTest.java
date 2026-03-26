package me.jianwen.mediask.infra.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.crypto.SecretKey;
import me.jianwen.mediask.domain.user.model.AccessToken;
import me.jianwen.mediask.domain.user.model.AccessTokenClaims;
import me.jianwen.mediask.domain.user.model.AuthenticatedUser;
import me.jianwen.mediask.domain.user.model.RoleCode;
import me.jianwen.mediask.domain.user.model.UserType;
import org.junit.jupiter.api.Test;

class JwtAccessTokenCodecTest {

    private static final String JWT_SECRET =
            "dev-only-jwt-secret-do-not-use-in-production-at-least-64-characters-long";
    private static final String JWT_ISSUER = "mediask";
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-03-22T08:00:00Z"), ZoneOffset.UTC);
    private static final JwtProperties JWT_PROPERTIES = new JwtProperties(JWT_SECRET, JWT_ISSUER, 1800L, 7L);
    private static final SecretKey SIGNING_KEY = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));

    @Test
    void issueAccessTokenAndParseAccessToken_WhenTokenValid_ReturnMinimalClaims() {
        String sessionId = "refresh-token-1";
        JwtAccessTokenCodec jwtAccessTokenCodec = new JwtAccessTokenCodec(JWT_PROPERTIES, FIXED_CLOCK);
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                2003L,
                "patient_li",
                "李患者",
                UserType.PATIENT,
                new LinkedHashSet<>(java.util.List.of(RoleCode.PATIENT)),
                Set.of("auth:logout", "patient:profile:view:self"),
                Set.of(),
                2201L,
                null,
                null);

        AccessToken accessToken = jwtAccessTokenCodec.issueAccessToken(authenticatedUser, sessionId);
        AccessTokenClaims parsedClaims = jwtAccessTokenCodec.parseAccessToken(accessToken.value());

        assertEquals(authenticatedUser.userId(), parsedClaims.userId());
        assertEquals(accessToken.tokenId(), parsedClaims.tokenId());
        assertEquals(sessionId, parsedClaims.sessionId());
        assertEquals(accessToken.expiresAt(), parsedClaims.expiresAt());
        assertTrue(accessToken.tokenId() != null && !accessToken.tokenId().isBlank());
    }

    @Test
    void parseAccessToken_WhenLegacyTokenWithoutSessionId_ReturnNullSessionId() {
        JwtAccessTokenCodec jwtAccessTokenCodec = new JwtAccessTokenCodec(JWT_PROPERTIES, FIXED_CLOCK);
        Instant issuedAt = FIXED_CLOCK.instant();
        Instant expiresAt = issuedAt.plusSeconds(JWT_PROPERTIES.accessTokenExpireSeconds());
        String legacyToken = Jwts.builder()
                .issuer(JWT_ISSUER)
                .id("legacy-token-id")
                .subject("2003")
                .issuedAt(java.util.Date.from(issuedAt))
                .expiration(java.util.Date.from(expiresAt))
                .signWith(SIGNING_KEY)
                .compact();

        AccessTokenClaims parsedClaims = jwtAccessTokenCodec.parseAccessToken(legacyToken);

        assertEquals(2003L, parsedClaims.userId());
        assertEquals("legacy-token-id", parsedClaims.tokenId());
        assertNull(parsedClaims.sessionId());
        assertEquals(expiresAt, parsedClaims.expiresAt());
    }
}
