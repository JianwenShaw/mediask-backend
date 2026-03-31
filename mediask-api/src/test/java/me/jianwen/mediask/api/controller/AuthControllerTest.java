package me.jianwen.mediask.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import me.jianwen.mediask.api.advice.ResultResponseBodyAdvice;
import me.jianwen.mediask.api.exception.GlobalExceptionHandler;
import me.jianwen.mediask.api.filter.RequestCorrelationFilter;
import me.jianwen.mediask.api.security.AuthenticatedUserPrincipal;
import me.jianwen.mediask.api.security.ApiCorsProperties;
import me.jianwen.mediask.api.security.ApiSecurityProperties;
import me.jianwen.mediask.api.security.JsonAuthenticationEntryPoint;
import me.jianwen.mediask.api.security.JwtAuthenticationFilter;
import me.jianwen.mediask.api.security.SecurityConfig;
import me.jianwen.mediask.application.user.usecase.GetCurrentUserUseCase;
import me.jianwen.mediask.application.user.usecase.LoginUseCase;
import me.jianwen.mediask.application.user.usecase.LogoutUseCase;
import me.jianwen.mediask.application.user.usecase.RefreshTokenUseCase;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.exception.ErrorCode;
import me.jianwen.mediask.common.result.Result;
import me.jianwen.mediask.domain.user.model.AccessToken;
import me.jianwen.mediask.domain.user.model.AccessTokenClaims;
import me.jianwen.mediask.domain.user.model.AccountStatus;
import me.jianwen.mediask.domain.user.model.AuthenticatedUser;
import me.jianwen.mediask.domain.user.model.LoginAccount;
import me.jianwen.mediask.domain.user.model.RefreshTokenSession;
import me.jianwen.mediask.domain.user.model.RoleCode;
import me.jianwen.mediask.domain.user.model.UserType;
import me.jianwen.mediask.domain.user.port.AccessTokenBlocklistPort;
import me.jianwen.mediask.domain.user.port.AccessTokenCodec;
import me.jianwen.mediask.domain.user.port.PasswordVerifier;
import me.jianwen.mediask.domain.user.port.RefreshTokenManager;
import me.jianwen.mediask.domain.user.port.RefreshTokenStore;
import me.jianwen.mediask.domain.user.port.UserAuthenticationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

class AuthControllerTest {

    private static final String VALID_TOKEN = "valid-token";
    private static final String VALID_TOKEN_ID = "valid-token-id";
    private static final String VALID_TOKEN_OTHER_SESSION = "valid-token-other-session";
    private static final String VALID_TOKEN_OTHER_SESSION_ID = "valid-token-other-session-id";
    private static final String VALID_TOKEN_OTHER_USER = "valid-token-other-user";
    private static final String VALID_TOKEN_OTHER_USER_ID = "valid-token-other-user-id";
    private static final String LEGACY_TOKEN_WITHOUT_SESSION = "legacy-token-without-session";
    private static final String LEGACY_TOKEN_WITHOUT_SESSION_ID = "legacy-token-without-session-id";
    private static final String PRIMARY_REFRESH_TOKEN = "rt.2003.refresh-token-1.refresh-secret-1";
    private static final String SAME_USER_OTHER_SESSION_REFRESH_TOKEN =
            "rt.2003.refresh-token-other-session.refresh-secret-other-session";
    private static final String OTHER_USER_REFRESH_TOKEN = "rt.2005.refresh-token-other-user.refresh-secret-other-user";
    private static final Instant TEST_NOW = Instant.parse("2026-03-30T07:00:00Z");
    private static final Clock TEST_CLOCK = Clock.fixed(TEST_NOW, ZoneOffset.UTC);
    private static final Instant TOKEN_EXPIRES_AT = TEST_NOW.plusSeconds(3600);

