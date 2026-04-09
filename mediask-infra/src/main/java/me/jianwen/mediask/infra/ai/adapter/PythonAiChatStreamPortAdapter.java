package me.jianwen.mediask.infra.ai.adapter;

import java.util.Objects;
import java.util.function.Consumer;
import me.jianwen.mediask.common.exception.ErrorCodeCategory;
import me.jianwen.mediask.common.exception.ErrorCodeType;
import me.jianwen.mediask.common.exception.SysException;
import me.jianwen.mediask.domain.ai.exception.AiErrorCode;
import me.jianwen.mediask.domain.ai.model.AiChatInvocation;
import me.jianwen.mediask.domain.ai.model.AiChatStreamEvent;
import me.jianwen.mediask.domain.ai.port.AiChatStreamPort;
import me.jianwen.mediask.infra.ai.client.PythonAiChatStreamClient;
import me.jianwen.mediask.infra.ai.client.dto.PythonChatStreamEvent;
import me.jianwen.mediask.infra.ai.client.mapper.PythonAiChatMapper;
import me.jianwen.mediask.infra.ai.client.support.AiServiceTransportException;

public final class PythonAiChatStreamPortAdapter implements AiChatStreamPort {

    private final PythonAiChatStreamClient pythonAiChatStreamClient;
    private final PythonAiChatMapper pythonAiChatMapper;

    public PythonAiChatStreamPortAdapter(
            PythonAiChatStreamClient pythonAiChatStreamClient, PythonAiChatMapper pythonAiChatMapper) {
        this.pythonAiChatStreamClient = pythonAiChatStreamClient;
        this.pythonAiChatMapper = pythonAiChatMapper;
    }

    @Override
    public void stream(AiChatInvocation invocation, Consumer<AiChatStreamEvent> eventConsumer) {
        try {
            pythonAiChatStreamClient.stream(pythonAiChatMapper.toStreamRequest(invocation), event -> {
                switch (event) {
                    case PythonChatStreamEvent.Message message ->
                            eventConsumer.accept(new AiChatStreamEvent.Message(message.content()));
                    case PythonChatStreamEvent.Meta meta ->
                            eventConsumer.accept(
                                    new AiChatStreamEvent.Meta(pythonAiChatMapper.toStreamMetaDomain(meta.response())));
                    case PythonChatStreamEvent.End ignored -> eventConsumer.accept(new AiChatStreamEvent.End());
                    case PythonChatStreamEvent.Error error ->
                            eventConsumer.accept(new AiChatStreamEvent.Error(error.code(), error.message()));
                }
            });
        } catch (AiServiceTransportException exception) {
            throw translate(exception);
        } catch (IllegalArgumentException exception) {
            throw new SysException(AiErrorCode.INVALID_RESPONSE, AiErrorCode.INVALID_RESPONSE.getMessage(), exception);
        }
    }

    private SysException translate(AiServiceTransportException exception) {
        return switch (exception.getFailureType()) {
            case TIMEOUT -> new SysException(
                    AiErrorCode.SERVICE_TIMEOUT, resolveMessage(exception, AiErrorCode.SERVICE_TIMEOUT), exception);
            case INVALID_RESPONSE -> new SysException(
                    AiErrorCode.INVALID_RESPONSE, resolveMessage(exception, AiErrorCode.INVALID_RESPONSE), exception);
            case UNAVAILABLE -> new SysException(
                    AiErrorCode.SERVICE_UNAVAILABLE,
                    resolveMessage(exception, AiErrorCode.SERVICE_UNAVAILABLE),
                    exception);
            case UPSTREAM_ERROR -> translateUpstreamError(exception);
        };
    }

    private SysException translateUpstreamError(AiServiceTransportException exception) {
        Integer upstreamCode = exception.getUpstreamCode();
        String upstreamMessage = normalize(exception.getMessage());
        if (upstreamCode != null && upstreamCode > 0) {
            String effectiveMessage = upstreamMessage == null ? AiErrorCode.SERVICE_UNAVAILABLE.getMessage() : upstreamMessage;
            return new SysException(new UpstreamAiErrorCode(upstreamCode, effectiveMessage), effectiveMessage, exception);
        }
        AiErrorCode fallbackCode = resolveUpstreamFallbackCode(exception);
        return new SysException(fallbackCode, resolveMessage(exception, fallbackCode), exception);
    }

    private AiErrorCode resolveUpstreamFallbackCode(AiServiceTransportException exception) {
        Integer upstreamCode = exception.getUpstreamCode();
        if (Objects.equals(upstreamCode, AiErrorCode.SERVICE_TIMEOUT.getCode())) {
            return AiErrorCode.SERVICE_TIMEOUT;
        }
        if (Objects.equals(upstreamCode, AiErrorCode.INVALID_RESPONSE.getCode())) {
            return AiErrorCode.INVALID_RESPONSE;
        }
        return AiErrorCode.SERVICE_UNAVAILABLE;
    }

    private String resolveMessage(AiServiceTransportException exception, AiErrorCode fallback) {
        Integer upstreamCode = exception.getUpstreamCode();
        if (Objects.equals(upstreamCode, AiErrorCode.SERVICE_UNAVAILABLE.getCode())
                || Objects.equals(upstreamCode, AiErrorCode.SERVICE_TIMEOUT.getCode())
                || Objects.equals(upstreamCode, AiErrorCode.INVALID_RESPONSE.getCode())) {
            return exception.getMessage();
        }
        return fallback.getMessage();
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record UpstreamAiErrorCode(int code, String message) implements ErrorCodeType {

        @Override
        public int getCode() {
            return code;
        }

        @Override
        public String getMessage() {
            return message;
        }

        @Override
        public ErrorCodeCategory getCategory() {
            return ErrorCodeCategory.INTERNAL_ERROR;
        }
    }
}
