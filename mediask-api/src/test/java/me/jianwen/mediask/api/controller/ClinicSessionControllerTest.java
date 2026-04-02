package me.jianwen.mediask.api.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import me.jianwen.mediask.api.advice.ResultResponseBodyAdvice;
import me.jianwen.mediask.api.exception.GlobalExceptionHandler;
import me.jianwen.mediask.api.filter.RequestCorrelationFilter;
import me.jianwen.mediask.api.security.ApiCorsProperties;
import me.jianwen.mediask.api.security.ApiSecurityProperties;
import me.jianwen.mediask.api.security.JsonAuthenticationEntryPoint;
import me.jianwen.mediask.api.security.JwtAuthenticationFilter;
import me.jianwen.mediask.api.security.SecurityConfig;
import me.jianwen.mediask.application.outpatient.usecase.ListClinicSessionsUseCase;
import me.jianwen.mediask.domain.outpatient.model.ClinicSessionListItem;
import me.jianwen.mediask.domain.outpatient.model.ClinicSessionPeriodCode;
import me.jianwen.mediask.domain.outpatient.model.ClinicType;
import me.jianwen.mediask.domain.outpatient.port.ClinicSessionQueryRepository;
import me.jianwen.mediask.domain.user.model.AccessToken;
import me.jianwen.mediask.domain.user.model.AccessTokenClaims;
import me.jianwen.mediask.domain.user.model.AuthenticatedUser;
import me.jianwen.mediask.domain.user.model.RoleCode;
import me.jianwen.mediask.domain.user.model.UserType;
import me.jianwen.mediask.domain.user.port.AccessTokenCodec;
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

class ClinicSessionControllerTest {

    private static final String VALID_TOKEN = "valid-token";

    private MockMvc mockMvc;
    private StubClinicSessionQueryRepository repository;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final AuthenticatedUser authenticatedUser = new AuthenticatedUser(
            2003L,
            "patient_li",
            "李患者",
            UserType.PATIENT,
            new LinkedHashSet<>(List.of(RoleCode.PATIENT)),
            Set.of("registration:create"),
            Set.of(),
            2201L,
            null,
            null);

    @BeforeEach
    void setUp() {
        repository = new StubClinicSessionQueryRepository();
        ClinicSessionController controller =
                new ClinicSessionController(new ListClinicSessionsUseCase(repository));

        JsonAuthenticationEntryPoint authenticationEntryPoint = new JsonAuthenticationEntryPoint(objectMapper);
        SecurityConfig securityConfig = new SecurityConfig();
        RequestMatcher publicRequestMatcher = securityConfig.publicRequestMatcher(new ApiSecurityProperties(false));
        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(
                new StubAccessTokenCodec(authenticatedUser),
                new StubAccessTokenBlocklistPort(),
                new StubUserAuthenticationRepository(authenticatedUser),
                authenticationEntryPoint,
                objectMapper,
                publicRequestMatcher);
        CorsFilter corsFilter = new CorsFilter(securityConfig.corsConfigurationSource(new ApiCorsProperties(
                List.of("http://localhost:3000", "http://localhost:5173"),
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

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(), new ResultResponseBodyAdvice())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setMessageConverters(
                        new StringHttpMessageConverter(),
                        new MappingJackson2HttpMessageConverter(objectMapper))
                .addFilters(new RequestCorrelationFilter(), corsFilter, jwtAuthenticationFilter, securityContextCleanupFilter)
                .build();
    }

    @Test
    void list_WhenAuthenticated_ReturnOpenClinicSessions() throws Exception {
        mockMvc.perform(get("/api/v1/clinic-sessions")
                        .header("Authorization", "Bearer " + VALID_TOKEN)
                        .param("departmentId", "3101")
                        .param("dateFrom", "2026-04-01")
                        .param("dateTo", "2026-04-07")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.requestId").isNotEmpty())
                .andExpect(jsonPath("$.data.items[0].clinicSessionId").value(4101))
                .andExpect(jsonPath("$.data.items[0].departmentName").value("呼吸内科"))
                .andExpect(jsonPath("$.data.items[0].doctorName").value("张医生"))
                .andExpect(jsonPath("$.data.items[0].periodCode").value("MORNING"))
                .andExpect(jsonPath("$.data.items[0].clinicType").value("GENERAL"))
                .andExpect(jsonPath("$.data.items[0].remainingCount").value(12))
                .andExpect(jsonPath("$.data.items[0].fee").value(18.00));
    }

    private static final class StubClinicSessionQueryRepository implements ClinicSessionQueryRepository {

        @Override
        public List<ClinicSessionListItem> listOpenSessions(Long departmentId, LocalDate dateFrom, LocalDate dateTo) {
            return List.of(new ClinicSessionListItem(
                    4101L,
                    departmentId == null ? 3101L : departmentId,
                    "呼吸内科",
                    2101L,
                    "张医生",
                    LocalDate.of(2026, 4, 1),
                    ClinicSessionPeriodCode.MORNING,
                    ClinicType.GENERAL,
                    12,
                    new BigDecimal("18.00")));
        }
    }

    private static final class StubAccessTokenCodec implements AccessTokenCodec {

        private final AuthenticatedUser authenticatedUser;

        private StubAccessTokenCodec(AuthenticatedUser authenticatedUser) {
            this.authenticatedUser = authenticatedUser;
        }

        @Override
        public AccessToken issueAccessToken(AuthenticatedUser user, String refreshTokenSessionId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AccessTokenClaims parseAccessToken(String tokenValue) {
            if (!VALID_TOKEN.equals(tokenValue)) {
                throw new IllegalArgumentException("invalid token");
            }
            return new AccessTokenClaims(
                    authenticatedUser.userId(),
                    "token-id",
                    "session-id",
                    java.time.Instant.parse("2026-04-02T12:00:00Z"));
        }
    }

    private static final class StubAccessTokenBlocklistPort implements me.jianwen.mediask.domain.user.port.AccessTokenBlocklistPort {

        @Override
        public void block(String tokenId, java.time.Instant expiresAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isBlocked(String tokenId) {
            return false;
        }
    }

    private static final class StubUserAuthenticationRepository implements UserAuthenticationRepository {

        private final AuthenticatedUser authenticatedUser;

        private StubUserAuthenticationRepository(AuthenticatedUser authenticatedUser) {
            this.authenticatedUser = authenticatedUser;
        }

        @Override
        public Optional<me.jianwen.mediask.domain.user.model.LoginAccount> findLoginAccountByUsername(String username) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<AuthenticatedUser> findAuthenticatedUserById(Long userId) {
            return authenticatedUser.userId().equals(userId) ? Optional.of(authenticatedUser) : Optional.empty();
        }

        @Override
        public void updateLastLoginAt(Long userId) {
            throw new UnsupportedOperationException();
        }
    }
}
