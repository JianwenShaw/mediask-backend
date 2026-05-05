package me.jianwen.mediask.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import me.jianwen.mediask.api.TestAuditSupport;
import me.jianwen.mediask.api.authz.EmrRecordResourceAccessResolver;
import me.jianwen.mediask.api.authz.EmrRecordResourceReferenceAssembler;
import me.jianwen.mediask.api.authz.PrescriptionResourceAccessResolver;
import me.jianwen.mediask.api.authz.PrescriptionResourceReferenceAssembler;
import me.jianwen.mediask.api.exception.GlobalExceptionHandler;
import me.jianwen.mediask.api.filter.RequestCorrelationFilter;
import me.jianwen.mediask.api.security.JsonAuthenticationEntryPoint;
import me.jianwen.mediask.api.security.JwtAuthenticationFilter;
import me.jianwen.mediask.api.security.ScenarioAuthorizationAspect;
import me.jianwen.mediask.application.audit.model.AuditContext;
import me.jianwen.mediask.application.authz.AuthorizationDecisionService;
import me.jianwen.mediask.application.clinical.command.CancelPrescriptionCommand;
import me.jianwen.mediask.application.clinical.command.CreatePrescriptionCommand;
import me.jianwen.mediask.application.clinical.command.IssuePrescriptionCommand;
import me.jianwen.mediask.application.clinical.command.UpdatePrescriptionItemsCommand;
import me.jianwen.mediask.application.clinical.query.GetPrescriptionDetailQuery;
import me.jianwen.mediask.application.clinical.usecase.CancelPrescriptionUseCase;
import me.jianwen.mediask.application.clinical.usecase.CreatePrescriptionUseCase;
import me.jianwen.mediask.application.clinical.usecase.GetPrescriptionDetailUseCase;
import me.jianwen.mediask.application.clinical.usecase.IssuePrescriptionUseCase;
import me.jianwen.mediask.application.clinical.usecase.UpdatePrescriptionItemsUseCase;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.clinical.exception.ClinicalErrorCode;
import me.jianwen.mediask.domain.clinical.model.EncounterDetail;
import me.jianwen.mediask.domain.clinical.model.EncounterPatientSummary;
import me.jianwen.mediask.domain.clinical.model.EmrRecordAccess;
import me.jianwen.mediask.domain.clinical.model.EmrRecordListItem;
import me.jianwen.mediask.domain.clinical.model.PrescriptionItem;
import me.jianwen.mediask.domain.clinical.model.PrescriptionOrder;
import me.jianwen.mediask.domain.clinical.model.PrescriptionStatus;
import me.jianwen.mediask.domain.clinical.model.VisitEncounterStatus;
import me.jianwen.mediask.domain.clinical.port.EncounterQueryRepository;
import me.jianwen.mediask.domain.clinical.port.EmrRecordQueryRepository;
import me.jianwen.mediask.domain.user.model.AccessToken;
import me.jianwen.mediask.domain.user.model.AccessTokenClaims;
import me.jianwen.mediask.domain.user.model.AuthenticatedUser;
import me.jianwen.mediask.domain.user.model.DataScopeRule;
import me.jianwen.mediask.domain.user.model.DataScopeType;
import me.jianwen.mediask.domain.user.model.RoleCode;
import me.jianwen.mediask.domain.user.model.UserType;
import me.jianwen.mediask.domain.user.port.AccessTokenBlocklistPort;
import me.jianwen.mediask.domain.user.port.AccessTokenCodec;
import me.jianwen.mediask.domain.user.port.UserAuthenticationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.filter.OncePerRequestFilter;

class PrescriptionControllerTest {

    private static final String DOCTOR_TOKEN = "doctor_test_token";
    private static final String PATIENT_TOKEN = "patient_test_token";

