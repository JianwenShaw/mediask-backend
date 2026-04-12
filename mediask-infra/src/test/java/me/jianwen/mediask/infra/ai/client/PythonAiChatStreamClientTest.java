package me.jianwen.mediask.infra.ai.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import me.jianwen.mediask.infra.ai.client.dto.PythonChatStreamEvent;
import me.jianwen.mediask.infra.ai.client.support.AiServiceErrorDecoder;
import me.jianwen.mediask.infra.ai.client.support.AiServiceSseEventReader;
import me.jianwen.mediask.infra.ai.client.support.AiServiceTransportException;
import me.jianwen.mediask.infra.ai.client.support.SseEventFrame;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;

class PythonAiChatStreamClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void handleFrame_WhenMetaThenEnd_ShouldAcceptSequence() throws Exception {
        PythonAiChatStreamClient client = client();
        List<PythonChatStreamEvent> events = new ArrayList<>();
        boolean[] metaSeen = {false};

        boolean terminal = handleFrame(
                client,
                new SseEventFrame(
                        "meta",
                        "{\"model_run_id\":1,\"provider_run_id\":\"provider-run-1\",\"summary\":\"头痛三天\",\"recommended_departments\":[],\"care_advice\":\"建议线下就诊\",\"citations\":[],\"risk_level\":\"medium\",\"guardrail_action\":\"caution\",\"matched_rule_codes\":[\"RISK_HEADACHE\"],\"tokens_input\":100,\"tokens_output\":200,\"latency_ms\":1234,\"is_degraded\":false}"),
                events::add,
                metaSeen);
        assertFalse(terminal);
        assertTrue(metaSeen[0]);

        terminal = handleFrame(client, new SseEventFrame("end", "{}"), events::add, metaSeen);
        assertTrue(terminal);
        assertEquals(2, events.size());
        assertEquals(PythonChatStreamEvent.Meta.class, events.get(0).getClass());
        assertEquals(PythonChatStreamEvent.End.class, events.get(1).getClass());
    }

    @Test
    void handleFrame_WhenMetaDuplicated_ShouldThrowInvalidResponse() {
        PythonAiChatStreamClient client = client();
        boolean[] metaSeen = {true};

        AiServiceTransportException exception = assertThrows(
                AiServiceTransportException.class,
                () -> handleFrame(
                        client,
                        new SseEventFrame(
                                "meta",
                                "{\"model_run_id\":1,\"provider_run_id\":\"provider-run-1\",\"summary\":\"头痛三天\",\"recommended_departments\":[],\"care_advice\":\"建议线下就诊\",\"citations\":[],\"risk_level\":\"medium\",\"guardrail_action\":\"caution\",\"matched_rule_codes\":[\"RISK_HEADACHE\"],\"tokens_input\":100,\"tokens_output\":200,\"latency_ms\":1234,\"is_degraded\":false}"),
                        event -> {},
                        metaSeen));

        assertEquals(AiServiceTransportException.FailureType.INVALID_RESPONSE, exception.getFailureType());
        assertEquals("ai stream contains duplicate meta event", exception.getMessage());
    }

    @Test
    void handleFrame_WhenEndBeforeMeta_ShouldThrowInvalidResponse() {
        PythonAiChatStreamClient client = client();
        boolean[] metaSeen = {false};

        AiServiceTransportException exception = assertThrows(
                AiServiceTransportException.class,
                () -> handleFrame(client, new SseEventFrame("end", "{}"), event -> {}, metaSeen));

        assertEquals(AiServiceTransportException.FailureType.INVALID_RESPONSE, exception.getFailureType());
        assertEquals("ai stream ended before meta event", exception.getMessage());
    }

    private PythonAiChatStreamClient client() {
        return new PythonAiChatStreamClient(
                RestClient.builder().build(),
                new AiServiceErrorDecoder(objectMapper),
                new AiServiceSseEventReader(),
                objectMapper);
    }

    @SuppressWarnings("unchecked")
    private boolean handleFrame(
            PythonAiChatStreamClient client,
            SseEventFrame frame,
            Consumer<PythonChatStreamEvent> eventConsumer,
            boolean[] metaSeen) {
        try {
            Method method = PythonAiChatStreamClient.class.getDeclaredMethod(
                    "handleFrame",
                    SseEventFrame.class,
                    Consumer.class,
                    org.springframework.http.HttpStatusCode.class,
                    String.class,
                    boolean[].class);
            method.setAccessible(true);
            return (boolean) method.invoke(client, frame, eventConsumer, HttpStatus.OK, "req_stream_001", metaSeen);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new AssertionError(cause);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
