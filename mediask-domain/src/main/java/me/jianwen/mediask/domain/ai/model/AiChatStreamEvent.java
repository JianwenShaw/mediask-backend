package me.jianwen.mediask.domain.ai.model;

import java.util.Objects;
import me.jianwen.mediask.common.util.ArgumentChecks;

public sealed interface AiChatStreamEvent
        permits AiChatStreamEvent.Message, AiChatStreamEvent.Meta, AiChatStreamEvent.End, AiChatStreamEvent.Error {

    record Message(String content) implements AiChatStreamEvent {

        public Message {
            content = ArgumentChecks.requireNonBlank(content, "content");
        }
    }

    record Meta(AiChatTriageResult triageResult) implements AiChatStreamEvent {

        public Meta {
            triageResult = Objects.requireNonNull(triageResult, "triageResult must not be null");
        }
    }

    record End() implements AiChatStreamEvent {}

    record Error(int code, String message) implements AiChatStreamEvent {

        public Error {
            if (code <= 0) {
                throw new IllegalArgumentException("code must be greater than 0");
            }
            message = ArgumentChecks.requireNonBlank(message, "message");
        }
    }
}