    private MockMvc doctorMockMvc;
    private MockMvc doctorWithoutPermissionMockMvc;
    private MockMvc patientMockMvc;
    private MockMvc unauthenticatedMockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final StubCreatePrescriptionUseCase createPrescriptionUseCase = new StubCreatePrescriptionUseCase();
    private final StubGetPrescriptionDetailUseCase getPrescriptionDetailUseCase = new StubGetPrescriptionDetailUseCase();
    private final StubUpdatePrescriptionItemsUseCase updatePrescriptionItemsUseCase = new StubUpdatePrescriptionItemsUseCase();
    private final StubIssuePrescriptionUseCase issuePrescriptionUseCase = new StubIssuePrescriptionUseCase();
    private final StubCancelPrescriptionUseCase cancelPrescriptionUseCase = new StubCancelPrescriptionUseCase();
    private final StubEmrRecordQueryRepository emrRecordQueryRepository = new StubEmrRecordQueryRepository();
    private final StubEncounterQueryRepository encounterQueryRepository = new StubEncounterQueryRepository();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        createPrescriptionUseCase.throwErrorCode = null;
        getPrescriptionDetailUseCase.throwErrorCode = null;
        updatePrescriptionUseCaseReset();
        emrRecordQueryRepository.reset();
        encounterQueryRepository.reset();
        doctorMockMvc = buildMockMvc(new AuthenticatedUser(
                2001L,
                "doctor_li",
                "李医生",
                UserType.DOCTOR,
                new LinkedHashSet<>(List.of(RoleCode.DOCTOR)),
                Set.of("prescription:create", "prescription:read", "prescription:update", "prescription:issue", "prescription:cancel"),
                Set.of(new DataScopeRule("PRESCRIPTION_ORDER", DataScopeType.DEPARTMENT, 3101L)),
                null,
                2101L,
                3101L));
        doctorWithoutPermissionMockMvc = buildMockMvc(new AuthenticatedUser(
                2001L,
                "doctor_wu",
                "吴医生",
                UserType.DOCTOR,
                new LinkedHashSet<>(List.of(RoleCode.DOCTOR)),
                Set.of(),
                Set.of(new DataScopeRule("PRESCRIPTION_ORDER", DataScopeType.DEPARTMENT, 3101L)),
                null,
                2108L,
                3101L));

        patientMockMvc = buildMockMvc(new AuthenticatedUser(
                1001L,
                "patient_zhang",
                "张患者",
                UserType.PATIENT,
                new LinkedHashSet<>(List.of(RoleCode.PATIENT)),
                Set.of("prescription:create", "prescription:read", "prescription:update", "prescription:issue", "prescription:cancel"),
                Set.of(new DataScopeRule("PRESCRIPTION_ORDER", DataScopeType.SELF, null)),
                1101L,
                null,
                null));

