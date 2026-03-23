package me.jianwen.mediask.api.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import java.util.LinkedHashSet;
import java.util.Optional;
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
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.exception.ErrorCode;
import me.jianwen.mediask.domain.user.model.AccountStatus;
import me.jianwen.mediask.domain.user.model.AuthenticatedUser;
import me.jianwen.mediask.domain.user.model.LoginAccount;
import me.jianwen.mediask.domain.user.model.RoleCode;
import me.jianwen.mediask.domain.user.model.UserType;
import me.jianwen.mediask.domain.user.port.AccessTokenCodec;
import me.jianwen.mediask.domain.user.port.PasswordVerifier;
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

    private static final String LOGIN_TOKEN = "access-token";
    private static final String VALID_TOKEN = "valid-token";

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final AuthenticatedUser authenticatedUser = new AuthenticatedUser(
            2003L,
            "patient_li",
            "李患者",
            UserType.PATIENT,
            new LinkedHashSet<>(java.util.List.of(RoleCode.PATIENT)),
            2201L,
            null,
            null);

    @BeforeEach
    void setUp() {
        StubUserAuthenticationRepository repository = new StubUserAuthenticationRepository(authenticatedUser);
        StubAccessTokenCodec accessTokenCodec = new StubAccessTokenCodec(authenticatedUser);
        PasswordVerifier passwordVerifier = (rawPassword, encodedPassword) ->
                encodedPassword.equals("hash<" + rawPassword + ">");

        AuthController authController = new AuthController(
                new LoginUseCase(repository, passwordVerifier, accessTokenCodec),
                new GetCurrentUserUseCase(repository));

        JsonAuthenticationEntryPoint authenticationEntryPoint = new JsonAuthenticationEntryPoint(objectMapper);
        SecurityConfig securityConfig = new SecurityConfig();
        RequestMatcher publicRequestMatcher = securityConfig.publicRequestMatcher(new ApiSecurityProperties(false));
        JwtAuthenticationFilter jwtAuthenticationFilter =
                new JwtAuthenticationFilter(accessTokenCodec, authenticationEntryPoint, publicRequestMatcher);
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
    void login_WhenCredentialsValid_ReturnAccessTokenAndCurrentUser() throws Exception {
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
                .andExpect(jsonPath("$.data.accessToken").value(LOGIN_TOKEN))
                .andExpect(jsonPath("$.data.userId").value(2003))
                .andExpect(jsonPath("$.data.roles[0]").value("PATIENT"));
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
                .andExpect(jsonPath("$.data.userId").value(2004))
                .andExpect(jsonPath("$.data.username").value("patient_space"))
                .andExpect(jsonPath("$.data.roles[0]").value("PATIENT"));
    }

    @Test
    void login_WhenInvalidBearerTokenPresent_StillReturnsAccessTokenAndCurrentUser() throws Exception {
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
                .andExpect(jsonPath("$.data.accessToken").value(LOGIN_TOKEN));
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
    void me_WhenTokenValid_ReturnCurrentUser() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value("patient_li"))
                .andExpect(jsonPath("$.data.patientId").value(2201))
                .andExpect(jsonPath("$.data.roles[0]").value("PATIENT"));
    }

    private static final class StubUserAuthenticationRepository implements UserAuthenticationRepository {

        private final AuthenticatedUser authenticatedUser;
        private final AuthenticatedUser spacedPasswordUser = new AuthenticatedUser(
                2004L,
                "patient_space",
                "空格密码用户",
                UserType.PATIENT,
                new LinkedHashSet<>(java.util.List.of(RoleCode.PATIENT)),
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

        private StubAccessTokenCodec(AuthenticatedUser authenticatedUser) {
            this.authenticatedUser = authenticatedUser;
        }

        @Override
        public String issueAccessToken(AuthenticatedUser authenticatedUser) {
            return LOGIN_TOKEN;
        }

        @Override
        public AuthenticatedUser parseAccessToken(String accessToken) {
            if (VALID_TOKEN.equals(accessToken)) {
                return authenticatedUser;
            }
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
    }
}
