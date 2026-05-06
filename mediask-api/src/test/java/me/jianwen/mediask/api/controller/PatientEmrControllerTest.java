package me.jianwen.mediask.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import java.time.Instant;
import java.time.LocalDate;
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
import me.jianwen.mediask.api.security.ScenarioAuthorizationAspect;
import me.jianwen.mediask.api.security.SecurityConfig;
import me.jianwen.mediask.application.authz.AuthorizationDecisionService;
import me.jianwen.mediask.application.clinical.query.ListCurrentPatientEmrsQuery;
import me.jianwen.mediask.application.clinical.usecase.ListCurrentPatientEmrsUseCase;
import me.jianwen.mediask.domain.clinical.model.EmrRecordListItem;
import me.jianwen.mediask.domain.clinical.model.EmrRecordStatus;
import me.jianwen.mediask.domain.user.model.AccessToken;
import me.jianwen.mediask.domain.user.model.AccessTokenClaims;
import me.jianwen.mediask.domain.user.model.AuthenticatedUser;
import me.jianwen.mediask.domain.user.model.RoleCode;
import me.jianwen.mediask.domain.user.model.UserType;
import me.jianwen.mediask.domain.user.port.AccessTokenCodec;
import me.jianwen.mediask.domain.user.port.UserAuthenticationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.filter.CorsFilter;

class PatientEmrControllerTest {

    private static final String PATIENT_TOKEN = "patient-token";
    private static final String DOCTOR_TOKEN = "doctor-token";
    private static final String NO_PERMISSION_PATIENT_TOKEN = "no-permission-patient-token";

    private MockMvc patientMockMvc;
    private MockMvc doctorMockMvc;
    private MockMvc noPermissionPatientMockMvc;
    private StubListCurrentPatientEmrsUseCase listCurrentPatientEmrsUseCase;

    @BeforeEach
    void setUp() {
        listCurrentPatientEmrsUseCase = new StubListCurrentPatientEmrsUseCase();
        patientMockMvc = buildMockMvc(new AuthenticatedUser(
                2003L,
                "patient_li",
                "李患者",
                UserType.PATIENT,
                new LinkedHashSet<>(List.of(RoleCode.PATIENT)),
                Set.of("emr:read"),
                Set.of(),
                2201L,
                null,
                null), listCurrentPatientEmrsUseCase);
        doctorMockMvc = buildMockMvc(new AuthenticatedUser(
                2004L,
                "doctor_zhang",
                "张医生",
                UserType.DOCTOR,
                new LinkedHashSet<>(List.of(RoleCode.DOCTOR)),
                Set.of("emr:read"),
                Set.of(),
                null,
                2101L,
                3101L), new StubListCurrentPatientEmrsUseCase());
        noPermissionPatientMockMvc = buildMockMvc(new AuthenticatedUser(
                2005L,
                "patient_wu",
                "吴患者",
                UserType.PATIENT,
                new LinkedHashSet<>(List.of(RoleCode.PATIENT)),
                Set.of(),
                Set.of(),
                2202L,
                null,
                null), new StubListCurrentPatientEmrsUseCase());
    }

    @Test
    void list_WhenAuthenticatedPatient_ReturnOwnEmrHistory() throws Exception {
        patientMockMvc.perform(get("/api/v1/patients/me/emrs")
                        .header("Authorization", "Bearer " + PATIENT_TOKEN)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.items[0].emrRecordId").value("7101"))
                .andExpect(jsonPath("$.data.items[0].encounterId").value("8104"))
                .andExpect(jsonPath("$.data.items[0].recordNo").value("EMR20260504001"))
                .andExpect(jsonPath("$.data.items[0].recordStatus").value("DRAFT"))
                .andExpect(jsonPath("$.data.items[0].departmentId").value("3102"))
                .andExpect(jsonPath("$.data.items[0].doctorId").value("3203"))
                .andExpect(jsonPath("$.data.items[0].doctorName").value("李医生"))
                .andExpect(jsonPath("$.data.items[0].sessionDate").value("2026-05-04"))
                .andExpect(jsonPath("$.data.items[0].createdAt").value("2026-05-04T15:10:00+08:00"));

        assertEquals(2003L, listCurrentPatientEmrsUseCase.lastQuery.patientUserId());
    }

    @Test
    void list_WhenDoctorAccessesPatientEndpoint_ReturnForbidden() throws Exception {
        doctorMockMvc.perform(get("/api/v1/patients/me/emrs")
                        .header("Authorization", "Bearer " + DOCTOR_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(2008));
    }

    @Test
    void list_WhenPermissionMissing_ReturnForbidden() throws Exception {
        noPermissionPatientMockMvc.perform(get("/api/v1/patients/me/emrs")
                        .header("Authorization", "Bearer " + NO_PERMISSION_PATIENT_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1003));
    }

    private MockMvc buildMockMvc(AuthenticatedUser authenticatedUser, StubListCurrentPatientEmrsUseCase useCase) {
        PatientEmrController target = new PatientEmrController(useCase, TestAuditSupport.auditApiSupport());
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(target);
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAspect(new ScenarioAuthorizationAspect(
                new AuthorizationDecisionService(List.of(), List.of()),
                TestAuditSupport.auditApiSupport(),
                TestAuditSupport.emptyEncounterQueryRepository(),
                TestAuditSupport.emptyAdminPatientQueryRepository()));
        PatientEmrController controller = proxyFactory.getProxy();

        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
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

    private static final class StubListCurrentPatientEmrsUseCase extends ListCurrentPatientEmrsUseCase {

        private ListCurrentPatientEmrsQuery lastQuery;

        private StubListCurrentPatientEmrsUseCase() {
            super(null, TestAuditSupport.auditTrailService());
        }

        @Override
        public List<EmrRecordListItem> handle(
                ListCurrentPatientEmrsQuery query,
                me.jianwen.mediask.application.audit.model.AuditContext auditContext) {
            this.lastQuery = query;
            return List.of(new EmrRecordListItem(
                    7101L,
                    "EMR20260504001",
                    8104L,
                    EmrRecordStatus.DRAFT,
                    3102L,
                    "全科门诊",
                    3203L,
                    "李医生",
                    LocalDate.parse("2026-05-04"),
                    "发热缓解后复诊评估",
                    OffsetDateTime.parse("2026-05-04T15:10:00+08:00")));
        }
    }

    private static final class StubAccessTokenCodec implements AccessTokenCodec {
        @Override
        public AccessToken issueAccessToken(AuthenticatedUser user, String refreshTokenSessionId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AccessTokenClaims parseAccessToken(String token) {
            if (!PATIENT_TOKEN.equals(token)
                    && !DOCTOR_TOKEN.equals(token)
                    && !NO_PERMISSION_PATIENT_TOKEN.equals(token)) {
                throw new IllegalArgumentException("invalid token");
            }
            return new AccessTokenClaims(2000L, "token-id", "session-id", Instant.parse("2026-05-04T10:00:00Z"));
        }
    }

    private static final class StubAccessTokenBlocklistPort implements me.jianwen.mediask.domain.user.port.AccessTokenBlocklistPort {
        @Override
        public void block(String tokenId, Instant expiresAt) {
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
        public Optional<AuthenticatedUser> findAuthenticatedUserById(Long userId) {
            return Optional.of(authenticatedUser);
        }

        @Override
        public Optional<me.jianwen.mediask.domain.user.model.LoginAccount> findLoginAccountByPhone(String phone) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateLastLoginAt(Long userId) {
            throw new UnsupportedOperationException();
        }
    }
}
