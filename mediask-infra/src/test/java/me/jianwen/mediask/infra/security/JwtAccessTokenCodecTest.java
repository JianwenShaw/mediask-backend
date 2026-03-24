package me.jianwen.mediask.infra.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.Set;
import me.jianwen.mediask.domain.user.model.AccessToken;
import me.jianwen.mediask.domain.user.model.AccessTokenClaims;
import me.jianwen.mediask.domain.user.model.AuthenticatedUser;
import me.jianwen.mediask.domain.user.model.RoleCode;
import me.jianwen.mediask.domain.user.model.UserType;
import org.junit.jupiter.api.Test;

class JwtAccessTokenCodecTest {

    @Test
    void issueAccessTokenAndParseAccessToken_WhenTokenValid_ReturnMinimalClaims() {
        JwtProperties jwtProperties = new JwtProperties(
                "dev-only-jwt-secret-do-not-use-in-production-at-least-64-characters-long",
                "mediask",
                1800L,
                7L);
        Clock fixedClock = Clock.fixed(Instant.parse("2026-03-22T08:00:00Z"), ZoneOffset.UTC);
        JwtAccessTokenCodec jwtAccessTokenCodec = new JwtAccessTokenCodec(jwtProperties, fixedClock);
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                2003L,
                "patient_li",
                "李患者",
                UserType.PATIENT,
                new LinkedHashSet<>(java.util.List.of(RoleCode.PATIENT)),
                Set.of("auth:logout", "patient:profile:view:self"),
                2201L,
                null,
                null);

        AccessToken accessToken = jwtAccessTokenCodec.issueAccessToken(authenticatedUser);
        AccessTokenClaims parsedClaims = jwtAccessTokenCodec.parseAccessToken(accessToken.value());

        assertEquals(authenticatedUser.userId(), parsedClaims.userId());
        assertEquals(accessToken.tokenId(), parsedClaims.tokenId());
        assertEquals(accessToken.expiresAt(), parsedClaims.expiresAt());
        assertTrue(accessToken.tokenId() != null && !accessToken.tokenId().isBlank());
    }
}
