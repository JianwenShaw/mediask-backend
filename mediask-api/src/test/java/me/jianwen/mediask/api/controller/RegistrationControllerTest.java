package me.jianwen.mediask.api.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import java.time.Instant;
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
import me.jianwen.mediask.application.outpatient.usecase.CreateRegistrationResult;
import me.jianwen.mediask.application.outpatient.usecase.CreateRegistrationUseCase;
import me.jianwen.mediask.domain.outpatient.model.RegistrationStatus;
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

class RegistrationControllerTest {

    private static final String PATIENT_TOKEN = "patient-token";
    private static final String DOCTOR_TOKEN = "doctor-token";

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private MockMvc patientMockMvc;
    private MockMvc doctorMockMvc;

    @BeforeEach
    void setUp() {
        patientMockMvc = buildMockMvc(new AuthenticatedUser(
                2003L,
                "patient_li",
                "李患者",
                UserType.PATIENT,
                new LinkedHashSet<>(List.of(RoleCode.PATIENT)),
                Set.of("registration:create"),
                Set.of(),
                2201L,
                null,
                null));
        doctorMockMvc = buildMockMvc(new AuthenticatedUser(
                2004L,
                "doctor_zhang",
                "张医生",
                UserType.DOCTOR,
                new LinkedHashSet<>(List.of(RoleCode.DOCTOR)),
                Set.of("registration:create"),
                Set.of(),
                null,
                2101L,
                3101L));
    }

    @Test
    void create_WhenAuthenticatedPatient_ReturnRegistration() throws Exception {
        patientMockMvc.perform(post("/api/v1/registrations")
                        .header("Authorization", "Bearer " + PATIENT_TOKEN)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "clinicSessionId": 4101,
                                  "clinicSlotId": 5101,
                                  "sourceAiSessionId": 7101
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.registrationId").value(6101))
                .andExpect(jsonPath("$.data.orderNo").value("REG6101"))
                .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"));
    }

    @Test
    void create_WhenUnauthenticated_ReturnUnauthorized() throws Exception {
        patientMockMvc.perform(post("/api/v1/registrations")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "clinicSessionId": 4101,
                                  "clinicSlotId": 5101
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void create_WhenAuthenticatedDoctor_ReturnRoleMismatch() throws Exception {
        doctorMockMvc.perform(post("/api/v1/registrations")
                        .header("Authorization", "Bearer " + DOCTOR_TOKEN)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "clinicSessionId": 4101,
                                  "clinicSlotId": 5101
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(2008));
    }

    private MockMvc buildMockMvc(AuthenticatedUser authenticatedUser) {
        RegistrationController controller = new RegistrationController(new StubCreateRegistrationUseCase());

        JsonAuthenticationEntryPoint authenticationEntryPoint = new JsonAuthenticationEntryPoint(objectMapper);
        SecurityConfig securityConfig = new SecurityConfig();
        RequestMatcher publicRequestMatcher = securityConfig.publicRequestMatcher(new ApiSecurityProperties(false));
        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(
                new StubAccessTokenCodec(),
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

        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(), new ResultResponseBodyAdvice())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setMessageConverters(
                        new StringHttpMessageConverter(),
                        new MappingJackson2HttpMessageConverter(objectMapper))
                .addFilters(new RequestCorrelationFilter(), corsFilter, jwtAuthenticationFilter, securityContextCleanupFilter)
                .build();
    }

    private static final class StubCreateRegistrationUseCase extends CreateRegistrationUseCase {

        private StubCreateRegistrationUseCase() {
            super(null, null);
        }

        @Override
        public CreateRegistrationResult handle(me.jianwen.mediask.application.outpatient.command.CreateRegistrationCommand command) {
            return new CreateRegistrationResult(6101L, "REG6101", RegistrationStatus.PENDING_PAYMENT);
        }
    }

    private static final class StubAccessTokenCodec implements AccessTokenCodec {

        @Override
        public AccessToken issueAccessToken(AuthenticatedUser user, String refreshTokenSessionId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AccessTokenClaims parseAccessToken(String tokenValue) {
            if (!PATIENT_TOKEN.equals(tokenValue) && !DOCTOR_TOKEN.equals(tokenValue)) {
                throw new IllegalArgumentException("invalid token");
            }
            return new AccessTokenClaims(2000L, "token-id", "session-id", Instant.parse("2026-04-02T12:00:00Z"));
        }
    }

    private static final class StubAccessTokenBlocklistPort implements me.jianwen.mediask.domain.user.port.AccessTokenBlocklistPort {

        @Override
        public void block(String tokenId, Instant expiresAt) {
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
            return Optional.of(authenticatedUser);
        }

        @Override
        public void updateLastLoginAt(Long userId) {
            throw new UnsupportedOperationException();
        }
    }
}
