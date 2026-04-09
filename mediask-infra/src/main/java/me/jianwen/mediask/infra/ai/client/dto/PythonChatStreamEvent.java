package me.jianwen.mediask.infra.ai.client.dto;

import java.util.Objects;
import me.jianwen.mediask.common.util.ArgumentChecks;

public sealed interface PythonChatStreamEvent
        permits PythonChatStreamEvent.Message,
                PythonChatStreamEvent.Meta,
                PythonChatStreamEvent.End,
                PythonChatStreamEvent.Error {

    record Message(String content) implements PythonChatStreamEvent {

        public Message {
            content = ArgumentChecks.requireNonBlank(content, "content");
        }
    }

    record Meta(PythonChatResponse response) implements PythonChatStreamEvent {

        public Meta {
            response = Objects.requireNonNull(response, "response must not be null");
        }
    }

    record End() implements PythonChatStreamEvent {}

    record Error(int code, String message) implements PythonChatStreamEvent {

        public Error {
            if (code <= 0) {
                throw new IllegalArgumentException("code must be greater than 0");
            }
            message = ArgumentChecks.requireNonBlank(message, "message");
        }
    }
}
