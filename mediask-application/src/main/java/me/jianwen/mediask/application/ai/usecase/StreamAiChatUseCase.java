package me.jianwen.mediask.application.ai.usecase;

import java.util.Objects;
import java.util.function.Consumer;
import me.jianwen.mediask.application.ai.command.StreamAiChatCommand;
import me.jianwen.mediask.common.id.SnowflakeIdGenerator;
import me.jianwen.mediask.domain.ai.model.AiChatInvocation;
import me.jianwen.mediask.domain.ai.model.AiChatStreamEvent;
import me.jianwen.mediask.domain.ai.port.AiChatStreamPort;

public class StreamAiChatUseCase {

    private final AiChatStreamPort aiChatStreamPort;

    public StreamAiChatUseCase(AiChatStreamPort aiChatStreamPort) {
        this.aiChatStreamPort = aiChatStreamPort;
    }

    public void handle(StreamAiChatCommand command, Consumer<AiChatStreamResultEvent> eventConsumer) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(eventConsumer, "eventConsumer must not be null");

        long sessionId = command.sessionId() != null ? command.sessionId() : SnowflakeIdGenerator.nextId();
        long turnId = SnowflakeIdGenerator.nextId();
        long modelRunId = SnowflakeIdGenerator.nextId();
        String sessionUuid = "ai-session-" + sessionId;

        AiChatInvocation invocation = new AiChatInvocation(
                modelRunId,
                turnId,
                sessionUuid,
                command.message(),
                command.sceneType(),
                command.departmentId(),
                null,
                true);
        aiChatStreamPort.stream(invocation, event -> eventConsumer.accept(toResultEvent(sessionId, turnId, event)));
    }

    private AiChatStreamResultEvent toResultEvent(long sessionId, long turnId, AiChatStreamEvent event) {
        return switch (event) {
            case AiChatStreamEvent.Message message -> new AiChatStreamResultEvent.Message(message.content());
            case AiChatStreamEvent.Meta meta ->
                    new AiChatStreamResultEvent.Meta(sessionId, turnId, meta.triageResult());
            case AiChatStreamEvent.End ignored -> new AiChatStreamResultEvent.End();
            case AiChatStreamEvent.Error error -> new AiChatStreamResultEvent.Error(error.code(), error.message());
        };
    }
}
