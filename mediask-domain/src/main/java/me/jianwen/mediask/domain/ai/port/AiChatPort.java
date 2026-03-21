package me.jianwen.mediask.domain.ai.port;

import me.jianwen.mediask.domain.ai.model.AiChatInvocation;
import me.jianwen.mediask.domain.ai.model.AiChatReply;

public interface AiChatPort {

    AiChatReply chat(AiChatInvocation invocation);
}
