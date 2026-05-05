package me.jianwen.mediask.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Period;
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
import me.jianwen.mediask.api.authz.EmrRecordResourceAccessResolver;
import me.jianwen.mediask.api.authz.EmrRecordResourceReferenceAssembler;
import me.jianwen.mediask.api.exception.GlobalExceptionHandler;
import me.jianwen.mediask.api.filter.RequestCorrelationFilter;
import me.jianwen.mediask.api.security.ApiCorsProperties;
import me.jianwen.mediask.api.security.ApiSecurityProperties;
import me.jianwen.mediask.api.security.JsonAuthenticationEntryPoint;
import me.jianwen.mediask.api.security.JwtAuthenticationFilter;
import me.jianwen.mediask.api.security.ScenarioAuthorizationAspect;
import me.jianwen.mediask.api.security.SecurityConfig;
import me.jianwen.mediask.application.audit.model.AuditContext;
import me.jianwen.mediask.application.authz.AuthorizationDecisionService;
import me.jianwen.mediask.application.clinical.query.GetEncounterDetailQuery;
import me.jianwen.mediask.application.clinical.query.GetEncounterAiSummaryQuery;
import me.jianwen.mediask.application.clinical.query.ListEncounterHistoryEmrsQuery;
import me.jianwen.mediask.application.clinical.query.ListEncountersQuery;
import me.jianwen.mediask.application.clinical.usecase.GetEncounterAiSummaryUseCase;
import me.jianwen.mediask.application.clinical.usecase.GetEncounterDetailUseCase;
import me.jianwen.mediask.application.clinical.usecase.ListEncounterHistoryEmrsUseCase;
import me.jianwen.mediask.application.clinical.usecase.ListEncountersUseCase;
import me.jianwen.mediask.application.clinical.usecase.UpdateEncounterStatusResult;
import me.jianwen.mediask.application.clinical.usecase.UpdateEncounterStatusUseCase;
import me.jianwen.mediask.application.clinical.command.UpdateEncounterStatusCommand;
import me.jianwen.mediask.domain.ai.model.AiTriageCitation;
import me.jianwen.mediask.domain.ai.model.AiTriageRecommendedDepartment;
import me.jianwen.mediask.domain.clinical.model.EncounterAiSummary;
import me.jianwen.mediask.domain.clinical.exception.ClinicalErrorCode;
import me.jianwen.mediask.domain.clinical.model.EncounterDetail;
import me.jianwen.mediask.domain.clinical.model.EncounterListItem;
import me.jianwen.mediask.domain.clinical.model.EncounterPatientSummary;
import me.jianwen.mediask.domain.clinical.model.EmrRecordListItem;
import me.jianwen.mediask.domain.clinical.model.EmrRecordStatus;
import me.jianwen.mediask.domain.clinical.model.VisitEncounterStatus;
import me.jianwen.mediask.domain.clinical.port.EncounterQueryRepository;
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

class EncounterControllerTest {

    private static final String DOCTOR_TOKEN = "doctor-token";
    private static final String PATIENT_TOKEN = "patient-token";
    private static final String NO_PERMISSION_DOCTOR_TOKEN = "no-permission-doctor-token";

    private final ObjectMapper objectMapper = buildObjectMapper();

    private MockMvc doctorMockMvc;
    private MockMvc patientMockMvc;
    private MockMvc noPermissionDoctorMockMvc;
    private StubListEncountersUseCase doctorListEncountersUseCase;
    private StubGetEncounterDetailUseCase doctorGetEncounterDetailUseCase;
    private StubGetEncounterAiSummaryUseCase doctorGetEncounterAiSummaryUseCase;
    private StubListEncounterHistoryEmrsUseCase doctorListEncounterHistoryEmrsUseCase;
    private StubUpdateEncounterStatusUseCase doctorUpdateEncounterStatusUseCase;
    private StubEncounterQueryRepository authzEncounterQueryRepository;

