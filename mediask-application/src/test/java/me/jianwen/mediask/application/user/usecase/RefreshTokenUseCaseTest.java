package me.jianwen.mediask.application.user.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import me.jianwen.mediask.application.user.command.RefreshTokenCommand;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.user.exception.UserErrorCode;
import me.jianwen.mediask.domain.user.model.AccessToken;
import me.jianwen.mediask.domain.user.model.AccessTokenClaims;
import me.jianwen.mediask.domain.user.model.AuthenticatedUser;
import me.jianwen.mediask.domain.user.model.LoginAccount;
import me.jianwen.mediask.domain.user.model.RefreshTokenSession;
import me.jianwen.mediask.domain.user.model.RoleCode;
import me.jianwen.mediask.domain.user.model.UserType;
import me.jianwen.mediask.domain.user.port.AccessTokenCodec;
import me.jianwen.mediask.domain.user.port.RefreshTokenManager;
import me.jianwen.mediask.domain.user.port.RefreshTokenStore;
import me.jianwen.mediask.domain.user.port.UserAuthenticationRepository;
import org.junit.jupiter.api.Test;

class RefreshTokenUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-03-25T12:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-03-30T12:00:00Z");
    private static final String CURRENT_REFRESH_TOKEN = "rt.2003.refresh-token-1.refresh-secret-1";

    @Test
    void handle_WhenRefreshTokenRequestedConcurrently_OnlyOneSucceeds() throws Exception {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                2003L,
                "patient_li",
                "Patient Li",
                UserType.PATIENT,
                new LinkedHashSet<>(java.util.List.of(RoleCode.PATIENT)),
                Set.of("auth:refresh", "patient:profile:view:self"),
                Set.of(),
                2201L,
                null,
                null);
        ConcurrentRefreshTokenSupport refreshTokenSupport = new ConcurrentRefreshTokenSupport();
        refreshTokenSupport.save(new RefreshTokenSession(
                authenticatedUser.userId(), "refresh-token-1", "refresh-secret-1", EXPIRES_AT));
        RefreshTokenUseCase useCase = new RefreshTokenUseCase(
                new StubUserAuthenticationRepository(authenticatedUser),
                new StubAccessTokenCodec(),
                refreshTokenSupport,
                refreshTokenSupport,
                Clock.fixed(NOW, ZoneOffset.UTC));

        int concurrentRequests = 8;
        ExecutorService executorService = Executors.newFixedThreadPool(concurrentRequests);
        CountDownLatch readyLatch = new CountDownLatch(concurrentRequests);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < concurrentRequests; i++) {
            futures.add(executorService.submit(new RefreshAttempt(useCase, readyLatch, startLatch)));
        }

        assertTrue(readyLatch.await(5, TimeUnit.SECONDS));
        startLatch.countDown();

        int successCount = 0;
        int invalidRefreshTokenCount = 0;
        for (Future<Boolean> future : futures) {
            try {
                if (future.get()) {
                    successCount++;
                }
            } catch (java.util.concurrent.ExecutionException exception) {
                Throwable cause = exception.getCause();
                if (cause instanceof BizException bizException
                        && bizException.getCode() == UserErrorCode.INVALID_REFRESH_TOKEN.getCode()) {
                    invalidRefreshTokenCount++;
                    continue;
                }
                throw exception;
            }
        }

        executorService.shutdownNow();

        assertEquals(1, successCount);
        assertEquals(concurrentRequests - 1, invalidRefreshTokenCount);
        assertTrue(refreshTokenSupport.findByTokenValue(CURRENT_REFRESH_TOKEN).isEmpty());
        assertEquals(1, refreshTokenSupport.activeSessionCount());
        assertFalse(refreshTokenSupport.latestIssuedTokenValue().isBlank());
    }

    private record RefreshAttempt(
            RefreshTokenUseCase useCase, CountDownLatch readyLatch, CountDownLatch startLatch) implements Callable<Boolean> {

        @Override
        public Boolean call() throws Exception {
            readyLatch.countDown();
            assertTrue(startLatch.await(5, TimeUnit.SECONDS));
            useCase.handle(new RefreshTokenCommand(CURRENT_REFRESH_TOKEN));
            return true;
        }
    }

    private static final class StubUserAuthenticationRepository implements UserAuthenticationRepository {

        private final AuthenticatedUser authenticatedUser;

        private StubUserAuthenticationRepository(AuthenticatedUser authenticatedUser) {
            this.authenticatedUser = authenticatedUser;
        }

        @Override
        public Optional<LoginAccount> findLoginAccountByPhone(String phone) {
            return Optional.empty();
        }

        @Override
        public Optional<AuthenticatedUser> findAuthenticatedUserById(Long userId) {
            if (authenticatedUser.userId().equals(userId)) {
                return Optional.of(authenticatedUser);
            }
            return Optional.empty();
        }

        @Override
        public void updateLastLoginAt(Long userId) {
        }
    }

    private static final class StubAccessTokenCodec implements AccessTokenCodec {

        private final AtomicInteger issueSequence = new AtomicInteger();

        @Override
        public AccessToken issueAccessToken(AuthenticatedUser authenticatedUser, String sessionId) {
            int sequence = issueSequence.incrementAndGet();
            return new AccessToken("access-token-" + sequence, "access-token-id-" + sequence, EXPIRES_AT);
        }

        @Override
        public AccessTokenClaims parseAccessToken(String accessToken) {
            throw new UnsupportedOperationException("not needed for refresh tests");
        }
    }

    private static final class ConcurrentRefreshTokenSupport implements RefreshTokenManager, RefreshTokenStore {

        private final ConcurrentHashMap<String, RefreshTokenSession> sessions = new ConcurrentHashMap<>();
        private final AtomicInteger sequence = new AtomicInteger(1);
        private volatile String latestIssuedTokenValue;

        @Override
        public RefreshTokenSession issue(Long userId) {
            int currentSequence = sequence.incrementAndGet();
            RefreshTokenSession session = new RefreshTokenSession(
                    userId,
                    "refresh-token-" + currentSequence,
                    "refresh-secret-" + currentSequence,
                    EXPIRES_AT);
            latestIssuedTokenValue = session.tokenValue();
            return session;
        }

        @Override
        public void save(RefreshTokenSession refreshTokenSession) {
            sessions.put(refreshTokenSession.tokenValue(), refreshTokenSession);
            latestIssuedTokenValue = refreshTokenSession.tokenValue();
        }

        @Override
        public synchronized boolean rotate(String currentRefreshToken, RefreshTokenSession nextRefreshTokenSession) {
            RefreshTokenSession removed = sessions.remove(currentRefreshToken);
            if (removed == null) {
                return false;
            }
            sessions.put(nextRefreshTokenSession.tokenValue(), nextRefreshTokenSession);
            latestIssuedTokenValue = nextRefreshTokenSession.tokenValue();
            return true;
        }

        @Override
        public Optional<RefreshTokenSession> findByTokenValue(String refreshToken) {
            return Optional.ofNullable(sessions.get(refreshToken));
        }

        @Override
        public void deleteByTokenValue(String refreshToken) {
            sessions.remove(refreshToken);
        }

        private int activeSessionCount() {
            return sessions.size();
        }

        private String latestIssuedTokenValue() {
            return latestIssuedTokenValue == null ? "" : latestIssuedTokenValue;
        }
    }
}
