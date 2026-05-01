package me.jianwen.mediask.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import me.jianwen.mediask.api.advice.ResultResponseBodyAdvice;
import me.jianwen.mediask.api.dto.AiTriageQueryRequest;
import me.jianwen.mediask.api.exception.GlobalExceptionHandler;
import me.jianwen.mediask.api.security.AuthenticatedUserPrincipal;
import me.jianwen.mediask.application.ai.command.SubmitAiTriageQueryCommand;
import me.jianwen.mediask.application.ai.usecase.StreamAiTriageQueryUseCase;
import me.jianwen.mediask.application.ai.usecase.SubmitAiTriageQueryUseCase;
import me.jianwen.mediask.domain.ai.model.AiTriageCitation;
import me.jianwen.mediask.domain.ai.model.AiTriageGatewayContext;
import me.jianwen.mediask.domain.ai.model.AiTriageQuery;
import me.jianwen.mediask.domain.ai.model.AiTriageQueryResponse;
import me.jianwen.mediask.domain.ai.model.AiTriageRecommendedDepartment;
import me.jianwen.mediask.domain.ai.model.AiTriageResult;
import me.jianwen.mediask.domain.ai.port.AiTriageGatewayPort;
import me.jianwen.mediask.domain.ai.port.AiTriageResultSnapshotRepository;
import me.jianwen.mediask.domain.triage.model.CatalogVersion;
import me.jianwen.mediask.domain.triage.model.DepartmentCandidate;
import me.jianwen.mediask.domain.triage.model.TriageCatalog;
import me.jianwen.mediask.domain.triage.port.TriageCatalogPublishPort;
import me.jianwen.mediask.domain.user.model.AuthenticatedUser;
import me.jianwen.mediask.domain.user.model.RoleCode;
import me.jianwen.mediask.domain.user.model.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AiTriageControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private StubSubmitUseCase submitUseCase;
    private MockMvc patientMockMvc;
    private MockMvc doctorMockMvc;

    @BeforeEach
    void setUp() {
        submitUseCase = new StubSubmitUseCase();
        patientMockMvc = buildMockMvc(patientUser(), submitUseCase);
        doctorMockMvc = buildMockMvc(doctorUser(), new StubSubmitUseCase());
    }

    @Test
    void query_WhenPatientAuthenticated_ReturnsStructuredResponse() throws Exception {
        patientMockMvc.perform(post("/api/v1/ai/triage/query")
                        .header("X-Request-Id", "req-triage-1")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "hospitalScope": "default",
                                  "userMessage": "头痛两天"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.requestId").value("req-triage-1"))
                .andExpect(jsonPath("$.data.requestId").value("req-triage-1"))
                .andExpect(jsonPath("$.data.sessionId").value("session-1"))
                .andExpect(jsonPath("$.data.queryRunId").value("query-1"))
                .andExpect(jsonPath("$.data.triageResult.triageStage").value("READY"))
                .andExpect(jsonPath("$.data.triageResult.recommendedDepartments[0].departmentId").value("3101"))
                .andExpect(jsonPath("$.data.triageResult.citations[0].chunkId").value("chunk-1"));

        assertEquals(2201L, submitUseCase.lastCommand.patientUserId());
        assertEquals("default", submitUseCase.lastCommand.hospitalScope());
        assertEquals("头痛两天", submitUseCase.lastCommand.userMessage());
        assertEquals("req-triage-1", submitUseCase.lastCommand.requestId());
    }

    @Test
    void query_WhenDoctorAuthenticated_ReturnsRoleMismatch() throws Exception {
        doctorMockMvc.perform(post("/api/v1/ai/triage/query")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "userMessage": "头痛两天"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(2008));
    }

    @Test
    void streamQuery_WhenPatientAuthenticated_ConvertsSnakeCaseToCamelCase() throws Exception {
        AiTriageController controller = new AiTriageController(
                submitUseCase,
                new StreamAiTriageQueryUseCase(new StreamingGatewayPort(), submitUseCase),
                objectMapper);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        controller.streamQuery(
                        new AiTriageQueryRequest(null, "default", "头痛两天"),
                        AuthenticatedUserPrincipal.from(patientUser()))
                .getBody()
                .writeTo(outputStream);

        String body = outputStream.toString(StandardCharsets.UTF_8);
        assertTrue(body.contains("event: start"));
        assertTrue(body.contains("\"requestId\":\"req-1\""));
        assertTrue(body.contains("\"textDelta\":\"根据你提供的信息\""));
        assertTrue(body.contains("\"triageResult\""));
        assertTrue(body.contains("\"triageStage\":\"READY\""));
        assertTrue(body.contains("\"queryRunId\":\"query-1\""));
        assertTrue(!body.contains("\"request_id\""));
        assertTrue(!body.contains("\"text_delta\""));
        assertTrue(!body.contains("\"triage_stage\""));
    }

    private MockMvc buildMockMvc(AuthenticatedUser user, StubSubmitUseCase submitUseCase) {
        AiTriageController controller = new AiTriageController(
                submitUseCase,
                new StreamAiTriageQueryUseCase(new StreamingGatewayPort(), submitUseCase),
                objectMapper);
        Filter testAuthenticationFilter = (request, response, chain) -> {
            try {
                AuthenticatedUserPrincipal principal = AuthenticatedUserPrincipal.from(user);
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(principal, "n/a", principal.authorities()));
                chain.doFilter(request, response);
            } finally {
                SecurityContextHolder.clearContext();
            }
        };
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(), new ResultResponseBodyAdvice())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .addFilters(testAuthenticationFilter)
                .build();
    }

    private AuthenticatedUser patientUser() {
        return new AuthenticatedUser(
                2201L,
                "patient_li",
                "李患者",
                UserType.PATIENT,
                Set.of(RoleCode.PATIENT),
                Set.of(),
                Set.of(),
                3301L,
                null,
                null);
    }

    private AuthenticatedUser doctorUser() {
        return new AuthenticatedUser(
                2202L,
                "doctor_zhang",
                "张医生",
                UserType.DOCTOR,
                Set.of(RoleCode.DOCTOR),
                Set.of(),
                Set.of(),
                null,
                2101L,
                3101L);
    }

    private static final class StubSubmitUseCase extends SubmitAiTriageQueryUseCase {

        private SubmitAiTriageQueryCommand lastCommand;

        private StubSubmitUseCase() {
            super(new NoopGatewayPort(), new NoopSnapshotRepository(), new NoopCatalogPublishPort());
        }

        @Override
        public AiTriageQueryResponse handle(SubmitAiTriageQueryCommand command) {
            this.lastCommand = command;
            return new AiTriageQueryResponse(
                    "req-triage-1",
                    "session-1",
                    "turn-1",
                    "query-1",
                    new AiTriageResult(
                            "READY",
                            "SUFFICIENT_INFO",
                            "VIEW_TRIAGE_RESULT",
                            "low",
                            "头痛两天",
                            List.of(),
                            List.of(new AiTriageRecommendedDepartment(3101L, "神经内科", 1, "头痛优先神经内科")),
                            "建议门诊就诊",
                            null,
                            "deptcat-v20260501-01",
                            List.of(new AiTriageCitation(1, "chunk-1", "头痛优先神经内科"))));
        }

        @Override
        public void persistIfFinalized(String hospitalScope, AiTriageQueryResponse response) {
        }
    }

    private static final class NoopGatewayPort implements AiTriageGatewayPort {

        @Override
        public AiTriageQueryResponse query(AiTriageGatewayContext context, AiTriageQuery query) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void streamQuery(AiTriageGatewayContext context, AiTriageQuery query, StreamEventHandler handler) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class StreamingGatewayPort implements AiTriageGatewayPort {

        @Override
        public AiTriageQueryResponse query(AiTriageGatewayContext context, AiTriageQuery query) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void streamQuery(AiTriageGatewayContext context, AiTriageQuery query, StreamEventHandler handler) {
            handler.onEvent(new StreamEvent(
                    "start",
                    "{\"request_id\":\"req-1\",\"session_id\":\"session-1\",\"turn_id\":\"turn-1\",\"query_run_id\":\"query-1\"}",
                    null));
            handler.onEvent(new StreamEvent(
                    "delta",
                    "{\"text_delta\":\"根据你提供的信息\"}",
                    null));
            handler.onEvent(new StreamEvent(
                    "final",
                    "{\"request_id\":\"req-1\",\"session_id\":\"session-1\",\"turn_id\":\"turn-1\",\"query_run_id\":\"query-1\",\"triage_result\":{\"triage_stage\":\"READY\",\"triage_completion_reason\":\"SUFFICIENT_INFO\",\"next_action\":\"VIEW_TRIAGE_RESULT\",\"risk_level\":\"low\",\"chief_complaint_summary\":\"头痛\",\"recommended_departments\":[{\"department_id\":3101,\"department_name\":\"神经内科\",\"priority\":1,\"reason\":\"头痛优先神经内科\"}],\"care_advice\":\"建议门诊就诊\",\"catalog_version\":\"deptcat-v20260501-01\",\"citations\":[]}}",
                    new AiTriageQueryResponse(
                            "req-1",
                            "session-1",
                            "turn-1",
                            "query-1",
                            new AiTriageResult(
                                    "READY",
                                    "SUFFICIENT_INFO",
                                    "VIEW_TRIAGE_RESULT",
                                    "low",
                                    "头痛",
                                    List.of(),
                                    List.of(new AiTriageRecommendedDepartment(3101L, "神经内科", 1, "头痛优先神经内科")),
                                    "建议门诊就诊",
                                    null,
                                    "deptcat-v20260501-01",
                                    List.of()))));
            handler.onEvent(new StreamEvent("done", "{}", null));
        }
    }

    private static final class NoopSnapshotRepository implements AiTriageResultSnapshotRepository {

        @Override
        public void save(me.jianwen.mediask.domain.ai.model.AiTriageResultSnapshot snapshot) {
        }
    }

    private static final class NoopCatalogPublishPort implements TriageCatalogPublishPort {

        @Override
        public void publish(me.jianwen.mediask.domain.triage.model.TriageCatalog catalog) {
        }

        @Override
        public java.util.Optional<me.jianwen.mediask.domain.triage.model.TriageCatalog> findActiveCatalog(String hospitalScope) {
            return java.util.Optional.empty();
        }

        @Override
        public java.util.Optional<TriageCatalog> findCatalogByVersion(String hospitalScope, CatalogVersion version) {
            return java.util.Optional.of(new TriageCatalog(
                    "default",
                    version,
                    OffsetDateTime.parse("2026-05-01T10:00:00+08:00"),
                    List.of(new DepartmentCandidate(3101L, "神经内科", "头痛头晕", List.of(), 10))));
        }

        @Override
        public java.util.Optional<CatalogVersion> findActiveVersion(String hospitalScope) {
            return java.util.Optional.empty();
        }

        @Override
        public CatalogVersion nextVersion(String hospitalScope) {
            throw new UnsupportedOperationException();
        }
    }
}
