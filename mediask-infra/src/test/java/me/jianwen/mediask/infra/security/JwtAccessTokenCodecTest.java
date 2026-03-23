package me.jianwen.mediask.infra.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import me.jianwen.mediask.domain.user.model.AuthenticatedUser;
import me.jianwen.mediask.domain.user.model.RoleCode;
import me.jianwen.mediask.domain.user.model.UserType;
import org.junit.jupiter.api.Test;

class JwtAccessTokenCodecTest {

    @Test
    void issueAccessTokenAndParseAccessToken_WhenTokenValid_ReturnAuthenticatedUser() {
        JwtProperties jwtProperties = new JwtProperties(
                "dev-only-jwt-secret-do-not-use-in-production-at-least-64-characters-long",
                "mediask",
                1800L);
        Clock fixedClock = Clock.fixed(Instant.parse("2026-03-22T08:00:00Z"), ZoneOffset.UTC);
        JwtAccessTokenCodec jwtAccessTokenCodec = new JwtAccessTokenCodec(jwtProperties, fixedClock);
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                2003L,
                "patient_li",
                "李患者",
                UserType.PATIENT,
                new LinkedHashSet<>(java.util.List.of(RoleCode.PATIENT)),
                2201L,
                null,
                null);

        String accessToken = jwtAccessTokenCodec.issueAccessToken(authenticatedUser);
        AuthenticatedUser parsedUser = jwtAccessTokenCodec.parseAccessToken(accessToken);

        assertEquals(authenticatedUser.userId(), parsedUser.userId());
        assertEquals(authenticatedUser.username(), parsedUser.username());
        assertEquals(authenticatedUser.displayName(), parsedUser.displayName());
        assertEquals(authenticatedUser.userType(), parsedUser.userType());
        assertIterableEquals(authenticatedUser.roles(), parsedUser.roles());
        assertEquals(authenticatedUser.patientId(), parsedUser.patientId());
        assertNull(parsedUser.doctorId());
        assertNull(parsedUser.primaryDepartmentId());
    }
}
