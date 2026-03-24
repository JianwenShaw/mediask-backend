package me.jianwen.mediask.api.controller;

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
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import me.jianwen.mediask.api.advice.ResultResponseBodyAdvice;
import me.jianwen.mediask.api.exception.GlobalExceptionHandler;
import me.jianwen.mediask.api.filter.RequestCorrelationFilter;
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

class AuthControllerTest {

    private static final String VALID_TOKEN = "valid-token";
    private static final String VALID_TOKEN_ID = "valid-token-id";

    private MockMvc mockMvc;
    private InMemoryRefreshTokenSupport refreshTokenSupport;
    private StubAccessTokenBlocklistPort accessTokenBlocklistPort;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final AuthenticatedUser authenticatedUser = new AuthenticatedUser(
            2003L,
            "patient_li",
            "李患者",
            UserType.PATIENT,
            new LinkedHashSet<>(java.util.List.of(RoleCode.PATIENT)),
            Set.of("auth:refresh", "patient:profile:view:self"),
            2201L,
            null,
            null);

    @BeforeEach
    void setUp() {
        refreshTokenSupport = new InMemoryRefreshTokenSupport();
        accessTokenBlocklistPort = new StubAccessTokenBlocklistPort();
        StubUserAuthenticationRepository repository = new StubUserAuthenticationRepository(authenticatedUser);
        StubAccessTokenCodec accessTokenCodec = new StubAccessTokenCodec(authenticatedUser);
        PasswordVerifier passwordVerifier = (rawPassword, encodedPassword) ->
                encodedPassword.equals("hash<" + rawPassword + ">");

        RefreshTokenSession initialRefreshToken = refreshTokenSupport.issue(authenticatedUser.userId());
        refreshTokenSupport.save(initialRefreshToken);

        AuthController authController = new AuthController(
                new LoginUseCase(repository, passwordVerifier, accessTokenCodec, refreshTokenSupport, refreshTokenSupport),
                new RefreshTokenUseCase(repository, accessTokenCodec, refreshTokenSupport, refreshTokenSupport, java.time.Clock.systemUTC()),
                new LogoutUseCase(
                        refreshTokenSupport,
                        accessTokenBlocklistPort,
                        accessTokenCodec,
                        java.time.Clock.systemUTC()),
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

        mockMvc = MockMvcBuilders.standaloneSetup(authController)
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
                .andExpect(jsonPath("$.data.userContext.permissions[0]").exists());
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
                .andExpect(jsonPath("$.data.userContext.roles[0]").value("PATIENT"));
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
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value(0));
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

    private static final class StubUserAuthenticationRepository implements UserAuthenticationRepository {

        private final AuthenticatedUser authenticatedUser;
        private final AuthenticatedUser spacedPasswordUser = new AuthenticatedUser(
                2004L,
                "patient_space",
                "空格密码用户",
                UserType.PATIENT,
                new LinkedHashSet<>(java.util.List.of(RoleCode.PATIENT)),
                Set.of("auth:refresh", "patient:profile:view:self"),
                2202L,
                null,
                null);

        private StubUserAuthenticationRepository(AuthenticatedUser authenticatedUser) {
            this.authenticatedUser = authenticatedUser;
        }

        @Override
        public Optional<LoginAccount> findLoginAccountByUsername(String username) {
            if (authenticatedUser.username().equals(username)) {
                return Optional.of(new LoginAccount(authenticatedUser, "hash<patient123>", AccountStatus.ACTIVE));
            }
            if (spacedPasswordUser.username().equals(username)) {
                return Optional.of(new LoginAccount(spacedPasswordUser, "hash<  secret-pass  >", AccountStatus.ACTIVE));
            }
            return Optional.empty();
        }

        @Override
        public Optional<AuthenticatedUser> findAuthenticatedUserById(Long userId) {
            if (authenticatedUser.userId().equals(userId)) {
                return Optional.of(authenticatedUser);
            }
            if (spacedPasswordUser.userId().equals(userId)) {
                return Optional.of(spacedPasswordUser);
            }
            return Optional.empty();
        }

        @Override
        public void updateLastLoginAt(Long userId) {
        }
    }

    private static final class StubAccessTokenCodec implements AccessTokenCodec {

        private final AuthenticatedUser authenticatedUser;
        private int issueSequence = 0;

        private StubAccessTokenCodec(AuthenticatedUser authenticatedUser) {
            this.authenticatedUser = authenticatedUser;
        }

        @Override
        public AccessToken issueAccessToken(AuthenticatedUser authenticatedUser) {
            issueSequence++;
            return new AccessToken(
                    "issued-access-token-" + issueSequence,
                    "issued-token-id-" + issueSequence,
                    Instant.parse("2026-03-30T08:00:00Z"));
        }

        @Override
        public AccessTokenClaims parseAccessToken(String accessToken) {
            if (VALID_TOKEN.equals(accessToken)) {
                return new AccessTokenClaims(authenticatedUser.userId(), VALID_TOKEN_ID, Instant.parse("2026-03-30T08:00:00Z"));
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
                    Instant.parse("2026-03-30T08:00:00Z"));
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

        @Override
        public void block(String tokenId, Instant expiresAt) {
            blockedTokenIds.add(tokenId);
        }

        @Override
        public boolean isBlocked(String tokenId) {
            return blockedTokenIds.contains(tokenId);
        }
    }
}
