package me.jianwen.mediask.api.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;
import me.jianwen.mediask.api.advice.ResultResponseBodyAdvice;
import me.jianwen.mediask.api.exception.GlobalExceptionHandler;
import me.jianwen.mediask.api.filter.RequestCorrelationFilter;
import me.jianwen.mediask.api.security.AuthenticatedUserPrincipal;
import me.jianwen.mediask.api.dto.AiChatStreamRequest;
import me.jianwen.mediask.application.ai.usecase.ChatAiResult;
import me.jianwen.mediask.application.ai.usecase.ChatAiUseCase;
import me.jianwen.mediask.application.ai.usecase.StreamAiChatUseCase;
import me.jianwen.mediask.common.request.RequestConstants;
import me.jianwen.mediask.domain.user.model.DataScopeRule;
import me.jianwen.mediask.domain.ai.exception.AiErrorCode;
import me.jianwen.mediask.domain.ai.model.AiChatStreamEvent;
import me.jianwen.mediask.domain.ai.model.AiChatTriageResult;
import me.jianwen.mediask.domain.ai.model.AiCitation;
import me.jianwen.mediask.domain.ai.model.AiExecutionMetadata;
import me.jianwen.mediask.domain.ai.model.GuardrailAction;
import me.jianwen.mediask.domain.ai.model.RecommendedDepartment;
import me.jianwen.mediask.domain.ai.model.RiskLevel;
import me.jianwen.mediask.domain.ai.port.AiChatPort;
import me.jianwen.mediask.domain.ai.port.AiContentEncryptorPort;
import me.jianwen.mediask.domain.ai.port.AiGuardrailEventRepository;
import me.jianwen.mediask.domain.ai.port.AiModelRunRepository;
import me.jianwen.mediask.domain.ai.port.AiSessionRepository;
import me.jianwen.mediask.domain.ai.port.AiChatStreamPort;
import me.jianwen.mediask.domain.ai.port.AiTurnContentRepository;
import me.jianwen.mediask.domain.ai.port.AiTurnRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class AiControllerTest {

    private static final String PATIENT_TOKEN = "patient-token";
    private static final String DOCTOR_TOKEN = "doctor-token";

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private StubAiChatStreamPort aiChatStreamPort;
    private StubChatAiUseCase chatAiUseCase;

    @BeforeEach
    void setUp() {
        aiChatStreamPort = new StubAiChatStreamPort();
        chatAiUseCase = new StubChatAiUseCase();
        AiController controller = new AiController(
                chatAiUseCase,
                streamAiChatUseCase(),
                new SyncTaskExecutor());
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
    void stream_WhenUpstreamReturnsMessageMetaAndEnd_ReturnSseContract() throws Exception {
        aiChatStreamPort.eventPublisher = consumer -> {
            consumer.accept(new AiChatStreamEvent.Message("answer-part-1"));
            consumer.accept(new AiChatStreamEvent.Meta(successTriageResult()));
            consumer.accept(new AiChatStreamEvent.End());
        };

        MvcResult mvcResult = mockMvc.perform(post("/api/v1/ai/chat/stream")
                        .header("Authorization", "Bearer " + PATIENT_TOKEN)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "头痛三天",
                                  "sceneType": "PRE_CONSULTATION",
                                  "useStream": true
                                }
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        MvcResult asyncResult = mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(header().exists(RequestConstants.REQUEST_ID_HEADER))
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString(MediaType.TEXT_EVENT_STREAM_VALUE)))
                .andReturn();

        String body = asyncResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(body.contains("event:message"));
        assertTrue(body.contains("answer-part-1"));
        assertTrue(body.contains("event:meta"));
        assertTrue(body.contains("\"riskLevel\":\"low\""));
        assertTrue(body.contains("\"guardrailAction\":\"allow\""));
        assertTrue(body.contains("\"nextAction\":\"VIEW_TRIAGE_RESULT\""));
        assertTrue(body.contains("\"sessionId\":"));
        assertTrue(body.contains("\"turnId\":"));
        assertTrue(body.contains("event:end"));
        assertFalse(body.contains("\"code\":0"));
    }

    @Test
    void chat_WhenSuccessful_ReturnJsonResult() throws Exception {
        chatAiUseCase.result = new ChatAiResult(
                90001L, 90011L, "建议尽快线下就医", new me.jianwen.mediask.domain.ai.model.AiChatReply(
                        "建议尽快线下就医",
                        "头痛三天",
                        RiskLevel.MEDIUM,
                        GuardrailAction.CAUTION,
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
                .andExpect(jsonPath("$.data.triageResult.nextAction").value("GO_REGISTRATION"));
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
    void stream_WhenUpstreamThrowsSystemException_ReturnErrorEvent() throws Exception {
        aiChatStreamPort.failure = new me.jianwen.mediask.common.exception.SysException(AiErrorCode.SERVICE_UNAVAILABLE);

        MvcResult mvcResult = mockMvc.perform(post("/api/v1/ai/chat/stream")
                        .header("Authorization", "Bearer " + PATIENT_TOKEN)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "头痛三天",
                                  "sceneType": "PRE_CONSULTATION"
                                }
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        MvcResult asyncResult = mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(header().exists(RequestConstants.REQUEST_ID_HEADER))
                .andReturn();

        String body = asyncResult.getResponse().getContentAsString();
        assertTrue(body.contains("event:error"));
        assertTrue(body.contains("\"code\":6001"));
        assertTrue(body.contains("\"msg\":\"ai service unavailable\""));
    }

    @Test
    void stream_WhenSceneTypeInvalid_ReturnJsonBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/ai/chat/stream")
                        .header("Authorization", "Bearer " + PATIENT_TOKEN)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "头痛三天",
                                  "sceneType": "UNKNOWN"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists(RequestConstants.REQUEST_ID_HEADER))
                .andExpect(jsonPath("$.code").value(1002))
                .andExpect(jsonPath("$.msg").value("sceneType is invalid"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void stream_WhenUnauthenticated_ReturnUnauthorizedJson() throws Exception {
        mockMvc.perform(post("/api/v1/ai/chat/stream")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "头痛三天",
                                  "sceneType": "PRE_CONSULTATION"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists(RequestConstants.REQUEST_ID_HEADER))
                .andExpect(jsonPath("$.code").value(1001))
                .andExpect(jsonPath("$.msg").value("unauthorized"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void stream_WhenAuthenticatedUserIsNotPatient_ReturnForbiddenJson() throws Exception {
        mockMvc.perform(post("/api/v1/ai/chat/stream")
                        .header("Authorization", "Bearer " + DOCTOR_TOKEN)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "头痛三天",
                                  "sceneType": "PRE_CONSULTATION"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(header().exists(RequestConstants.REQUEST_ID_HEADER))
                .andExpect(jsonPath("$.code").value(2008))
                .andExpect(jsonPath("$.msg").value("role mismatch"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void stream_WhenEndFrameWriteFails_CompleteEmitterWithError() {
        aiChatStreamPort.eventPublisher = consumer -> {
            consumer.accept(new AiChatStreamEvent.Meta(successTriageResult()));
            consumer.accept(new AiChatStreamEvent.End());
        };
        TestSseEmitter emitter = new TestSseEmitter(true, false);
        TestAiController controller = new TestAiController(streamAiChatUseCase(), emitter);

        controller.stream(new AiChatStreamRequest(null, "头痛三天", null, "PRE_CONSULTATION", true), patientPrincipal());

        assertTrue(emitter.completeWithErrorCalled);
        assertInstanceOf(java.io.IOException.class, emitter.completionError);
        assertFalse(emitter.completeCalled);
    }

    @Test
    void stream_WhenErrorFrameWriteFails_CompleteEmitterWithError() {
        aiChatStreamPort.eventPublisher =
                consumer -> consumer.accept(new AiChatStreamEvent.Error(6001, "ai service unavailable"));
        TestSseEmitter emitter = new TestSseEmitter(false, true);
        TestAiController controller = new TestAiController(streamAiChatUseCase(), emitter);

        controller.stream(new AiChatStreamRequest(null, "头痛三天", null, "PRE_CONSULTATION", true), patientPrincipal());

        assertTrue(emitter.completeWithErrorCalled);
        assertInstanceOf(java.io.IOException.class, emitter.completionError);
        assertFalse(emitter.completeCalled);
    }

    @Test
    void stream_WhenExecutorRejects_ReturnErrorEvent() throws Exception {
        AiController controller = new AiController(
                chatAiUseCase,
                streamAiChatUseCase(),
                task -> {
                    throw new TaskRejectedException("executor saturated");
                });
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(), new ResultResponseBodyAdvice())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .addFilters(new RequestCorrelationFilter(), new TestAuthenticationFilter(), new SecurityContextCleanupFilter())
                .setMessageConverters(
                        new StringHttpMessageConverter(),
                        new MappingJackson2HttpMessageConverter(objectMapper))
                .build();

        MvcResult mvcResult = mockMvc.perform(post("/api/v1/ai/chat/stream")
                        .header("Authorization", "Bearer " + PATIENT_TOKEN)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "头痛三天",
                                  "sceneType": "PRE_CONSULTATION"
                                }
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        MvcResult asyncResult = mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(header().exists(RequestConstants.REQUEST_ID_HEADER))
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString(MediaType.TEXT_EVENT_STREAM_VALUE)))
                .andReturn();

        String body = asyncResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(body.contains("event:error"));
        assertTrue(body.contains("\"code\":9999"));
        assertTrue(body.contains("\"msg\":\"system error\""));
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

    private AiChatTriageResult successTriageResult() {
        return new AiChatTriageResult(
                "头痛三天",
                RiskLevel.LOW,
                GuardrailAction.ALLOW,
                List.of(new RecommendedDepartment(101L, "神经内科", 1, "持续头痛建议优先排查")),
                "建议尽快就医",
                List.of(new AiCitation(7003001L, 1, 0.82, "持续头痛伴发热应线下评估。")),
                AiExecutionMetadata.empty());
    }

    private StreamAiChatUseCase streamAiChatUseCase() {
        return new StreamAiChatUseCase(
                aiChatStreamPort,
                new NoopAiSessionRepository(),
                new NoopAiTurnRepository(),
                content -> {},
                new NoopAiModelRunRepository(),
                event -> {},
                plainText -> plainText);
    }

    private static final class StubAiChatStreamPort implements AiChatStreamPort {

        private Consumer<Consumer<AiChatStreamEvent>> eventPublisher = consumer -> {};
        private RuntimeException failure;

        @Override
        public void stream(
                me.jianwen.mediask.domain.ai.model.AiChatInvocation invocation,
                Consumer<AiChatStreamEvent> eventConsumer) {
            if (failure != null) {
                throw failure;
            }
            eventPublisher.accept(eventConsumer);
        }
    }

    private static final class TestAiController extends AiController {

        private final SseEmitter emitter;

        private TestAiController(StreamAiChatUseCase streamAiChatUseCase, SseEmitter emitter) {
            super(new StubChatAiUseCase(), streamAiChatUseCase, new SyncTaskExecutor());
            this.emitter = emitter;
        }

        @Override
        SseEmitter createEmitter() {
            return emitter;
        }
    }

    private static final class TestSseEmitter extends SseEmitter {

        private final boolean failEndEvent;
        private final boolean failErrorEvent;
        private boolean completeCalled;
        private boolean completeWithErrorCalled;
        private Throwable completionError;

        private TestSseEmitter(boolean failEndEvent, boolean failErrorEvent) {
            super(0L);
            this.failEndEvent = failEndEvent;
            this.failErrorEvent = failErrorEvent;
        }

        @Override
        public void send(SseEventBuilder builder) throws java.io.IOException {
            if (failEndEvent) {
                throw new java.io.IOException("client disconnected before end");
            }
            if (failErrorEvent) {
                throw new java.io.IOException("client disconnected before error");
            }
        }

        @Override
        public void complete() {
            completeCalled = true;
        }

        @Override
        public void completeWithError(Throwable ex) {
            completeWithErrorCalled = true;
            completionError = ex;
        }
    }

    private static final class StubChatAiUseCase extends ChatAiUseCase {

        private ChatAiResult result;

        private StubChatAiUseCase() {
            super(
                    new NoopAiChatPort(),
                    new NoopAiSessionRepository(),
                    new NoopAiTurnRepository(),
                    content -> {},
                    new NoopAiModelRunRepository(),
                    event -> {},
                    plainText -> plainText);
        }

        @Override
        public ChatAiResult handle(me.jianwen.mediask.application.ai.command.ChatAiCommand command) {
            return result;
        }
    }

    private static final class NoopAiChatPort implements AiChatPort {
        @Override
        public me.jianwen.mediask.domain.ai.model.AiChatReply chat(
                me.jianwen.mediask.domain.ai.model.AiChatInvocation invocation) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class NoopAiSessionRepository implements AiSessionRepository {
        @Override
        public void save(me.jianwen.mediask.domain.ai.model.AiSession aiSession) {}

        @Override
        public java.util.Optional<me.jianwen.mediask.domain.ai.model.AiSession> findById(Long sessionId) {
            return java.util.Optional.empty();
        }

        @Override
        public void update(me.jianwen.mediask.domain.ai.model.AiSession aiSession) {}
    }

    private static final class NoopAiTurnRepository implements AiTurnRepository {
        @Override
        public void save(me.jianwen.mediask.domain.ai.model.AiTurn aiTurn) {}

        @Override
        public int findMaxTurnNoBySessionId(Long sessionId) {
            return 0;
        }

        @Override
        public void update(me.jianwen.mediask.domain.ai.model.AiTurn aiTurn) {}
    }

    private static final class NoopAiModelRunRepository implements AiModelRunRepository {
        @Override
        public void save(me.jianwen.mediask.domain.ai.model.AiModelRun aiModelRun) {}

        @Override
        public void update(me.jianwen.mediask.domain.ai.model.AiModelRun aiModelRun) {}
    }
}
