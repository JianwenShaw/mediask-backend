package me.jianwen.mediask.domain.ai.port;

import java.util.function.Consumer;
import me.jianwen.mediask.domain.ai.model.AiChatInvocation;
import me.jianwen.mediask.domain.ai.model.AiChatStreamEvent;

public interface AiChatStreamPort {

    void stream(AiChatInvocation invocation, Consumer<AiChatStreamEvent> eventConsumer);
}
