package me.jianwen.mediask.application.user.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import me.jianwen.mediask.application.user.command.LogoutCommand;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.user.exception.UserErrorCode;
import me.jianwen.mediask.domain.user.model.AccessTokenClaims;
import me.jianwen.mediask.domain.user.model.RefreshTokenSession;
import me.jianwen.mediask.domain.user.port.AccessTokenBlocklistPort;
import me.jianwen.mediask.domain.user.port.AccessTokenCodec;
import me.jianwen.mediask.domain.user.port.RefreshTokenStore;
import org.junit.jupiter.api.Test;

class LogoutUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-03-25T12:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-03-30T12:00:00Z");
    private static final String PRIMARY_REFRESH_TOKEN = "rt.2003.refresh-token-1.refresh-secret-1";
    private static final String LEGACY_ACCESS_TOKEN = "legacy-access-token";
    private static final String PRIMARY_TOKEN_ID = "access-token-id-1";

    @Test
    void handle_WhenAccessTokenHasNoSessionId_ThrowPermissionDenied() {
        InMemoryRefreshTokenStore refreshTokenStore = new InMemoryRefreshTokenStore();
        refreshTokenStore.save(new RefreshTokenSession(2003L, "refresh-token-1", "refresh-secret-1", EXPIRES_AT));
        StubAccessTokenBlocklistPort accessTokenBlocklistPort = new StubAccessTokenBlocklistPort();
        LogoutUseCase useCase = new LogoutUseCase(
                refreshTokenStore,
                accessTokenBlocklistPort,
                new StubAccessTokenCodec(),
                Clock.fixed(NOW, ZoneOffset.UTC));

        BizException exception = assertThrows(
                BizException.class,
                () -> useCase.handle(new LogoutCommand(PRIMARY_REFRESH_TOKEN, LEGACY_ACCESS_TOKEN, 2003L)));

        assertEquals(UserErrorCode.PERMISSION_DENIED.getCode(), exception.getCode());
        assertTrue(refreshTokenStore.findByTokenValue(PRIMARY_REFRESH_TOKEN).isPresent());
        assertFalse(accessTokenBlocklistPort.isBlocked(PRIMARY_TOKEN_ID));
    }

    @Test
    void handle_WhenBlocklistWriteFails_KeepRefreshTokenAvailable() {
        InMemoryRefreshTokenStore refreshTokenStore = new InMemoryRefreshTokenStore();
        refreshTokenStore.save(new RefreshTokenSession(2003L, "refresh-token-1", "refresh-secret-1", EXPIRES_AT));
        StubAccessTokenBlocklistPort accessTokenBlocklistPort = new StubAccessTokenBlocklistPort();
        accessTokenBlocklistPort.failOnBlock(PRIMARY_TOKEN_ID);
        LogoutUseCase useCase = new LogoutUseCase(
                refreshTokenStore,
                accessTokenBlocklistPort,
                new CurrentSessionAccessTokenCodec(),
                Clock.fixed(NOW, ZoneOffset.UTC));

        assertThrows(
                IllegalStateException.class,
                () -> useCase.handle(new LogoutCommand(PRIMARY_REFRESH_TOKEN, "current-session-access-token", 2003L)));

        assertTrue(refreshTokenStore.findByTokenValue(PRIMARY_REFRESH_TOKEN).isPresent());
        assertFalse(accessTokenBlocklistPort.isBlocked(PRIMARY_TOKEN_ID));
    }

    private static final class StubAccessTokenCodec implements AccessTokenCodec {

        @Override
        public me.jianwen.mediask.domain.user.model.AccessToken issueAccessToken(
                me.jianwen.mediask.domain.user.model.AuthenticatedUser authenticatedUser, String sessionId) {
            throw new UnsupportedOperationException("not needed for logout tests");
        }

        @Override
        public AccessTokenClaims parseAccessToken(String accessToken) {
            if (LEGACY_ACCESS_TOKEN.equals(accessToken)) {
                return new AccessTokenClaims(2003L, PRIMARY_TOKEN_ID, null, EXPIRES_AT);
            }
            throw new BizException(UserErrorCode.PERMISSION_DENIED);
        }
    }

    private static final class CurrentSessionAccessTokenCodec implements AccessTokenCodec {

        @Override
        public me.jianwen.mediask.domain.user.model.AccessToken issueAccessToken(
                me.jianwen.mediask.domain.user.model.AuthenticatedUser authenticatedUser, String sessionId) {
            throw new UnsupportedOperationException("not needed for logout tests");
        }

        @Override
        public AccessTokenClaims parseAccessToken(String accessToken) {
            if ("current-session-access-token".equals(accessToken)) {
                return new AccessTokenClaims(2003L, PRIMARY_TOKEN_ID, "refresh-token-1", EXPIRES_AT);
            }
            throw new BizException(UserErrorCode.PERMISSION_DENIED);
        }
    }

    private static final class InMemoryRefreshTokenStore implements RefreshTokenStore {

        private final ConcurrentHashMap<String, RefreshTokenSession> sessions = new ConcurrentHashMap<>();

        @Override
        public void save(RefreshTokenSession refreshTokenSession) {
            sessions.put(refreshTokenSession.tokenValue(), refreshTokenSession);
        }

        @Override
        public boolean rotate(String currentRefreshToken, RefreshTokenSession nextRefreshTokenSession) {
            throw new UnsupportedOperationException("not needed for logout tests");
        }

        @Override
        public Optional<RefreshTokenSession> findByTokenValue(String refreshToken) {
            return Optional.ofNullable(sessions.get(refreshToken));
        }

        @Override
        public void deleteByTokenValue(String refreshToken) {
            sessions.remove(refreshToken);
        }
    }

    private static final class StubAccessTokenBlocklistPort implements AccessTokenBlocklistPort {

        private final Set<String> blockedTokenIds = ConcurrentHashMap.newKeySet();
        private final Set<String> failingTokenIds = ConcurrentHashMap.newKeySet();

        private void failOnBlock(String tokenId) {
            failingTokenIds.add(tokenId);
        }

        @Override
        public void block(String tokenId, Instant expiresAt) {
            if (failingTokenIds.contains(tokenId)) {
                throw new IllegalStateException("redis unavailable");
            }
            blockedTokenIds.add(tokenId);
        }

        @Override
        public boolean isBlocked(String tokenId) {
            return blockedTokenIds.contains(tokenId);
        }
    }
}