    private MockMvc mockMvc;
    private InMemoryRefreshTokenSupport refreshTokenSupport;
    private StubAccessTokenBlocklistPort accessTokenBlocklistPort;
    private StubUserAuthenticationRepository repository;
    private StubAccessTokenCodec accessTokenCodec;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final AuthenticatedUser authenticatedUser = new AuthenticatedUser(
            2003L,
            "patient_li",
            "李患者",
            UserType.PATIENT,
            new LinkedHashSet<>(java.util.List.of(RoleCode.PATIENT)),
            Set.of("auth:refresh", "patient:profile:view:self"),
            Set.of(),
            2201L,
            null,
            null);
    private final AuthenticatedUser otherUser = new AuthenticatedUser(
            2005L,
            "patient_wang",
            "王患者",
            UserType.PATIENT,
            new LinkedHashSet<>(java.util.List.of(RoleCode.PATIENT)),
            Set.of("auth:refresh", "patient:profile:view:self"),
            Set.of(),
            2205L,
            null,
            null);

    @BeforeEach
    void setUp() {
        refreshTokenSupport = new InMemoryRefreshTokenSupport();
        accessTokenBlocklistPort = new StubAccessTokenBlocklistPort();
        repository = new StubUserAuthenticationRepository(authenticatedUser, otherUser);
        accessTokenCodec = new StubAccessTokenCodec(authenticatedUser);
        PasswordVerifier passwordVerifier = (rawPassword, encodedPassword) ->
                encodedPassword.equals("hash<" + rawPassword + ">");

        RefreshTokenSession initialRefreshToken = refreshTokenSupport.issue(authenticatedUser.userId());
        refreshTokenSupport.save(initialRefreshToken);
        refreshTokenSupport.save(new RefreshTokenSession(
                authenticatedUser.userId(),
                "refresh-token-other-session",
                "refresh-secret-other-session",
                TOKEN_EXPIRES_AT));
        refreshTokenSupport.save(new RefreshTokenSession(
                otherUser.userId(), "refresh-token-other-user", "refresh-secret-other-user", TOKEN_EXPIRES_AT));

        AuthController authController = new AuthController(
                new LoginUseCase(repository, passwordVerifier, accessTokenCodec, refreshTokenSupport, refreshTokenSupport),
                new RefreshTokenUseCase(repository, accessTokenCodec, refreshTokenSupport, refreshTokenSupport, TEST_CLOCK),
                new LogoutUseCase(
                        refreshTokenSupport,
                        accessTokenBlocklistPort,
                        accessTokenCodec,
                        TEST_CLOCK),
                new GetCurrentUserUseCase(repository));

        JsonAuthenticationEntryPoint authenticationEntryPoint = new JsonAuthenticationEntryPoint(objectMapper);
        SecurityConfig securityConfig = new SecurityConfig();
        RequestMatcher publicRequestMatcher = securityConfig.publicRequestMatcher(new ApiSecurityProperties(false));
        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(
                accessTokenCodec,
                accessTokenBlocklistPort,
                repository,
                authenticationEntryPoint,
                objectMapper,
                publicRequestMatcher);
        CorsFilter corsFilter = new CorsFilter(securityConfig.corsConfigurationSource(new ApiCorsProperties(
                java.util.List.of("http://localhost:3000", "http://localhost:5173"),
                null,
                null,
                true,
                3600L)));

        Filter securityContextCleanupFilter = (request, response, chain) -> {
            try {
                chain.doFilter(request, response);
            } finally {
                SecurityContextHolder.clearContext();
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(authController, new ProtectedTestController())
                .setControllerAdvice(new GlobalExceptionHandler(), new ResultResponseBodyAdvice())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setMessageConverters(
                        new StringHttpMessageConverter(),
                        new MappingJackson2HttpMessageConverter(objectMapper))
                .addFilters(new RequestCorrelationFilter(), corsFilter, jwtAuthenticationFilter, securityContextCleanupFilter)
                .build();
    }

    @Test
    void login_WhenCredentialsValid_ReturnTokensAndUserContext() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "patient_li",
                                  "password": "patient123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.requestId").isNotEmpty())
                .andExpect(jsonPath("$.data.accessToken").value("issued-access-token-1"))
                .andExpect(jsonPath("$.data.refreshToken").value("rt.2003.refresh-token-2.refresh-secret-2"))
                .andExpect(jsonPath("$.data.userContext.userId").value(2003))
                .andExpect(jsonPath("$.data.userContext.roles[0]").value("PATIENT"))
                .andExpect(jsonPath("$.data.userContext.permissions[0]").value("auth:refresh"))
                .andExpect(jsonPath("$.data.userContext.patientId").value(2201));

