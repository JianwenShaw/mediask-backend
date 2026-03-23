package me.jianwen.mediask.infra.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.crypto.SecretKey;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.exception.ErrorCode;
import me.jianwen.mediask.domain.user.model.AuthenticatedUser;
import me.jianwen.mediask.domain.user.model.RoleCode;
import me.jianwen.mediask.domain.user.model.UserType;
import me.jianwen.mediask.domain.user.port.AccessTokenCodec;

public final class JwtAccessTokenCodec implements AccessTokenCodec {

    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_DISPLAY_NAME = "displayName";
    private static final String CLAIM_USER_TYPE = "userType";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_PATIENT_ID = "patientId";
    private static final String CLAIM_DOCTOR_ID = "doctorId";
    private static final String CLAIM_PRIMARY_DEPARTMENT_ID = "primaryDepartmentId";

    private final JwtProperties jwtProperties;
    private final Clock clock;
    private final SecretKey signingKey;

    public JwtAccessTokenCodec(JwtProperties jwtProperties, Clock clock) {
        this.jwtProperties = jwtProperties;
        this.clock = clock;
        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String issueAccessToken(AuthenticatedUser authenticatedUser) {
        Instant now = clock.instant();
        Instant expiresAt = now.plusSeconds(jwtProperties.accessTokenExpireSeconds());
        return Jwts.builder()
                .issuer(jwtProperties.issuer())
                .subject(String.valueOf(authenticatedUser.userId()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim(CLAIM_USERNAME, authenticatedUser.username())
                .claim(CLAIM_DISPLAY_NAME, authenticatedUser.displayName())
                .claim(CLAIM_USER_TYPE, authenticatedUser.userType().name())
                .claim(
                        CLAIM_ROLES,
                        authenticatedUser.roles().stream().map(RoleCode::name).toList())
                .claim(CLAIM_PATIENT_ID, authenticatedUser.patientId())
                .claim(CLAIM_DOCTOR_ID, authenticatedUser.doctorId())
                .claim(CLAIM_PRIMARY_DEPARTMENT_ID, authenticatedUser.primaryDepartmentId())
                .signWith(signingKey)
                .compact();
    }

    @Override
    public AuthenticatedUser parseAccessToken(String accessToken) {
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
            return new AuthenticatedUser(
                    parsePositiveLong(claims.getSubject(), "subject"),
                    parseRequiredString(claims.get(CLAIM_USERNAME), CLAIM_USERNAME),
                    parseRequiredString(claims.get(CLAIM_DISPLAY_NAME), CLAIM_DISPLAY_NAME),
                    UserType.fromCode(parseRequiredString(claims.get(CLAIM_USER_TYPE), CLAIM_USER_TYPE)),
                    parseRoles(claims.get(CLAIM_ROLES)),
                    parseNullableLong(claims.get(CLAIM_PATIENT_ID), CLAIM_PATIENT_ID),
                    parseNullableLong(claims.get(CLAIM_DOCTOR_ID), CLAIM_DOCTOR_ID),
                    parseNullableLong(claims.get(CLAIM_PRIMARY_DEPARTMENT_ID), CLAIM_PRIMARY_DEPARTMENT_ID));
        } catch (BizException exception) {
            throw exception;
        } catch (JwtException | IllegalArgumentException exception) {
            throw unauthorized(exception);
        }
    }

    private Set<RoleCode> parseRoles(Object value) {
        if (!(value instanceof Collection<?> collection) || collection.isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<RoleCode> roles = new LinkedHashSet<>();
        for (Object roleValue : collection) {
            roles.add(RoleCode.fromCode(parseRequiredString(roleValue, CLAIM_ROLES)));
        }
        return Collections.unmodifiableSet(roles);
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

    private Long parseNullableLong(Object value, String fieldName) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            long parsed = number.longValue();
            if (parsed <= 0L) {
                throw unauthorized();
            }
            return parsed;
        }
        return parsePositiveLong(String.valueOf(value), fieldName);
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
