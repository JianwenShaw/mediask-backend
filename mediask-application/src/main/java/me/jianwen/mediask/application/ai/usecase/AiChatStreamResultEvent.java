package me.jianwen.mediask.application.ai.usecase;

import java.util.Objects;
import me.jianwen.mediask.common.util.ArgumentChecks;
import me.jianwen.mediask.domain.ai.model.AiChatTriageResult;

public sealed interface AiChatStreamResultEvent
        permits AiChatStreamResultEvent.Message,
                AiChatStreamResultEvent.Meta,
                AiChatStreamResultEvent.End,
                AiChatStreamResultEvent.Error {

    record Message(String content) implements AiChatStreamResultEvent {

        public Message {
            content = ArgumentChecks.requireNonBlank(content, "content");
        }
    }

    record Meta(Long sessionId, Long turnId, AiChatTriageResult triageResult) implements AiChatStreamResultEvent {

        public Meta {
            sessionId = ArgumentChecks.requirePositive(sessionId, "sessionId");
            turnId = ArgumentChecks.requirePositive(turnId, "turnId");
            triageResult = Objects.requireNonNull(triageResult, "triageResult must not be null");
        }
    }

    record End() implements AiChatStreamResultEvent {}

    record Error(int code, String message) implements AiChatStreamResultEvent {

        public Error {
            if (code <= 0) {
                throw new IllegalArgumentException("code must be greater than 0");
            }
            message = ArgumentChecks.requireNonBlank(message, "message");
        }
    }
}