    @BeforeEach
    void setUp() {
        doctorListEncountersUseCase = new StubListEncountersUseCase();
        doctorGetEncounterDetailUseCase = new StubGetEncounterDetailUseCase();
        doctorGetEncounterAiSummaryUseCase = new StubGetEncounterAiSummaryUseCase();
        doctorListEncounterHistoryEmrsUseCase = new StubListEncounterHistoryEmrsUseCase();
        doctorUpdateEncounterStatusUseCase = new StubUpdateEncounterStatusUseCase();
        authzEncounterQueryRepository = new StubEncounterQueryRepository();
        doctorMockMvc = buildMockMvc(new AuthenticatedUser(
                2004L,
                "doctor_zhang",
                "张医生",
                UserType.DOCTOR,
                new LinkedHashSet<>(List.of(RoleCode.DOCTOR)),
                Set.of("encounter:query", "encounter:update", "emr:read"),
                Set.of(new me.jianwen.mediask.domain.user.model.DataScopeRule(
                        "EMR_RECORD",
                        me.jianwen.mediask.domain.user.model.DataScopeType.DEPARTMENT,
                        3101L)),
                null,
                2101L,
                3101L),
                doctorListEncountersUseCase,
                doctorGetEncounterDetailUseCase,
                doctorGetEncounterAiSummaryUseCase,
                doctorListEncounterHistoryEmrsUseCase,
                doctorUpdateEncounterStatusUseCase);
        patientMockMvc = buildMockMvc(new AuthenticatedUser(
                2003L,
                "patient_li",
                "李患者",
                UserType.PATIENT,
                new LinkedHashSet<>(List.of(RoleCode.PATIENT)),
                Set.of("registration:query:self"),
                Set.of(),
                2201L,
                null,
                null),
                new StubListEncountersUseCase(),
                new StubGetEncounterDetailUseCase(),
                new StubGetEncounterAiSummaryUseCase(),
                new StubListEncounterHistoryEmrsUseCase(),
                new StubUpdateEncounterStatusUseCase());
        noPermissionDoctorMockMvc = buildMockMvc(new AuthenticatedUser(
                2005L,
                "doctor_wang",
                "王医生",
                UserType.DOCTOR,
                new LinkedHashSet<>(List.of(RoleCode.DOCTOR)),
                Set.of(),
                Set.of(),
                null,
                2102L,
                3101L),
                new StubListEncountersUseCase(),
                new StubGetEncounterDetailUseCase(),
                new StubGetEncounterAiSummaryUseCase(),
                new StubListEncounterHistoryEmrsUseCase(),
                new StubUpdateEncounterStatusUseCase());
    }

