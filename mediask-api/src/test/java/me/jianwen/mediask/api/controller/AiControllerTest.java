package me.jianwen.mediask.api.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import java.time.OffsetDateTime;
import java.util.List;
import me.jianwen.mediask.api.advice.ResultResponseBodyAdvice;
import me.jianwen.mediask.api.exception.GlobalExceptionHandler;
import me.jianwen.mediask.api.filter.RequestCorrelationFilter;
import me.jianwen.mediask.api.security.AuthenticatedUserPrincipal;
import me.jianwen.mediask.application.ai.query.GetAiSessionDetailQuery;
import me.jianwen.mediask.application.ai.query.GetAiSessionRegistrationHandoffQuery;
import me.jianwen.mediask.application.ai.query.GetAiSessionTriageResultQuery;
import me.jianwen.mediask.application.ai.query.ListAiSessionsQuery;
import me.jianwen.mediask.application.ai.usecase.AiRegistrationHandoffSupport;
import me.jianwen.mediask.application.ai.usecase.ChatAiResult;
import me.jianwen.mediask.application.ai.usecase.ChatAiUseCase;
import me.jianwen.mediask.application.ai.usecase.GetAiSessionDetailUseCase;
import me.jianwen.mediask.application.ai.usecase.GetAiSessionRegistrationHandoffUseCase;
import me.jianwen.mediask.application.ai.usecase.GetAiSessionTriageResultUseCase;
import me.jianwen.mediask.application.ai.usecase.ListAiSessionsUseCase;
import me.jianwen.mediask.common.request.RequestConstants;
import me.jianwen.mediask.domain.ai.model.AiChatReply;
import me.jianwen.mediask.domain.ai.model.AiCitation;
import me.jianwen.mediask.domain.ai.model.AiContentRole;
import me.jianwen.mediask.domain.ai.model.AiExecutionMetadata;
import me.jianwen.mediask.domain.ai.model.AiSceneType;
import me.jianwen.mediask.domain.ai.model.AiSessionDetail;
import me.jianwen.mediask.domain.ai.model.AiSessionListItem;
import me.jianwen.mediask.domain.ai.model.AiSessionMessage;
import me.jianwen.mediask.domain.ai.model.AiSessionRegistrationHandoffView;
import me.jianwen.mediask.domain.ai.model.AiSessionStatus;
import me.jianwen.mediask.domain.ai.model.AiSessionTriageResultView;
import me.jianwen.mediask.domain.ai.model.AiSessionTurnDetail;
import me.jianwen.mediask.domain.ai.model.AiTriageCompletionReason;
import me.jianwen.mediask.domain.ai.model.AiTriageResultStatus;
import me.jianwen.mediask.domain.ai.model.AiTriageStage;
import me.jianwen.mediask.domain.ai.model.AiTurnStatus;
import me.jianwen.mediask.domain.ai.model.GuardrailAction;
import me.jianwen.mediask.domain.ai.model.RecommendedDepartment;
import me.jianwen.mediask.domain.ai.model.RiskLevel;
import me.jianwen.mediask.domain.user.model.DataScopeRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AiControllerTest {

    private static final String PATIENT_TOKEN = "patient-token";
    private static final String DOCTOR_TOKEN = "doctor-token";

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private StubChatAiUseCase chatAiUseCase;
    private StubListAiSessionsUseCase listAiSessionsUseCase;
    private StubGetAiSessionDetailUseCase getAiSessionDetailUseCase;
    private StubGetAiSessionTriageResultUseCase getAiSessionTriageResultUseCase;
    private StubGetAiSessionRegistrationHandoffUseCase getAiSessionRegistrationHandoffUseCase;

    @BeforeEach
    void setUp() {
        chatAiUseCase = new StubChatAiUseCase();
        listAiSessionsUseCase = new StubListAiSessionsUseCase();
        getAiSessionDetailUseCase = new StubGetAiSessionDetailUseCase();
        getAiSessionTriageResultUseCase = new StubGetAiSessionTriageResultUseCase();
        getAiSessionRegistrationHandoffUseCase = new StubGetAiSessionRegistrationHandoffUseCase();
        AiController controller = new AiController(
                chatAiUseCase,
                listAiSessionsUseCase,
                getAiSessionDetailUseCase,
                getAiSessionTriageResultUseCase,
                getAiSessionRegistrationHandoffUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(), new ResultResponseBodyAdvice())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .addFilters(new RequestCorrelationFilter(), new TestAuthenticationFilter(), new SecurityContextCleanupFilter())
                .setMessageConverters(
                        new StringHttpMessageConverter(),
                        new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void chat_WhenSuccessful_ReturnJsonResult() throws Exception {
        chatAiUseCase.result = new ChatAiResult(
                90001L,
                90011L,
                "建议尽快线下就医",
                new AiChatReply(
                        "建议尽快线下就医",
                        AiTriageStage.READY,
                        AiTriageCompletionReason.SUFFICIENT_INFO,
                        "头痛三天",
                        RiskLevel.MEDIUM,
                        GuardrailAction.CAUTION,
                        List.of(),
                        List.of(new RecommendedDepartment(101L, "神经内科", 1, "头痛持续")),
                        "建议挂号",
                        List.of(new AiCitation(7001L, 1, 0.82D, "引用片段")),
                        AiExecutionMetadata.empty()));

        mockMvc.perform(post("/api/v1/ai/chat")
                        .header("Authorization", "Bearer " + PATIENT_TOKEN)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "头痛三天",
                                  "sceneType": "PRE_CONSULTATION",
                                  "useStream": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().exists(RequestConstants.REQUEST_ID_HEADER))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.sessionId").value("90001"))
                .andExpect(jsonPath("$.data.turnId").value("90011"))
                .andExpect(jsonPath("$.data.answer").value("建议尽快线下就医"))
                .andExpect(jsonPath("$.data.triageResult.triageStage").value("READY"))
                .andExpect(jsonPath("$.data.triageResult.nextAction").value("VIEW_TRIAGE_RESULT"));
    }

    @Test
    void chat_WhenCollecting_ReturnFollowUpQuestions() throws Exception {
        chatAiUseCase.result = new ChatAiResult(
                90001L,
                90011L,
                "还需要补充两个问题",
                new AiChatReply(
                        "还需要补充两个问题",
                        AiTriageStage.COLLECTING,
                        null,
                        "头痛三天",
                        RiskLevel.LOW,
                        GuardrailAction.ALLOW,
                        List.of("是否伴随恶心？", "体温最高多少度？"),
                        List.of(),
                        null,
                        List.of(),
                        AiExecutionMetadata.empty()));

        mockMvc.perform(post("/api/v1/ai/chat")
                        .header("Authorization", "Bearer " + PATIENT_TOKEN)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "头痛三天",
                                  "sceneType": "PRE_CONSULTATION",
                                  "useStream": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.triageResult.triageStage").value("COLLECTING"))
                .andExpect(jsonPath("$.data.triageResult.nextAction").value("CONTINUE_TRIAGE"))
                .andExpect(jsonPath("$.data.triageResult.followUpQuestions[0]").value("是否伴随恶心？"));
    }

    @Test
    void chat_WhenUseStreamTrue_ReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/ai/chat")
                        .header("Authorization", "Bearer " + PATIENT_TOKEN)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "头痛三天",
                                  "sceneType": "PRE_CONSULTATION",
                                  "useStream": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1002))
                .andExpect(jsonPath("$.msg").value("useStream must be false for /api/v1/ai/chat"));
    }

    @Test
    void getSessions_WhenSuccessful_ReturnJsonResult() throws Exception {
        listAiSessionsUseCase.result = List.of(
                new AiSessionListItem(
                        90002L,
                        101L,
                        AiSceneType.PRE_CONSULTATION,
                        AiSessionStatus.CLOSED,
                        "复诊头痛",
                        "复诊头痛已缓解",
                        OffsetDateTime.parse("2026-04-13T09:30:00+08:00"),
                        OffsetDateTime.parse("2026-04-13T09:35:00+08:00")),
                new AiSessionListItem(
                        90001L,
                        101L,
                        AiSceneType.PRE_CONSULTATION,
                        AiSessionStatus.ACTIVE,
                        "头痛三天",
                        "头痛三天伴低烧",
                        OffsetDateTime.parse("2026-04-12T09:30:00+08:00"),
                        null));

        mockMvc.perform(get("/api/v1/ai/sessions").header("Authorization", "Bearer " + PATIENT_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].sessionId").value("90002"))
                .andExpect(jsonPath("$.data.items[1].sessionId").value("90001"));
    }

    @Test
    void getSession_WhenSuccessful_ReturnJsonResult() throws Exception {
        getAiSessionDetailUseCase.result = new AiSessionDetail(
                90001L,
                1L,
                101L,
                AiSceneType.PRE_CONSULTATION,
                AiSessionStatus.ACTIVE,
                "头痛三天",
                "头痛三天伴低烧",
                OffsetDateTime.parse("2026-04-12T09:30:00+08:00"),
                null,
                List.of(new AiSessionTurnDetail(
                        90011L,
                        1,
                        AiTurnStatus.COMPLETED,
                        OffsetDateTime.parse("2026-04-12T09:30:00+08:00"),
                        OffsetDateTime.parse("2026-04-12T09:31:00+08:00"),
                        null,
                        null,
                        List.of(
                                new AiSessionMessage(
                                        AiContentRole.USER,
                                        "头痛三天",
                                        OffsetDateTime.parse("2026-04-12T09:30:00+08:00")),
                                new AiSessionMessage(
                                        AiContentRole.ASSISTANT,
                                        "建议挂神经内科",
                                        OffsetDateTime.parse("2026-04-12T09:31:00+08:00"))))));

        mockMvc.perform(get("/api/v1/ai/sessions/90001").header("Authorization", "Bearer " + PATIENT_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.turns[0].messages[0].content").value("头痛三天"))
                .andExpect(jsonPath("$.data.turns[0].messages[1].content").value("建议挂神经内科"));
    }

    @Test
    void getTriageResult_WhenSuccessful_ReturnJsonResult() throws Exception {
        getAiSessionTriageResultUseCase.result = new AiSessionTriageResultView(
                90001L,
                1L,
                AiTriageResultStatus.UPDATING,
                AiTriageStage.READY,
                90011L,
                OffsetDateTime.parse("2026-04-12T09:31:00+08:00"),
                true,
                2,
                "头痛三天",
                RiskLevel.MEDIUM,
                GuardrailAction.CAUTION,
                List.of(new RecommendedDepartment(101L, "神经内科", 1, "头痛持续")),
                "建议挂号",
                List.of(new AiCitation(7001L, 1, 0.82D, "引用片段")));

        mockMvc.perform(get("/api/v1/ai/sessions/90001/triage-result").header("Authorization", "Bearer " + PATIENT_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value("90001"))
                .andExpect(jsonPath("$.data.resultStatus").value("UPDATING"))
                .andExpect(jsonPath("$.data.triageStage").value("READY"))
                .andExpect(jsonPath("$.data.nextAction").value("VIEW_TRIAGE_RESULT"))
                .andExpect(jsonPath("$.data.activeCycleTurnNo").value(2))
                .andExpect(jsonPath("$.data.citations[0].chunkId").value("7001"));
    }

    @Test
    void getRegistrationHandoff_WhenSuccessful_ReturnJsonResult() throws Exception {
        getAiSessionRegistrationHandoffUseCase.result = new AiSessionRegistrationHandoffView(
                90001L,
                1L,
                101L,
                "神经内科",
                "头痛三天",
                "OUTPATIENT",
                null,
                new AiSessionRegistrationHandoffView.RegistrationQuery(
                        101L,
                        java.time.LocalDate.parse("2026-04-15"),
                        java.time.LocalDate.parse("2026-04-21")));

        mockMvc.perform(post("/api/v1/ai/sessions/90001/registration-handoff")
                        .header("Authorization", "Bearer " + PATIENT_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recommendedDepartmentId").value("101"))
                .andExpect(jsonPath("$.data.registrationQuery.dateTo").value("2026-04-21"));
    }

    @Test
    void getRegistrationHandoff_WhenEmergencyOffline_ReturnBlockedResponse() throws Exception {
        getAiSessionRegistrationHandoffUseCase.result = new AiSessionRegistrationHandoffView(
                90001L, 1L, null, null, "胸痛加重", null, "EMERGENCY_OFFLINE", null);

        mockMvc.perform(post("/api/v1/ai/sessions/90001/registration-handoff")
                        .header("Authorization", "Bearer " + PATIENT_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.blockedReason").value("EMERGENCY_OFFLINE"))
                .andExpect(jsonPath("$.data.registrationQuery").doesNotExist());
    }

    @Test
    void getSessions_WhenUnauthenticated_ReturnUnauthorizedJson() throws Exception {
        mockMvc.perform(get("/api/v1/ai/sessions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void getSessions_WhenAuthenticatedUserIsNotPatient_ReturnForbiddenJson() throws Exception {
        mockMvc.perform(get("/api/v1/ai/sessions").header("Authorization", "Bearer " + DOCTOR_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(2008));
    }

    private AuthenticatedUserPrincipal patientPrincipal() {
        return new AuthenticatedUserPrincipal(
                1L,
                "patient1",
                "Patient One",
                "PATIENT",
                List.of("PATIENT"),
                List.of(),
                List.<DataScopeRule>of(),
                1001L,
                null,
                null,
                List.of());
    }

    private AuthenticatedUserPrincipal nonPatientPrincipal() {
        return new AuthenticatedUserPrincipal(
                2L,
                "doctor1",
                "Doctor One",
                "DOCTOR",
                List.of("DOCTOR"),
                List.of(),
                List.<DataScopeRule>of(),
                null,
                2001L,
                301L,
                List.of());
    }

    private final class TestAuthenticationFilter implements Filter {
        @Override
        public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response,
                jakarta.servlet.FilterChain chain)
                throws java.io.IOException, jakarta.servlet.ServletException {
            String authorization = ((jakarta.servlet.http.HttpServletRequest) request).getHeader("Authorization");
            if (("Bearer " + PATIENT_TOKEN).equals(authorization)) {
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(patientPrincipal(), null, patientPrincipal().authorities()));
            } else if (("Bearer " + DOCTOR_TOKEN).equals(authorization)) {
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                        nonPatientPrincipal(), null, nonPatientPrincipal().authorities()));
            }
            chain.doFilter(request, response);
        }
    }

    private static final class SecurityContextCleanupFilter implements Filter {
        @Override
        public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response,
                jakarta.servlet.FilterChain chain)
                throws java.io.IOException, jakarta.servlet.ServletException {
            try {
                chain.doFilter(request, response);
            } finally {
                SecurityContextHolder.clearContext();
            }
        }
    }

    private static final class StubChatAiUseCase extends ChatAiUseCase {
        private ChatAiResult result;

        private StubChatAiUseCase() {
            super(
                    invocation -> { throw new UnsupportedOperationException(); },
                    new NoopAiSessionRepository(),
                    new NoopAiTurnRepository(),
                    content -> {},
                    new NoopAiModelRunRepository(),
                    event -> {},
                    new NoopAiContentEncryptorPort(),
                    hospitalScope -> new me.jianwen.mediask.domain.ai.model.TriageDepartmentCatalog(
                            hospitalScope, "deptcat-test", List.of()));
        }

        @Override
        public ChatAiResult handle(me.jianwen.mediask.application.ai.command.ChatAiCommand command) {
            return result;
        }
    }

    private static final class StubListAiSessionsUseCase extends ListAiSessionsUseCase {
        private List<AiSessionListItem> result = List.of();

        private StubListAiSessionsUseCase() {
            super(new NoopAiSessionQueryRepository());
        }

        @Override
        public List<AiSessionListItem> handle(ListAiSessionsQuery query) {
            return result;
        }
    }

    private static final class StubGetAiSessionDetailUseCase extends GetAiSessionDetailUseCase {
        private AiSessionDetail result;

        private StubGetAiSessionDetailUseCase() {
            super(new NoopAiSessionQueryRepository(), new NoopAiContentEncryptorPort());
        }

        @Override
        public AiSessionDetail handle(GetAiSessionDetailQuery query) {
            return result;
        }
    }

    private static final class StubGetAiSessionTriageResultUseCase extends GetAiSessionTriageResultUseCase {
        private AiSessionTriageResultView result;

        private StubGetAiSessionTriageResultUseCase() {
            super(new NoopAiSessionQueryRepository());
        }

        @Override
        public AiSessionTriageResultView handle(GetAiSessionTriageResultQuery query) {
            return result;
        }
    }

    private static final class StubGetAiSessionRegistrationHandoffUseCase extends GetAiSessionRegistrationHandoffUseCase {
        private AiSessionRegistrationHandoffView result;

        private StubGetAiSessionRegistrationHandoffUseCase() {
            super(new AiRegistrationHandoffSupport(new NoopAiSessionQueryRepository()), java.time.Clock.systemUTC());
        }

        @Override
        public AiSessionRegistrationHandoffView handle(GetAiSessionRegistrationHandoffQuery query) {
            return result;
        }
    }

    private static final class NoopAiSessionRepository implements me.jianwen.mediask.domain.ai.port.AiSessionRepository {
        @Override
        public void save(me.jianwen.mediask.domain.ai.model.AiSession aiSession) {}

        @Override
        public java.util.Optional<me.jianwen.mediask.domain.ai.model.AiSession> findById(Long sessionId) {
            return java.util.Optional.empty();
        }

        @Override
        public void update(me.jianwen.mediask.domain.ai.model.AiSession aiSession) {}
    }

    private static final class NoopAiTurnRepository implements me.jianwen.mediask.domain.ai.port.AiTurnRepository {
        @Override
        public void save(me.jianwen.mediask.domain.ai.model.AiTurn aiTurn) {}

        @Override
        public int findMaxTurnNoBySessionId(Long sessionId) {
            return 0;
        }

        @Override
        public void update(me.jianwen.mediask.domain.ai.model.AiTurn aiTurn) {}
    }

    private static final class NoopAiModelRunRepository implements me.jianwen.mediask.domain.ai.port.AiModelRunRepository {
        @Override
        public void save(me.jianwen.mediask.domain.ai.model.AiModelRun aiModelRun) {}

        @Override
        public void update(me.jianwen.mediask.domain.ai.model.AiModelRun aiModelRun) {}

        @Override
        public Integer findLatestFinalizedTurnNoBySessionId(Long sessionId) {
            return null;
        }
    }

    private static final class NoopAiSessionQueryRepository implements me.jianwen.mediask.domain.ai.port.AiSessionQueryRepository {
        @Override
        public List<AiSessionListItem> listSessionsByPatientUserId(Long patientUserId) {
            return List.of();
        }

        @Override
        public java.util.Optional<AiSessionDetail> findSessionDetailById(Long sessionId) {
            return java.util.Optional.empty();
        }

        @Override
        public java.util.Optional<AiSessionTriageResultView> findLatestTriageResultBySessionId(Long sessionId) {
            return java.util.Optional.empty();
        }

        @Override
        public java.util.Optional<AiTriageStage> findLatestTriageStageBySessionId(Long sessionId) {
            return java.util.Optional.empty();
        }

        @Override
        public boolean hasAccessibleTriageSession(Long patientUserId, Long sessionId) {
            return false;
        }
    }

    private static final class NoopAiContentEncryptorPort implements me.jianwen.mediask.domain.ai.port.AiContentEncryptorPort {
        @Override
        public String encrypt(String plainText) {
            return plainText;
        }

        @Override
        public String decrypt(String encryptedText) {
            return encryptedText;
        }
    }
}
