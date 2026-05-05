package me.jianwen.mediask.api.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import me.jianwen.mediask.api.TestAuditSupport;
import me.jianwen.mediask.api.advice.ResultResponseBodyAdvice;
import me.jianwen.mediask.api.exception.GlobalExceptionHandler;
import me.jianwen.mediask.api.filter.RequestCorrelationFilter;
import me.jianwen.mediask.api.security.ApiCorsProperties;
import me.jianwen.mediask.api.security.ApiSecurityProperties;
import me.jianwen.mediask.api.security.JsonAuthenticationEntryPoint;
import me.jianwen.mediask.api.security.JwtAuthenticationFilter;
import me.jianwen.mediask.api.security.SecurityConfig;
import me.jianwen.mediask.application.audit.model.AuditContext;
import me.jianwen.mediask.application.outpatient.usecase.CreateRegistrationResult;
import me.jianwen.mediask.application.outpatient.usecase.CreateRegistrationUseCase;
import me.jianwen.mediask.application.outpatient.usecase.CancelRegistrationResult;
import me.jianwen.mediask.application.outpatient.usecase.CancelRegistrationUseCase;
import me.jianwen.mediask.application.outpatient.usecase.GetRegistrationDetailUseCase;
import me.jianwen.mediask.application.outpatient.usecase.ListRegistrationsUseCase;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.outpatient.exception.OutpatientErrorCode;
import me.jianwen.mediask.domain.outpatient.model.ClinicSessionPeriodCode;
import me.jianwen.mediask.domain.outpatient.model.RegistrationDetail;
import me.jianwen.mediask.domain.outpatient.model.RegistrationListItem;
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

    private final ObjectMapper objectMapper = buildObjectMapper();

    private MockMvc patientMockMvc;
    private MockMvc doctorMockMvc;
    private StubCreateRegistrationUseCase patientCreateRegistrationUseCase;
    private StubListRegistrationsUseCase patientListRegistrationsUseCase;
    private StubGetRegistrationDetailUseCase patientGetRegistrationDetailUseCase;
    private StubCancelRegistrationUseCase patientCancelRegistrationUseCase;

    @BeforeEach
    void setUp() {
        patientCreateRegistrationUseCase = new StubCreateRegistrationUseCase();
        patientListRegistrationsUseCase = new StubListRegistrationsUseCase();
        patientGetRegistrationDetailUseCase = new StubGetRegistrationDetailUseCase();
        patientCancelRegistrationUseCase = new StubCancelRegistrationUseCase();
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
                null),
                patientCreateRegistrationUseCase,
                patientListRegistrationsUseCase,
                patientGetRegistrationDetailUseCase,
                patientCancelRegistrationUseCase);
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
                3101L),
                new StubCreateRegistrationUseCase(),
                new StubListRegistrationsUseCase(),
                new StubGetRegistrationDetailUseCase(),
                new StubCancelRegistrationUseCase());
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
                                  "sourceAiSessionId": "session-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.registrationId").value("6101"))
                .andExpect(jsonPath("$.data.orderNo").value("REG6101"))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

        assertEquals(2003L, patientCreateRegistrationUseCase.lastCommand.patientUserId());
        assertEquals(4101L, patientCreateRegistrationUseCase.lastCommand.clinicSessionId());
        assertEquals(5101L, patientCreateRegistrationUseCase.lastCommand.clinicSlotId());
        assertEquals("session-1", patientCreateRegistrationUseCase.lastCommand.sourceAiSessionId());
    }

    @Test
    void list_WhenAuthenticatedPatient_ReturnOwnRegistrations() throws Exception {
        patientMockMvc.perform(get("/api/v1/registrations")
                        .header("Authorization", "Bearer " + PATIENT_TOKEN)
                        .param("status", "CONFIRMED"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.items[0].registrationId").value("6101"))
                .andExpect(jsonPath("$.data.items[0].orderNo").value("REG6101"))
                .andExpect(jsonPath("$.data.items[0].status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.items[0].createdAt").value("2026-04-02T10:00:00+08:00"))
                .andExpect(jsonPath("$.data.items[0].sourceAiSessionId").value("session-1"));

        assertEquals(2003L, patientListRegistrationsUseCase.lastQuery.patientUserId());
        assertEquals(RegistrationStatus.CONFIRMED, patientListRegistrationsUseCase.lastQuery.status());
    }

    @Test
    void detail_WhenAuthenticatedPatient_ReturnRegistrationDetail() throws Exception {
        patientMockMvc.perform(get("/api/v1/registrations/6101")
                        .header("Authorization", "Bearer " + PATIENT_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.registrationId").value("6101"))
                .andExpect(jsonPath("$.data.orderNo").value("REG6101"))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.createdAt").value("2026-04-02T10:00:00+08:00"))
                .andExpect(jsonPath("$.data.sourceAiSessionId").value("session-1"))
                .andExpect(jsonPath("$.data.clinicSessionId").value("4101"))
                .andExpect(jsonPath("$.data.clinicSlotId").value("5101"))
                .andExpect(jsonPath("$.data.departmentId").value("3101"))
                .andExpect(jsonPath("$.data.departmentName").value("神经内科"))
                .andExpect(jsonPath("$.data.doctorId").value("2101"))
                .andExpect(jsonPath("$.data.doctorName").value("张医生"))
                .andExpect(jsonPath("$.data.sessionDate").value("2026-04-03"))
                .andExpect(jsonPath("$.data.periodCode").value("MORNING"))
                .andExpect(jsonPath("$.data.fee").value(18.00));

        assertEquals(6101L, patientGetRegistrationDetailUseCase.lastQuery.registrationId());
        assertEquals(2003L, patientGetRegistrationDetailUseCase.lastQuery.patientUserId());
    }

    @Test
    void cancel_WhenAuthenticatedPatient_ReturnCancelledRegistration() throws Exception {
        patientMockMvc.perform(patch("/api/v1/registrations/6101/cancel")
                        .header("Authorization", "Bearer " + PATIENT_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.registrationId").value("6101"))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.cancelledAt").value("2026-04-03T11:00:00+08:00"));

        assertEquals(6101L, patientCancelRegistrationUseCase.lastCommand.registrationId());
        assertEquals(2003L, patientCancelRegistrationUseCase.lastCommand.patientUserId());
    }

    @Test
    void create_WhenUnauthenticated_ReturnUnauthorized() throws Exception {
        patientMockMvc.perform(post("/api/v1/registrations")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "clinicSessionId": 4101,
                                  "clinicSlotId": 5101,
                                  "sourceAiSessionId": "session-1"
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
                                  "clinicSlotId": 5101,
                                  "sourceAiSessionId": "session-1"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(2008));
    }

    @Test
    void list_WhenUnauthenticated_ReturnUnauthorized() throws Exception {
        patientMockMvc.perform(get("/api/v1/registrations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void list_WhenAuthenticatedDoctor_ReturnRoleMismatch() throws Exception {
        doctorMockMvc.perform(get("/api/v1/registrations")
                        .header("Authorization", "Bearer " + DOCTOR_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(2008));
    }

    @Test
    void detail_WhenRegistrationMissing_ReturnNotFound() throws Exception {
        patientGetRegistrationDetailUseCase.throwable = new BizException(OutpatientErrorCode.REGISTRATION_NOT_FOUND);

        patientMockMvc.perform(get("/api/v1/registrations/9999")
                        .header("Authorization", "Bearer " + PATIENT_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(3008));
    }

    @Test
    void cancel_WhenCancellationRejected_ReturnConflict() throws Exception {
        patientCancelRegistrationUseCase.throwable = new BizException(OutpatientErrorCode.REGISTRATION_CANCEL_NOT_ALLOWED);

        patientMockMvc.perform(patch("/api/v1/registrations/6101/cancel")
                        .header("Authorization", "Bearer " + PATIENT_TOKEN))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(3009));
    }

    private MockMvc buildMockMvc(
            AuthenticatedUser authenticatedUser,
            StubCreateRegistrationUseCase createRegistrationUseCase,
            StubListRegistrationsUseCase listRegistrationsUseCase,
            StubGetRegistrationDetailUseCase getRegistrationDetailUseCase,
            StubCancelRegistrationUseCase cancelRegistrationUseCase) {
        RegistrationController controller = new RegistrationController(
                createRegistrationUseCase,
                listRegistrationsUseCase,
                getRegistrationDetailUseCase,
                cancelRegistrationUseCase,
                TestAuditSupport.auditApiSupport());

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

        private me.jianwen.mediask.application.outpatient.command.CreateRegistrationCommand lastCommand;
        private RuntimeException throwable;

        private StubCreateRegistrationUseCase() {
            super(null, null, null, TestAuditSupport.auditTrailService());
        }

        @Override
        public CreateRegistrationResult handle(
                me.jianwen.mediask.application.outpatient.command.CreateRegistrationCommand command,
                AuditContext auditContext) {
            this.lastCommand = command;
            if (throwable != null) {
                throw throwable;
            }
            return new CreateRegistrationResult(6101L, "REG6101", RegistrationStatus.CONFIRMED);
        }
    }

    private static final class StubListRegistrationsUseCase extends ListRegistrationsUseCase {

        private me.jianwen.mediask.application.outpatient.query.ListRegistrationsQuery lastQuery;

        private StubListRegistrationsUseCase() {
            super(null);
        }

        @Override
        public List<RegistrationListItem> handle(me.jianwen.mediask.application.outpatient.query.ListRegistrationsQuery query) {
            this.lastQuery = query;
            return List.of(
                    new RegistrationListItem(
                            6101L,
                            "REG6101",
                            RegistrationStatus.CONFIRMED,
                            OffsetDateTime.parse("2026-04-02T10:00:00+08:00"),
                            "session-1"));
        }
    }

    private static final class StubGetRegistrationDetailUseCase extends GetRegistrationDetailUseCase {

        private me.jianwen.mediask.application.outpatient.query.GetRegistrationDetailQuery lastQuery;
        private RuntimeException throwable;

        private StubGetRegistrationDetailUseCase() {
            super(null);
        }

        @Override
        public RegistrationDetail handle(me.jianwen.mediask.application.outpatient.query.GetRegistrationDetailQuery query) {
            this.lastQuery = query;
            if (throwable != null) {
                throw throwable;
            }
            return new RegistrationDetail(
                    6101L,
                    2003L,
                    "REG6101",
                    RegistrationStatus.CONFIRMED,
                    OffsetDateTime.parse("2026-04-02T10:00:00+08:00"),
                    "session-1",
                    4101L,
                    5101L,
                    3101L,
                    "神经内科",
                    2101L,
                    "张医生",
                    java.time.LocalDate.parse("2026-04-03"),
                    ClinicSessionPeriodCode.MORNING,
                    new java.math.BigDecimal("18.00"),
                    null,
                    null);
        }
    }

    private static final class StubCancelRegistrationUseCase extends CancelRegistrationUseCase {

        private me.jianwen.mediask.application.outpatient.command.CancelRegistrationCommand lastCommand;
        private RuntimeException throwable;

        private StubCancelRegistrationUseCase() {
            super(null, null, null, TestAuditSupport.auditTrailService());
        }

        @Override
        public CancelRegistrationResult handle(
                me.jianwen.mediask.application.outpatient.command.CancelRegistrationCommand command,
                AuditContext auditContext) {
            this.lastCommand = command;
            if (throwable != null) {
                throw throwable;
            }
            return new CancelRegistrationResult(
                    6101L,
                    RegistrationStatus.CANCELLED,
                    OffsetDateTime.parse("2026-04-03T11:00:00+08:00"));
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

    private ObjectMapper buildObjectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }
}
