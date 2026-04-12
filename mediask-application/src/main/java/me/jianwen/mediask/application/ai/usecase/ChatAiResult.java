package me.jianwen.mediask.application.ai.usecase;

import java.util.Objects;
import me.jianwen.mediask.common.util.ArgumentChecks;
import me.jianwen.mediask.domain.ai.model.AiChatReply;

public record ChatAiResult(Long sessionId, Long turnId, String answer, AiChatReply reply) {

    public ChatAiResult {
        sessionId = ArgumentChecks.requirePositive(sessionId, "sessionId");
        turnId = ArgumentChecks.requirePositive(turnId, "turnId");
        answer = ArgumentChecks.requireNonBlank(answer, "answer");
        reply = Objects.requireNonNull(reply, "reply must not be null");
    }
}
