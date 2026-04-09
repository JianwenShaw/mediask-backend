package me.jianwen.mediask.infra.ai.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.function.Consumer;
import me.jianwen.mediask.common.request.RequestConstants;
import me.jianwen.mediask.infra.ai.client.dto.PythonChatRequest;
import me.jianwen.mediask.infra.ai.client.dto.PythonChatResponse;
import me.jianwen.mediask.infra.ai.client.dto.PythonChatStreamEvent;
import me.jianwen.mediask.infra.ai.client.support.AiServiceErrorDecoder;
import me.jianwen.mediask.infra.ai.client.support.AiServiceErrorResponse;
import me.jianwen.mediask.infra.ai.client.support.AiServiceSseEventReader;
import me.jianwen.mediask.infra.ai.client.support.AiServiceTransportException;
import me.jianwen.mediask.infra.ai.client.support.SseEventFrame;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public final class PythonAiChatStreamClient {

    private static final String CHAT_STREAM_ENDPOINT = "/api/v1/chat/stream";

    private final RestClient aiServiceRestClient;
    private final AiServiceErrorDecoder aiServiceErrorDecoder;
    private final AiServiceSseEventReader aiServiceSseEventReader;
    private final ObjectMapper objectMapper;

    public PythonAiChatStreamClient(
            RestClient aiServiceRestClient,
            AiServiceErrorDecoder aiServiceErrorDecoder,
            AiServiceSseEventReader aiServiceSseEventReader,
            ObjectMapper objectMapper) {
        this.aiServiceRestClient = aiServiceRestClient;
        this.aiServiceErrorDecoder = aiServiceErrorDecoder;
        this.aiServiceSseEventReader = aiServiceSseEventReader;
        this.objectMapper = objectMapper;
    }

    public void stream(PythonChatRequest request, Consumer<PythonChatStreamEvent> eventConsumer) {
        try {
            aiServiceRestClient.post()
                    .uri(CHAT_STREAM_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .body(request)
                    .exchange((httpRequest, response) -> {
                        if (response.getStatusCode().isError()) {
                            throw aiServiceErrorDecoder.decode(
                                    response.getStatusCode(),
                                    response.getHeaders(),
                                    response.getBody().readAllBytes());
                        }
                        consumeStream(response, eventConsumer);
                        return null;
                    });
        } catch (AiServiceTransportException exception) {
            throw exception;
        } catch (ResourceAccessException exception) {
            throw AiServiceTransportException.fromResourceAccessException(exception);
        } catch (RestClientException exception) {
            throw AiServiceTransportException.invalidResponse("ai stream response invalid", exception);
        }
    }

    private void consumeStream(
            RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response,
            Consumer<PythonChatStreamEvent> eventConsumer)
            throws IOException {
        boolean[] terminal = {false};
        HttpStatusCode statusCode = response.getStatusCode();
        String upstreamRequestId = response.getHeaders().getFirst(RequestConstants.REQUEST_ID_HEADER);
        aiServiceSseEventReader.read(response.getBody(), frame -> {
            if (terminal[0]) {
                throw AiServiceTransportException.invalidResponse(
                        statusCode, upstreamRequestId, "ai stream contains events after terminal event", null);
            }
            terminal[0] = handleFrame(frame, eventConsumer);
        });
        if (!terminal[0]) {
            throw AiServiceTransportException.invalidResponse(
                    statusCode, upstreamRequestId, "ai stream ended without terminal event", null);
        }
    }

    private boolean handleFrame(SseEventFrame frame, Consumer<PythonChatStreamEvent> eventConsumer) {
        return switch (frame.eventName()) {
            case "message" -> {
                eventConsumer.accept(new PythonChatStreamEvent.Message(frame.data()));
                yield false;
            }
            case "meta" -> {
                eventConsumer.accept(new PythonChatStreamEvent.Meta(readRequired(frame.data(), PythonChatResponse.class)));
                yield false;
            }
            case "end" -> {
                eventConsumer.accept(new PythonChatStreamEvent.End());
                yield true;
            }
            case "error" -> {
                AiServiceErrorResponse error = readRequired(frame.data(), AiServiceErrorResponse.class);
                if (error.code() == null || error.code() <= 0 || error.msg() == null || error.msg().isBlank()) {
                    throw AiServiceTransportException.invalidResponse("ai stream error event invalid", null);
                }
                eventConsumer.accept(new PythonChatStreamEvent.Error(error.code(), error.msg()));
                yield true;
            }
            default -> throw AiServiceTransportException.invalidResponse(
                    "unsupported ai stream event: " + frame.eventName(), null);
        };
    }

    private <T> T readRequired(String data, Class<T> type) {
        try {
            T value = objectMapper.readValue(data, type);
            if (value == null) {
                throw AiServiceTransportException.invalidResponse("ai stream event body is empty", null);
            }
            return value;
        } catch (IOException exception) {
            throw AiServiceTransportException.invalidResponse("ai stream event body invalid", exception);
        }
    }
}