        unauthenticatedMockMvc = buildMockMvc(null);
    }

    // --- Create tests (existing) ---

    @Test
    void create_WhenAuthenticatedDoctorWithPermission_ReturnsPrescription() throws Exception {
        doctorMockMvc.perform(post("/api/v1/prescriptions")
                        .header("Authorization", "Bearer " + DOCTOR_TOKEN)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "encounterId":8101,
                                  "items":[
                                    {
                                      "sortOrder":0,
                                      "drugName":"阿莫西林胶囊",
                                      "drugSpecification":"0.25g*24粒",
                                      "dosageText":"每次2粒",
                                      "frequencyText":"每日3次",
                                      "durationText":"5天",
                                      "quantity":30,
                                      "unit":"粒",
                                      "route":"口服"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.prescriptionOrderId").exists())
                .andExpect(jsonPath("$.data.encounterId").value(8101))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.items[0].drugName").value("阿莫西林胶囊"));

        assertEquals(2101L, createPrescriptionUseCase.lastCommand.doctorId());
        assertEquals(8101L, createPrescriptionUseCase.lastCommand.encounterId());
    }

    @Test
    void create_WhenUnauthenticated_ReturnsUnauthorized() throws Exception {
        unauthenticatedMockMvc.perform(post("/api/v1/prescriptions")
                        .contentType(APPLICATION_JSON)
                        .content("{\"encounterId\":8101,\"items\":[{\"sortOrder\":0,\"drugName\":\"阿莫西林\",\"quantity\":1}]}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void create_WhenAuthenticatedPatient_ReturnsForbidden() throws Exception {
        patientMockMvc.perform(post("/api/v1/prescriptions")
                        .header("Authorization", "Bearer " + PATIENT_TOKEN)
                        .contentType(APPLICATION_JSON)
                        .content("{\"encounterId\":8101,\"items\":[{\"sortOrder\":0,\"drugName\":\"阿莫西林\",\"quantity\":1}]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(2008));
    }

    @Test
    void create_WhenDoctorWithoutPermission_ReturnsForbidden() throws Exception {
        doctorWithoutPermissionMockMvc.perform(post("/api/v1/prescriptions")
                        .header("Authorization", "Bearer " + DOCTOR_TOKEN)
                        .contentType(APPLICATION_JSON)
                        .content("{\"encounterId\":8101,\"items\":[{\"sortOrder\":0,\"drugName\":\"阿莫西林\",\"quantity\":1}]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1003));
    }

    @Test
    void create_WhenEmrMissing_ReturnsNotFound() throws Exception {
        createPrescriptionUseCase.throwErrorCode = ClinicalErrorCode.PRESCRIPTION_EMR_RECORD_NOT_FOUND;

        doctorMockMvc.perform(post("/api/v1/prescriptions")
                        .header("Authorization", "Bearer " + DOCTOR_TOKEN)
                        .contentType(APPLICATION_JSON)
                        .content("{\"encounterId\":8101,\"items\":[{\"sortOrder\":0,\"drugName\":\"阿莫西林\",\"quantity\":1}]}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ClinicalErrorCode.PRESCRIPTION_EMR_RECORD_NOT_FOUND.getCode()));
    }

    // --- Detail tests (existing) ---

    @Test
    void detail_WhenAuthenticatedDoctorWithPermission_ReturnsPrescription() throws Exception {
        doctorMockMvc.perform(get("/api/v1/prescriptions/8101")
                        .header("Authorization", "Bearer " + DOCTOR_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.prescriptionOrderId").value(7101))
                .andExpect(jsonPath("$.data.items[0].drugName").value("阿莫西林胶囊"));

        assertEquals(8101L, getPrescriptionDetailUseCase.lastQuery.encounterId());
        assertEquals(2101L, getPrescriptionDetailUseCase.lastQuery.doctorId());
    }

    @Test
    void detail_WhenAuthenticatedPatientWithPermission_ReturnsPrescription() throws Exception {
        patientMockMvc.perform(get("/api/v1/prescriptions/8101")
                        .header("Authorization", "Bearer " + PATIENT_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.prescriptionOrderId").value(7101));

        assertEquals(1001L, getPrescriptionDetailUseCase.lastQuery.patientUserId());
    }

    @Test
    void detail_WhenPrescriptionMissing_ReturnsNotFound() throws Exception {
        getPrescriptionDetailUseCase.throwErrorCode = ClinicalErrorCode.PRESCRIPTION_NOT_FOUND;

        doctorMockMvc.perform(get("/api/v1/prescriptions/8101")
                        .header("Authorization", "Bearer " + DOCTOR_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ClinicalErrorCode.PRESCRIPTION_NOT_FOUND.getCode()));
    }

    @Test
    void detail_WhenDoctorWithoutPermission_ReturnsForbidden() throws Exception {
        doctorWithoutPermissionMockMvc.perform(get("/api/v1/prescriptions/8101")
                        .header("Authorization", "Bearer " + DOCTOR_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1003));
    }

    // --- UpdateItems tests ---

    @Test
    void updateItems_WhenAuthenticatedDoctor_ReturnsUpdatedPrescription() throws Exception {
        doctorMockMvc.perform(patch("/api/v1/prescriptions/8101/items")
                        .header("Authorization", "Bearer " + DOCTOR_TOKEN)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "items":[
                                    {
                                      "sortOrder":0,
                                      "drugName":"布洛芬缓释胶囊",
                                      "drugSpecification":"0.3g*20粒",
                                      "dosageText":"每次1粒",
                                      "frequencyText":"每日2次",
                                      "durationText":"3天",
                                      "quantity":6,
                                      "unit":"粒",
                                      "route":"口服"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.items[0].drugName").value("布洛芬缓释胶囊"));

        assertEquals(2101L, updatePrescriptionItemsUseCase.lastCommand.doctorId());
        assertEquals(8101L, updatePrescriptionItemsUseCase.lastCommand.encounterId());
    }

    @Test
    void updateItems_WhenUnauthenticated_ReturnsUnauthorized() throws Exception {
        unauthenticatedMockMvc.perform(patch("/api/v1/prescriptions/8101/items")
                        .contentType(APPLICATION_JSON)
                        .content("{\"items\":[{\"sortOrder\":0,\"drugName\":\"阿莫西林\",\"quantity\":1}]}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void updateItems_WhenPatient_ReturnsForbidden() throws Exception {
        patientMockMvc.perform(patch("/api/v1/prescriptions/8101/items")
                        .header("Authorization", "Bearer " + PATIENT_TOKEN)
                        .contentType(APPLICATION_JSON)
                        .content("{\"items\":[{\"sortOrder\":0,\"drugName\":\"阿莫西林\",\"quantity\":1}]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(2008));
    }

    @Test
    void updateItems_WhenNotDRAFT_ReturnsConflict() throws Exception {
        updatePrescriptionItemsUseCase.throwErrorCode = ClinicalErrorCode.PRESCRIPTION_STATUS_TRANSITION_NOT_ALLOWED;

        doctorMockMvc.perform(patch("/api/v1/prescriptions/8101/items")
                        .header("Authorization", "Bearer " + DOCTOR_TOKEN)
                        .contentType(APPLICATION_JSON)
                        .content("{\"items\":[{\"sortOrder\":0,\"drugName\":\"阿莫西林\",\"quantity\":1}]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ClinicalErrorCode.PRESCRIPTION_STATUS_TRANSITION_NOT_ALLOWED.getCode()));
    }

    @Test
    void updateItems_WhenPrescriptionNotFound_ReturnsNotFound() throws Exception {
        updatePrescriptionItemsUseCase.throwErrorCode = ClinicalErrorCode.PRESCRIPTION_NOT_FOUND;

        doctorMockMvc.perform(patch("/api/v1/prescriptions/8101/items")
                        .header("Authorization", "Bearer " + DOCTOR_TOKEN)
                        .contentType(APPLICATION_JSON)
                        .content("{\"items\":[{\"sortOrder\":0,\"drugName\":\"阿莫西林\",\"quantity\":1}]}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ClinicalErrorCode.PRESCRIPTION_NOT_FOUND.getCode()));
    }

    // --- Issue tests ---

    @Test
    void issue_WhenAuthenticatedDoctor_ReturnsIssuedPrescription() throws Exception {
        doctorMockMvc.perform(post("/api/v1/prescriptions/8101/issue")
                        .header("Authorization", "Bearer " + DOCTOR_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("ISSUED"))
                .andExpect(jsonPath("$.data.prescriptionOrderId").value(7101));

        assertEquals(2101L, issuePrescriptionUseCase.lastCommand.doctorId());
        assertEquals(8101L, issuePrescriptionUseCase.lastCommand.encounterId());
    }

    @Test
    void issue_WhenUnauthenticated_ReturnsUnauthorized() throws Exception {
        unauthenticatedMockMvc.perform(post("/api/v1/prescriptions/8101/issue"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void issue_WhenPatient_ReturnsForbidden() throws Exception {
        patientMockMvc.perform(post("/api/v1/prescriptions/8101/issue")
                        .header("Authorization", "Bearer " + PATIENT_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(2008));
    }

    @Test
    void issue_WhenAlreadyIssued_ReturnsConflict() throws Exception {
        issuePrescriptionUseCase.throwErrorCode = ClinicalErrorCode.PRESCRIPTION_STATUS_TRANSITION_NOT_ALLOWED;

        doctorMockMvc.perform(post("/api/v1/prescriptions/8101/issue")
                        .header("Authorization", "Bearer " + DOCTOR_TOKEN))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ClinicalErrorCode.PRESCRIPTION_STATUS_TRANSITION_NOT_ALLOWED.getCode()));
    }

    // --- Cancel tests ---

    @Test
    void cancel_WhenAuthenticatedDoctor_ReturnsCancelledPrescription() throws Exception {
        doctorMockMvc.perform(post("/api/v1/prescriptions/8101/cancel")
                        .header("Authorization", "Bearer " + DOCTOR_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.prescriptionOrderId").value(7101));

        assertEquals(2101L, cancelPrescriptionUseCase.lastCommand.doctorId());
        assertEquals(8101L, cancelPrescriptionUseCase.lastCommand.encounterId());
    }

    @Test
    void cancel_WhenUnauthenticated_ReturnsUnauthorized() throws Exception {
        unauthenticatedMockMvc.perform(post("/api/v1/prescriptions/8101/cancel"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void cancel_WhenPatient_ReturnsForbidden() throws Exception {
        patientMockMvc.perform(post("/api/v1/prescriptions/8101/cancel")
                        .header("Authorization", "Bearer " + PATIENT_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(2008));
    }

    @Test
    void cancel_WhenAlreadyCancelled_ReturnsConflict() throws Exception {
        cancelPrescriptionUseCase.throwErrorCode = ClinicalErrorCode.PRESCRIPTION_STATUS_TRANSITION_NOT_ALLOWED;

        doctorMockMvc.perform(post("/api/v1/prescriptions/8101/cancel")
                        .header("Authorization", "Bearer " + DOCTOR_TOKEN))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ClinicalErrorCode.PRESCRIPTION_STATUS_TRANSITION_NOT_ALLOWED.getCode()));
    }

    // --- Helper methods ---

    private void updatePrescriptionUseCaseReset() {
        updatePrescriptionItemsUseCase.lastCommand = null;
        updatePrescriptionItemsUseCase.throwErrorCode = null;
        issuePrescriptionUseCase.lastCommand = null;
        issuePrescriptionUseCase.throwErrorCode = null;
        cancelPrescriptionUseCase.lastCommand = null;
        cancelPrescriptionUseCase.throwErrorCode = null;
    }

    private MockMvc buildMockMvc(AuthenticatedUser user) {
        AccessTokenCodec accessTokenCodec = new StubAccessTokenCodec();
        AccessTokenBlocklistPort accessTokenBlocklistPort = new StubAccessTokenBlocklistPort();
        UserAuthenticationRepository userAuthenticationRepository = new StubUserAuthenticationRepository(user);
        JsonAuthenticationEntryPoint authenticationEntryPoint = new JsonAuthenticationEntryPoint(objectMapper);

        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(
                accessTokenCodec,
                accessTokenBlocklistPort,
                userAuthenticationRepository,
                authenticationEntryPoint,
                objectMapper,
                request -> false);

        PrescriptionController controller = new PrescriptionController(
                createPrescriptionUseCase,
                getPrescriptionDetailUseCase,
                updatePrescriptionItemsUseCase,
                issuePrescriptionUseCase,
                cancelPrescriptionUseCase,
                TestAuditSupport.auditApiSupport());
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(controller);
        proxyFactory.addAspect(new ScenarioAuthorizationAspect(
                buildAuthorizationDecisionService(),
                TestAuditSupport.auditApiSupport(),
                TestAuditSupport.emptyEncounterQueryRepository(),
                TestAuditSupport.emptyAdminPatientQueryRepository()));
        Object proxiedController = proxyFactory.getProxy();

        return MockMvcBuilders.standaloneSetup(proxiedController)
                .addFilter((OncePerRequestFilter) jwtAuthenticationFilter)
                .addFilter(new RequestCorrelationFilter())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    private AuthorizationDecisionService buildAuthorizationDecisionService() {
        return new AuthorizationDecisionService(
                List.of(
                        new EmrRecordResourceReferenceAssembler(),
                        new PrescriptionResourceReferenceAssembler()),
                List.of(
                        new EmrRecordResourceAccessResolver(encounterQueryRepository),
                        new PrescriptionResourceAccessResolver(encounterQueryRepository)));
    }

    // --- Stub use cases ---

    private static class StubCreatePrescriptionUseCase extends CreatePrescriptionUseCase {
        private CreatePrescriptionCommand lastCommand;
        private ClinicalErrorCode throwErrorCode;

        private StubCreatePrescriptionUseCase() {
            super(null, null, null, TestAuditSupport.auditTrailService());
        }

        @Override
        public PrescriptionOrder handle(CreatePrescriptionCommand command, AuditContext auditContext) {
            this.lastCommand = command;
            if (throwErrorCode != null) {
                throw new BizException(throwErrorCode);
            }
            return new PrescriptionOrder(
                    7101L,
                    "RX123456",
                    6102L,
                    command.encounterId(),
                    1001L,
                    command.doctorId(),
                    PrescriptionStatus.DRAFT,
                    List.of(new PrescriptionItem(
                            8101L, 0, "阿莫西林胶囊", "0.25g*24粒", "每次2粒", "每日3次", "5天", new BigDecimal("30"), "粒", "口服")),
                    0,
                    Instant.parse("2026-04-18T02:00:00Z"),
                    Instant.parse("2026-04-18T02:00:00Z"));
        }
    }

    private static class StubGetPrescriptionDetailUseCase extends GetPrescriptionDetailUseCase {
        private GetPrescriptionDetailQuery lastQuery;
        private ClinicalErrorCode throwErrorCode;

        private StubGetPrescriptionDetailUseCase() {
            super(null, null, TestAuditSupport.auditTrailService());
        }

        @Override
        public PrescriptionOrder handle(
                GetPrescriptionDetailQuery query,
                AuditContext auditContext,
                me.jianwen.mediask.domain.audit.model.DataAccessPurposeCode purposeCode) {
            this.lastQuery = query;
            if (throwErrorCode != null) {
                throw new BizException(throwErrorCode);
            }
            return new PrescriptionOrder(
                    7101L,
                    "RX123456",
                    6102L,
                    query.encounterId(),
                    1001L,
                    query.doctorId() == null ? 2101L : query.doctorId(),
                    PrescriptionStatus.DRAFT,
                    List.of(new PrescriptionItem(
                            8101L, 0, "阿莫西林胶囊", "0.25g*24粒", "每次2粒", "每日3次", "5天", new BigDecimal("30"), "粒", "口服")),
                    0,
                    Instant.parse("2026-04-18T02:00:00Z"),
                    Instant.parse("2026-04-18T02:00:00Z"));
        }
    }

    private static class StubUpdatePrescriptionItemsUseCase extends UpdatePrescriptionItemsUseCase {
        private UpdatePrescriptionItemsCommand lastCommand;
        private ClinicalErrorCode throwErrorCode;

        private StubUpdatePrescriptionItemsUseCase() {
            super(null, null, null, TestAuditSupport.auditTrailService());
        }

        @Override
        public PrescriptionOrder handle(UpdatePrescriptionItemsCommand command, AuditContext auditContext) {
            this.lastCommand = command;
            if (throwErrorCode != null) {
                throw new BizException(throwErrorCode);
            }
            List<PrescriptionItem> items = command.items().stream()
                    .map(item -> new PrescriptionItem(
                            9101L, item.sortOrder(), item.drugName(), item.drugSpecification(),
                            item.dosageText(), item.frequencyText(), item.durationText(),
                            item.quantity(), item.unit(), item.route()))
                    .toList();
            return new PrescriptionOrder(
                    7101L,
                    "RX123456",
                    6102L,
                    command.encounterId(),
                    1001L,
                    command.doctorId(),
                    PrescriptionStatus.DRAFT,
                    items,
                    1,
                    Instant.parse("2026-04-18T02:00:00Z"),
                    Instant.parse("2026-04-18T03:00:00Z"));
        }
    }

    private static class StubIssuePrescriptionUseCase extends IssuePrescriptionUseCase {
        private IssuePrescriptionCommand lastCommand;
        private ClinicalErrorCode throwErrorCode;

        private StubIssuePrescriptionUseCase() {
            super(null, null, null, TestAuditSupport.auditTrailService());
        }

        @Override
        public PrescriptionOrder handle(IssuePrescriptionCommand command, AuditContext auditContext) {
            this.lastCommand = command;
            if (throwErrorCode != null) {
                throw new BizException(throwErrorCode);
            }
            return new PrescriptionOrder(
                    7101L,
                    "RX123456",
                    6102L,
                    command.encounterId(),
                    1001L,
                    command.doctorId(),
                    PrescriptionStatus.ISSUED,
                    List.of(new PrescriptionItem(
                            8101L, 0, "阿莫西林胶囊", "0.25g*24粒", "每次2粒", "每日3次", "5天", new BigDecimal("30"), "粒", "口服")),
                    1,
                    Instant.parse("2026-04-18T02:00:00Z"),
                    Instant.parse("2026-04-18T03:00:00Z"));
        }
    }

    private static class StubCancelPrescriptionUseCase extends CancelPrescriptionUseCase {
        private CancelPrescriptionCommand lastCommand;
        private ClinicalErrorCode throwErrorCode;

        private StubCancelPrescriptionUseCase() {
            super(null, null, null, TestAuditSupport.auditTrailService());
        }

        @Override
        public PrescriptionOrder handle(CancelPrescriptionCommand command, AuditContext auditContext) {
            this.lastCommand = command;
            if (throwErrorCode != null) {
                throw new BizException(throwErrorCode);
            }
            return new PrescriptionOrder(
                    7101L,
                    "RX123456",
                    6102L,
                    command.encounterId(),
                    1001L,
                    command.doctorId(),
                    PrescriptionStatus.CANCELLED,
                    List.of(new PrescriptionItem(
                            8101L, 0, "阿莫西林胶囊", "0.25g*24粒", "每次2粒", "每日3次", "5天", new BigDecimal("30"), "粒", "口服")),
                    1,
                    Instant.parse("2026-04-18T02:00:00Z"),
                    Instant.parse("2026-04-18T03:00:00Z"));
        }
    }

    // --- Stub repositories ---

    private static class StubEmrRecordQueryRepository implements EmrRecordQueryRepository {
        private Long recordId = 7101L;
        private EmrRecordAccess access = new EmrRecordAccess(7101L, 1001L, 3101L);

        void reset() {
            this.recordId = 7101L;
            this.access = new EmrRecordAccess(7101L, 1001L, 3101L);
        }

        @Override
        public Optional<me.jianwen.mediask.domain.clinical.model.EmrRecord> findByEncounterId(Long encounterId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<EmrRecordListItem> listByPatientUserId(Long patientUserId, Long excludeEncounterId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Long> findRecordIdByEncounterId(Long encounterId) {
            if (encounterId == 8101L && recordId != null) {
                return Optional.of(recordId);
            }
            return Optional.empty();
        }

        @Override
        public Optional<EmrRecordAccess> findAccessByRecordId(Long recordId) {
            if (access != null && access.recordId().equals(recordId)) {
                return Optional.of(access);
            }
            return Optional.empty();
        }
    }

    private static class StubEncounterQueryRepository implements EncounterQueryRepository {

        private Optional<EncounterDetail> encounter = Optional.of(new EncounterDetail(
                8101L,
                6101L,
                2101L,
                new EncounterPatientSummary(
                        1001L,
                        "张患者",
                        "FEMALE",
                        3101L,
                        "神经内科",
                        java.time.LocalDate.parse("2026-04-18"),
                        "MORNING",
                        VisitEncounterStatus.SCHEDULED,
                        null,
                        null,
                        null)));

        private void reset() {
            this.encounter = Optional.of(new EncounterDetail(
                    8101L,
                    6101L,
                    2101L,
                    new EncounterPatientSummary(
                            1001L,
                            "张患者",
                            "FEMALE",
                            3101L,
                            "神经内科",
                            java.time.LocalDate.parse("2026-04-18"),
                            "MORNING",
                            VisitEncounterStatus.SCHEDULED,
                            null,
                            null,
                            null)));
        }

        @Override
        public List<me.jianwen.mediask.domain.clinical.model.EncounterListItem> listByDoctorId(
                Long doctorId, VisitEncounterStatus status) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<EncounterDetail> findDetailByEncounterId(Long encounterId) {
            if (encounter.isPresent() && encounter.get().encounterId().equals(encounterId)) {
                return encounter;
            }
            return Optional.empty();
        }
    }

    // --- Stub auth ---

    private static class StubAccessTokenCodec implements AccessTokenCodec {
        @Override
        public AccessToken issueAccessToken(AuthenticatedUser authenticatedUser, String sessionId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AccessTokenClaims parseAccessToken(String accessToken) {
            if (DOCTOR_TOKEN.equals(accessToken)) {
                return new AccessTokenClaims(2001L, "doctor-token-id", "doctor-session", Instant.now().plusSeconds(3600));
            }
            if (PATIENT_TOKEN.equals(accessToken)) {
                return new AccessTokenClaims(1001L, "patient-token-id", "patient-session", Instant.now().plusSeconds(3600));
            }
            throw new IllegalArgumentException("unsupported access token");
        }
    }

    private static class StubAccessTokenBlocklistPort implements AccessTokenBlocklistPort {
        @Override
        public void block(String tokenId, Instant expiresAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isBlocked(String tokenId) {
            return false;
        }
    }

    private static class StubUserAuthenticationRepository implements UserAuthenticationRepository {
        private final AuthenticatedUser user;

        private StubUserAuthenticationRepository(AuthenticatedUser user) {
            this.user = user;
        }

        @Override
        public java.util.Optional<me.jianwen.mediask.domain.user.model.LoginAccount> findLoginAccountByUsername(String username) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.Optional<AuthenticatedUser> findAuthenticatedUserById(Long userId) {
            if (user == null || !user.userId().equals(userId)) {
                return java.util.Optional.empty();
            }
            return java.util.Optional.of(user);
        }

        @Override
        public void updateLastLoginAt(Long userId) {
            throw new UnsupportedOperationException();
        }
    }
}