        assertEquals("refresh-token-2", accessTokenCodec.lastIssuedSessionId());
    }

    @Test
    void login_WhenPasswordContainsLeadingAndTrailingSpaces_PreserveRawPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "patient_space",
                                  "password": "  secret-pass  "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userContext.userId").value(2004))
                .andExpect(jsonPath("$.data.userContext.username").value("patient_space"))
                .andExpect(jsonPath("$.data.userContext.roles[0]").value("PATIENT"))
                .andExpect(jsonPath("$.data.userContext.permissions[0]").value("auth:refresh"))
                .andExpect(jsonPath("$.data.userContext.patientId").value(2202));
    }

    @Test
    void login_WhenInvalidBearerTokenPresent_StillReturnsTokensAndUserContext() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .header("Authorization", "Bearer expired-or-invalid-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "patient_li",
                                  "password": "patient123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").value("issued-access-token-1"));
    }

    @Test
    void login_WhenCrossOriginRequestAllowed_ReturnCorsHeaders() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .header("Origin", "http://localhost:3000")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "patient_li",
                                  "password": "patient123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void refresh_WhenRefreshTokenValid_ReturnRotatedTokensAndUserContext() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "rt.2003.refresh-token-1.refresh-secret-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").value("issued-access-token-1"))
                .andExpect(jsonPath("$.data.refreshToken").value("rt.2003.refresh-token-2.refresh-secret-2"))
                .andExpect(jsonPath("$.data.userContext.permissions[0]").exists());

        assertEquals("refresh-token-2", accessTokenCodec.lastIssuedSessionId());
    }

    @Test
    void logout_WhenTokenValid_ReturnSuccess() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + VALID_TOKEN)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "rt.2003.refresh-token-1.refresh-secret-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value(0));

        assertTrue(accessTokenBlocklistPort.isBlocked(VALID_TOKEN_ID));
        assertTrue(refreshTokenSupport.findByTokenValue(PRIMARY_REFRESH_TOKEN).isEmpty());
    }

    @Test
    void logout_WhenAccessTokenExpiredButRefreshTokenValid_ReturnSuccess() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer expired-or-invalid-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "rt.2003.refresh-token-1.refresh-secret-1"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value(1001));

        assertTrue(refreshTokenSupport.findByTokenValue(PRIMARY_REFRESH_TOKEN).isPresent());
    }

    @Test
    void logout_WhenAccessTokenMissingButRefreshTokenValid_ReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "rt.2003.refresh-token-1.refresh-secret-1"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value(1001));

        assertTrue(refreshTokenSupport.findByTokenValue(PRIMARY_REFRESH_TOKEN).isPresent());
        assertFalse(accessTokenBlocklistPort.isBlocked(VALID_TOKEN_ID));
    }

    @Test
    void logout_WhenRefreshTokenBelongsToDifferentUser_ReturnForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + VALID_TOKEN)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "rt.2005.refresh-token-other-user.refresh-secret-other-user"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value(2011));

        assertTrue(refreshTokenSupport.findByTokenValue(OTHER_USER_REFRESH_TOKEN).isPresent());
    }

    @Test
    void logout_WhenSameUserRefreshTokenFromAnotherSession_ReturnForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + VALID_TOKEN)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "rt.2003.refresh-token-other-session.refresh-secret-other-session"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value(2011));

        assertTrue(refreshTokenSupport.findByTokenValue(SAME_USER_OTHER_SESSION_REFRESH_TOKEN).isPresent());
    }

    @Test
    void logout_WhenAccessTokenHasNoSessionId_ReturnForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + LEGACY_TOKEN_WITHOUT_SESSION)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "rt.2003.refresh-token-1.refresh-secret-1"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value(2011));

        assertTrue(refreshTokenSupport.findByTokenValue(PRIMARY_REFRESH_TOKEN).isPresent());
        assertFalse(accessTokenBlocklistPort.isBlocked(LEGACY_TOKEN_WITHOUT_SESSION_ID));
    }

    @Test
    void me_WhenPreflightRequestFromAllowedOrigin_ReturnCorsHeaders() throws Exception {
        mockMvc.perform(options("/api/v1/auth/me")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(header().string("Access-Control-Allow-Headers", "Authorization"));
    }

    @Test
    void me_WhenTokenMissing_ReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value(1001))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void me_WhenTokenInvalid_ReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer expired-or-invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value(1001))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void me_WhenTokenValid_ReturnCurrentUserContext() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value("patient_li"))
                .andExpect(jsonPath("$.data.patientId").value(2201))
                .andExpect(jsonPath("$.data.roles[0]").value("PATIENT"))
                .andExpect(jsonPath("$.data.permissions[0]").exists());
    }

    @Test
    void protectedEndpoint_WhenPermissionRevokedAfterLogin_FailsImmediately() throws Exception {
        mockMvc.perform(get("/api/v1/test/patient-self").header("Authorization", "Bearer " + VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").value(2003));

        repository.replacePermissions(authenticatedUser.userId(), Set.of("auth:refresh"));

        mockMvc.perform(get("/api/v1/test/patient-self").header("Authorization", "Bearer " + VALID_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value(1003));
    }

    @Test
    void me_WhenTokenRevoked_ReturnUnauthorized() throws Exception {
        accessTokenBlocklistPort.block(VALID_TOKEN_ID, TOKEN_EXPIRES_AT);

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + VALID_TOKEN))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void me_WhenBlocklistCheckFails_ReturnSystemError() throws Exception {
        accessTokenBlocklistPort.failOnLookup(VALID_TOKEN_ID);

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + VALID_TOKEN))
                .andExpect(status().isInternalServerError())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value(9999));
    }

    @RestController
    @RequestMapping("/api/v1/test")
    private static final class ProtectedTestController {

        @GetMapping("/patient-self")
        public Result<Map<String, Object>> patientSelf(@org.springframework.security.core.annotation.AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
            if (principal == null) {
                throw new BizException(ErrorCode.UNAUTHORIZED);
            }
            if (!principal.permissions().contains("patient:profile:view:self")) {
                throw new BizException(ErrorCode.FORBIDDEN);
            }
            return Result.ok(Map.of("userId", principal.userId(), "username", principal.username()));
        }
    }

    private static final class StubUserAuthenticationRepository implements UserAuthenticationRepository {

        private final Map<Long, AuthenticatedUser> usersById = new ConcurrentHashMap<>();
        private final Map<String, LoginAccount> loginAccountsByUsername = new ConcurrentHashMap<>();
        private final AuthenticatedUser spacedPasswordUser = new AuthenticatedUser(
                2004L,
                "patient_space",
                "空格密码用户",
                UserType.PATIENT,
                new LinkedHashSet<>(java.util.List.of(RoleCode.PATIENT)),
                Set.of("auth:refresh", "patient:profile:view:self"),
                Set.of(),
                2202L,
                null,
                null);

        private StubUserAuthenticationRepository(AuthenticatedUser authenticatedUser, AuthenticatedUser otherUser) {
            registerUser(authenticatedUser, "hash<patient123>");
            registerUser(otherUser, "hash<patient456>");
            registerUser(spacedPasswordUser, "hash<  secret-pass  >");
        }

        private void registerUser(AuthenticatedUser user, String passwordHash) {
            usersById.put(user.userId(), user);
            loginAccountsByUsername.put(user.username(), new LoginAccount(user, passwordHash, AccountStatus.ACTIVE));
        }

        private void replacePermissions(Long userId, Set<String> permissions) {
            AuthenticatedUser currentUser = usersById.get(userId);
            AuthenticatedUser updatedUser = new AuthenticatedUser(
                    currentUser.userId(),
                    currentUser.username(),
                    currentUser.displayName(),
                    currentUser.userType(),
                    currentUser.roles(),
                    permissions,
                    currentUser.dataScopeRules(),
                    currentUser.patientId(),
                    currentUser.doctorId(),
                    currentUser.primaryDepartmentId());
            LoginAccount loginAccount = loginAccountsByUsername.get(currentUser.username());
            usersById.put(userId, updatedUser);
            loginAccountsByUsername.put(currentUser.username(), new LoginAccount(updatedUser, loginAccount.passwordHash(), AccountStatus.ACTIVE));
        }

        @Override
        public Optional<LoginAccount> findLoginAccountByUsername(String username) {
            return Optional.ofNullable(loginAccountsByUsername.get(username));
        }

        @Override
        public Optional<AuthenticatedUser> findAuthenticatedUserById(Long userId) {
            return Optional.ofNullable(usersById.get(userId));
        }

        @Override
        public void updateLastLoginAt(Long userId) {
        }
    }

    private static final class StubAccessTokenCodec implements AccessTokenCodec {

        private final AuthenticatedUser authenticatedUser;
        private int issueSequence = 0;
        private String lastIssuedSessionId;

        private StubAccessTokenCodec(AuthenticatedUser authenticatedUser) {
            this.authenticatedUser = authenticatedUser;
        }

        @Override
        public AccessToken issueAccessToken(AuthenticatedUser authenticatedUser, String sessionId) {
            issueSequence++;
            lastIssuedSessionId = sessionId;
            return new AccessToken(
                    "issued-access-token-" + issueSequence,
                    "issued-token-id-" + issueSequence,
                    TOKEN_EXPIRES_AT);
        }

        private String lastIssuedSessionId() {
            return lastIssuedSessionId;
        }

        @Override
        public AccessTokenClaims parseAccessToken(String accessToken) {
            if (VALID_TOKEN.equals(accessToken)) {
                return new AccessTokenClaims(authenticatedUser.userId(), VALID_TOKEN_ID, "refresh-token-1", TOKEN_EXPIRES_AT);
            }
            if (VALID_TOKEN_OTHER_SESSION.equals(accessToken)) {
                return new AccessTokenClaims(
                        authenticatedUser.userId(),
                        VALID_TOKEN_OTHER_SESSION_ID,
                        "refresh-token-other-session",
                        TOKEN_EXPIRES_AT);
            }
            if (VALID_TOKEN_OTHER_USER.equals(accessToken)) {
                return new AccessTokenClaims(2005L, VALID_TOKEN_OTHER_USER_ID, "refresh-token-other-user", TOKEN_EXPIRES_AT);
            }
            if (LEGACY_TOKEN_WITHOUT_SESSION.equals(accessToken)) {
                return new AccessTokenClaims(
                        authenticatedUser.userId(), LEGACY_TOKEN_WITHOUT_SESSION_ID, null, TOKEN_EXPIRES_AT);
            }
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
    }

    private static final class InMemoryRefreshTokenSupport implements RefreshTokenManager, RefreshTokenStore {

        private final Map<String, RefreshTokenSession> sessions = new ConcurrentHashMap<>();
        private int sequence = 0;

        @Override
        public RefreshTokenSession issue(Long userId) {
            sequence++;
            return new RefreshTokenSession(
                    userId,
                    "refresh-token-" + sequence,
                    "refresh-secret-" + sequence,
                    TOKEN_EXPIRES_AT);
        }

        @Override
        public void save(RefreshTokenSession refreshTokenSession) {
            sessions.put(refreshTokenSession.tokenValue(), refreshTokenSession);
        }

        @Override
        public boolean rotate(String currentRefreshToken, RefreshTokenSession nextRefreshTokenSession) {
            RefreshTokenSession removed = sessions.remove(currentRefreshToken);
            if (removed == null) {
                return false;
            }
            sessions.put(nextRefreshTokenSession.tokenValue(), nextRefreshTokenSession);
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
    }

    private static final class StubAccessTokenBlocklistPort implements AccessTokenBlocklistPort {

        private final Set<String> blockedTokenIds = ConcurrentHashMap.newKeySet();
        private final Set<String> failingTokenIds = ConcurrentHashMap.newKeySet();

        private void failOnLookup(String tokenId) {
            failingTokenIds.add(tokenId);
        }

        @Override
        public void block(String tokenId, Instant expiresAt) {
            blockedTokenIds.add(tokenId);
        }

        @Override
        public boolean isBlocked(String tokenId) {
            if (failingTokenIds.contains(tokenId)) {
                throw new IllegalStateException("redis unavailable");
            }
            return blockedTokenIds.contains(tokenId);
        }
    }
}