    @Test
    void list_WhenAuthenticatedDoctor_ReturnOwnEncounters() throws Exception {
        doctorMockMvc.perform(get("/api/v1/encounters")
                        .header("Authorization", "Bearer " + DOCTOR_TOKEN)
                        .param("status", "SCHEDULED")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.items[0].encounterId").value("8101"))
                .andExpect(jsonPath("$.data.items[0].registrationId").value("6101"))
                .andExpect(jsonPath("$.data.items[0].patientUserId").value("2003"))
                .andExpect(jsonPath("$.data.items[0].patientName").value("李患者"))
                .andExpect(jsonPath("$.data.items[0].departmentId").value("3101"))
                .andExpect(jsonPath("$.data.items[0].departmentName").value("心内科"))
                .andExpect(jsonPath("$.data.items[0].periodCode").value("MORNING"))
                .andExpect(jsonPath("$.data.items[0].startedAt").value("2026-04-03T09:00:00+08:00"))
                .andExpect(jsonPath("$.data.items[0].encounterStatus").value("SCHEDULED"));

        assertEquals(2101L, doctorListEncountersUseCase.lastQuery.doctorId());
        assertEquals(VisitEncounterStatus.SCHEDULED, doctorListEncountersUseCase.lastQuery.status());
    }

    @Test
    void list_WhenUnauthenticated_ReturnUnauthorized() throws Exception {
        doctorMockMvc.perform(get("/api/v1/encounters"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void list_WhenAuthenticatedPatientWithoutPermission_ReturnForbidden() throws Exception {
        patientMockMvc.perform(get("/api/v1/encounters")
                        .header("Authorization", "Bearer " + PATIENT_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1003));
    }

    @Test
    void list_WhenDoctorMissingPermission_ReturnForbidden() throws Exception {
        noPermissionDoctorMockMvc.perform(get("/api/v1/encounters")
                        .header("Authorization", "Bearer " + NO_PERMISSION_DOCTOR_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1003));
    }

    @Test
    void list_WhenStatusInvalid_ReturnBadRequest() throws Exception {
        doctorMockMvc.perform(get("/api/v1/encounters")
                        .header("Authorization", "Bearer " + DOCTOR_TOKEN)
                        .param("status", "WAITING"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1002));
    }

    @Test
    void detail_WhenAuthenticatedDoctor_ReturnOwnEncounter() throws Exception {
        doctorMockMvc.perform(get("/api/v1/encounters/8101")
                        .header("Authorization", "Bearer " + DOCTOR_TOKEN)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.encounterId").value("8101"))
                .andExpect(jsonPath("$.data.registrationId").value("6101"))
                .andExpect(jsonPath("$.data.patientSummary.patientUserId").value("2003"))
                .andExpect(jsonPath("$.data.patientSummary.patientName").value("李患者"))
                .andExpect(jsonPath("$.data.patientSummary.gender").value("FEMALE"))
                .andExpect(jsonPath("$.data.patientSummary.departmentId").value("3101"))
                .andExpect(jsonPath("$.data.patientSummary.departmentName").value("心内科"))
                .andExpect(jsonPath("$.data.patientSummary.periodCode").value("MORNING"))
                .andExpect(jsonPath("$.data.patientSummary.startedAt").value("2026-04-03T09:00:00+08:00"))
                .andExpect(jsonPath("$.data.patientSummary.encounterStatus").value("SCHEDULED"))
                .andExpect(jsonPath("$.data.patientSummary.age").value(
                        Period.between(LocalDate.parse("1990-05-15"), LocalDate.parse("2026-04-03")).getYears()));

        assertEquals(8101L, doctorGetEncounterDetailUseCase.lastQuery.encounterId());
        assertEquals(2101L, doctorGetEncounterDetailUseCase.lastQuery.doctorId());
    }

    @Test
    void detail_WhenAuthenticatedPatientWithoutPermission_ReturnForbidden() throws Exception {
        patientMockMvc.perform(get("/api/v1/encounters/8101")
                        .header("Authorization", "Bearer " + PATIENT_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1003));
    }

    @Test
    void detail_WhenEncounterMissing_ReturnNotFound() throws Exception {
        doctorGetEncounterDetailUseCase.throwNotFound = true;

        doctorMockMvc.perform(get("/api/v1/encounters/9999")
                        .header("Authorization", "Bearer " + DOCTOR_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(4004));
    }

    @Test
    void detail_WhenEncounterBelongsToAnotherDoctor_ReturnForbidden() throws Exception {
        doctorGetEncounterDetailUseCase.throwAccessDenied = true;

        doctorMockMvc.perform(get("/api/v1/encounters/8101")
                        .header("Authorization", "Bearer " + DOCTOR_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(4003));
    }

    @Test
    void aiSummary_WhenAuthenticatedDoctor_ReturnStructuredSummary() throws Exception {
        doctorMockMvc.perform(get("/api/v1/encounters/8101/ai-summary")
                        .header("Authorization", "Bearer " + DOCTOR_TOKEN)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.encounterId").value("8101"))
                .andExpect(jsonPath("$.data.sessionId").value("session-1"))
                .andExpect(jsonPath("$.data.chiefComplaintSummary").value("头痛"))
                .andExpect(jsonPath("$.data.riskLevel").value("low"))
                .andExpect(jsonPath("$.data.recommendedDepartments[0].departmentId").value("3101"))
                .andExpect(jsonPath("$.data.careAdvice").value("建议门诊就诊"))
                .andExpect(jsonPath("$.data.citations[0].chunkId").value("chunk-1"))
                .andExpect(jsonPath("$.data.catalogVersion").value("deptcat-v20260501-01"))
                .andExpect(jsonPath("$.data.finalizedAt").value("2026-05-01T09:03:00+08:00"));

        assertEquals(8101L, doctorGetEncounterAiSummaryUseCase.lastQuery.encounterId());
        assertEquals(2101L, doctorGetEncounterAiSummaryUseCase.lastQuery.doctorId());
    }

    @Test
    void aiSummary_WhenMissing_ReturnNotFound() throws Exception {
        doctorGetEncounterAiSummaryUseCase.throwSummaryNotFound = true;

        doctorMockMvc.perform(get("/api/v1/encounters/8101/ai-summary")
                        .header("Authorization", "Bearer " + DOCTOR_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(4005));
    }

    @Test
    void emrHistory_WhenAuthenticatedDoctor_ReturnPatientHistory() throws Exception {
        doctorMockMvc.perform(get("/api/v1/encounters/8101/emr-history")
                        .header("Authorization", "Bearer " + DOCTOR_TOKEN)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.items[0].emrRecordId").value("7101"))
                .andExpect(jsonPath("$.data.items[0].encounterId").value("8001"))
                .andExpect(jsonPath("$.data.items[0].recordNo").value("EMR20260418001"))
                .andExpect(jsonPath("$.data.items[0].doctorId").value("2101"))
                .andExpect(jsonPath("$.data.items[0].departmentId").value("3101"))
                .andExpect(jsonPath("$.data.items[0].sessionDate").value("2026-04-01"))
                .andExpect(jsonPath("$.data.items[0].createdAt").value("2026-04-01T08:30:00+08:00"));

        assertEquals(8101L, doctorListEncounterHistoryEmrsUseCase.lastQuery.encounterId());
    }

    @Test
    void updateStatus_WhenAuthenticatedDoctor_ReturnUpdatedEncounter() throws Exception {
        doctorMockMvc.perform(patch("/api/v1/encounters/8101")
                        .header("Authorization", "Bearer " + DOCTOR_TOKEN)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "action": "START"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.encounterId").value("8101"))
                .andExpect(jsonPath("$.data.encounterStatus").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.startedAt").value("2026-04-03T09:00:00+08:00"))
                .andExpect(jsonPath("$.data.endedAt").doesNotExist());

        assertEquals(8101L, doctorUpdateEncounterStatusUseCase.lastCommand.encounterId());
        assertEquals(2101L, doctorUpdateEncounterStatusUseCase.lastCommand.doctorId());
        assertEquals(UpdateEncounterStatusCommand.Action.START, doctorUpdateEncounterStatusUseCase.lastCommand.action());
    }

    @Test
    void updateStatus_WhenInvalidAction_ReturnBadRequest() throws Exception {
        doctorMockMvc.perform(patch("/api/v1/encounters/8101")
                        .header("Authorization", "Bearer " + DOCTOR_TOKEN)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "action": "INVALID"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1002));
    }

    @Test
    void updateStatus_WhenDoctorMissingPermission_ReturnForbidden() throws Exception {
        noPermissionDoctorMockMvc.perform(patch("/api/v1/encounters/8101")
                        .header("Authorization", "Bearer " + NO_PERMISSION_DOCTOR_TOKEN)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "action": "START"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1003));
    }

    @Test
    void updateStatus_WhenEncounterNotFound_ReturnNotFound() throws Exception {
        doctorUpdateEncounterStatusUseCase.throwNotFound = true;

        doctorMockMvc.perform(patch("/api/v1/encounters/9999")
                        .header("Authorization", "Bearer " + DOCTOR_TOKEN)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "action": "START"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(4004));
    }

    @Test
    void updateStatus_WhenTransitionNotAllowed_ReturnConflict() throws Exception {
        doctorUpdateEncounterStatusUseCase.throwTransitionNotAllowed = true;

        doctorMockMvc.perform(patch("/api/v1/encounters/8101")
                        .header("Authorization", "Bearer " + DOCTOR_TOKEN)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "action": "COMPLETE"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(4010));
    }

    private MockMvc buildMockMvc(
            AuthenticatedUser authenticatedUser,
            StubListEncountersUseCase listEncountersUseCase,
            StubGetEncounterDetailUseCase getEncounterDetailUseCase,
            StubGetEncounterAiSummaryUseCase getEncounterAiSummaryUseCase,
            StubListEncounterHistoryEmrsUseCase listEncounterHistoryEmrsUseCase,
            StubUpdateEncounterStatusUseCase updateEncounterStatusUseCase) {
        EncounterController target = new EncounterController(
                listEncountersUseCase,
                getEncounterDetailUseCase,
                getEncounterAiSummaryUseCase,
                listEncounterHistoryEmrsUseCase,
                updateEncounterStatusUseCase,
                TestAuditSupport.auditApiSupport());
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(target);
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAspect(new ScenarioAuthorizationAspect(
                new AuthorizationDecisionService(
                        List.of(new EmrRecordResourceReferenceAssembler()),
                        List.of(new EmrRecordResourceAccessResolver(authzEncounterQueryRepository))),
                TestAuditSupport.auditApiSupport(),
                authzEncounterQueryRepository,
                TestAuditSupport.emptyAdminPatientQueryRepository()));
        EncounterController controller = proxyFactory.getProxy();

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

    private static final class StubListEncountersUseCase extends ListEncountersUseCase {

        private ListEncountersQuery lastQuery;

        private StubListEncountersUseCase() {
            super(null);
        }

        @Override
        public List<EncounterListItem> handle(ListEncountersQuery query) {
            this.lastQuery = query;
            return List.of(new EncounterListItem(
                    8101L,
                    6101L,
                    2003L,
                    "李患者",
                    3101L,
                    "心内科",
                    LocalDate.parse("2026-04-03"),
                    "MORNING",
                    VisitEncounterStatus.SCHEDULED,
                    OffsetDateTime.parse("2026-04-03T09:00:00+08:00"),
                    null));
        }
    }

    private static final class StubGetEncounterDetailUseCase extends GetEncounterDetailUseCase {

        private GetEncounterDetailQuery lastQuery;
        private boolean throwNotFound;
        private boolean throwAccessDenied;

        private StubGetEncounterDetailUseCase() {
            super(null);
        }

        @Override
        public EncounterDetail handle(GetEncounterDetailQuery query) {
            this.lastQuery = query;
            if (throwAccessDenied) {
                throw new me.jianwen.mediask.common.exception.BizException(ClinicalErrorCode.ENCOUNTER_ACCESS_DENIED);
            }
            if (throwNotFound) {
                throw new me.jianwen.mediask.common.exception.BizException(ClinicalErrorCode.ENCOUNTER_NOT_FOUND);
            }
            return new EncounterDetail(
                    8101L,
                    6101L,
                    2101L,
                    new EncounterPatientSummary(
                            2003L,
                            "李患者",
                            "FEMALE",
                            3101L,
                            "心内科",
                            LocalDate.parse("2026-04-03"),
                            "MORNING",
                            VisitEncounterStatus.SCHEDULED,
                            OffsetDateTime.parse("2026-04-03T09:00:00+08:00"),
                            null,
                            LocalDate.parse("1990-05-15")));
        }
    }

    private static final class StubUpdateEncounterStatusUseCase extends UpdateEncounterStatusUseCase {

        private UpdateEncounterStatusCommand lastCommand;
        private boolean throwNotFound;
        private boolean throwTransitionNotAllowed;

        private StubUpdateEncounterStatusUseCase() {
            super(null, null, null, TestAuditSupport.auditTrailService());
        }

        @Override
        public UpdateEncounterStatusResult handle(UpdateEncounterStatusCommand command, AuditContext auditContext) {
            this.lastCommand = command;
            if (throwNotFound) {
                throw new me.jianwen.mediask.common.exception.BizException(ClinicalErrorCode.ENCOUNTER_NOT_FOUND);
            }
            if (throwTransitionNotAllowed) {
                throw new me.jianwen.mediask.common.exception.BizException(
                        ClinicalErrorCode.ENCOUNTER_STATUS_TRANSITION_NOT_ALLOWED);
            }
            return new UpdateEncounterStatusResult(
                    command.encounterId(),
                    VisitEncounterStatus.IN_PROGRESS,
                    OffsetDateTime.parse("2026-04-03T09:00:00+08:00"),
                    null);
        }
    }

    private static final class StubGetEncounterAiSummaryUseCase extends GetEncounterAiSummaryUseCase {

        private GetEncounterAiSummaryQuery lastQuery;
        private boolean throwSummaryNotFound;

        private StubGetEncounterAiSummaryUseCase() {
            super(null, null, null, TestAuditSupport.auditTrailService());
        }

        @Override
        public EncounterAiSummary handle(GetEncounterAiSummaryQuery query, AuditContext auditContext) {
            this.lastQuery = query;
            if (throwSummaryNotFound) {
                throw new me.jianwen.mediask.common.exception.BizException(
                        ClinicalErrorCode.ENCOUNTER_AI_SUMMARY_NOT_FOUND);
            }
            return new EncounterAiSummary(
                    query.encounterId(),
                    2003L,
                    "session-1",
                    "头痛",
                    "low",
                    List.of(new AiTriageRecommendedDepartment(3101L, "神经内科", 1, "头痛优先神经内科")),
                    "建议门诊就诊",
                    List.of(new AiTriageCitation(1, "chunk-1", "头痛优先神经内科")),
                    null,
                    "deptcat-v20260501-01",
                    OffsetDateTime.parse("2026-05-01T09:03:00+08:00"));
        }
    }

    private static final class StubListEncounterHistoryEmrsUseCase extends ListEncounterHistoryEmrsUseCase {

        private ListEncounterHistoryEmrsQuery lastQuery;

        private StubListEncounterHistoryEmrsUseCase() {
            super(null, null, TestAuditSupport.auditTrailService());
        }

        @Override
        public List<EmrRecordListItem> handle(
                ListEncounterHistoryEmrsQuery query,
                AuditContext auditContext) {
            this.lastQuery = query;
            return List.of(new EmrRecordListItem(
                    7101L,
                    "EMR20260418001",
                    8001L,
                    EmrRecordStatus.DRAFT,
                    3101L,
                    "心内科",
                    2101L,
                    "张医生",
                    LocalDate.parse("2026-04-01"),
                    "反复头痛 1 周",
                    OffsetDateTime.parse("2026-04-01T08:30:00+08:00")));
        }
    }

    private static final class StubEncounterQueryRepository implements EncounterQueryRepository {

        @Override
        public List<EncounterListItem> listByDoctorId(Long doctorId, VisitEncounterStatus status) {
            return List.of();
        }

        @Override
        public Optional<EncounterDetail> findDetailByEncounterId(Long encounterId) {
            if (!Long.valueOf(8101L).equals(encounterId)) {
                return Optional.empty();
            }
            return Optional.of(new EncounterDetail(
                    8101L,
                    6101L,
                    2101L,
                    new EncounterPatientSummary(
                            2003L,
                            "李患者",
                            "FEMALE",
                            3101L,
                            "心内科",
                            LocalDate.parse("2026-04-03"),
                            "MORNING",
                            VisitEncounterStatus.SCHEDULED,
                            OffsetDateTime.parse("2026-04-03T09:00:00+08:00"),
                            null,
                            LocalDate.parse("1990-05-15"))));
        }
    }

    private static final class StubAccessTokenCodec implements AccessTokenCodec {

        @Override
        public AccessToken issueAccessToken(AuthenticatedUser user, String refreshTokenSessionId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AccessTokenClaims parseAccessToken(String token) {
            if (!DOCTOR_TOKEN.equals(token) && !PATIENT_TOKEN.equals(token) && !NO_PERMISSION_DOCTOR_TOKEN.equals(token)) {
                throw new IllegalArgumentException("invalid token");
            }
            return new AccessTokenClaims(2000L, "token-id", "session-id", Instant.parse("2026-04-02T12:00:00Z"));
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
        public Optional<me.jianwen.mediask.domain.user.model.LoginAccount> findLoginAccountByUsername(String username) {
            throw new UnsupportedOperationException();
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
