package me.jianwen.mediask.api.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

public record AiChatResponse(
        @JsonSerialize(using = ToStringSerializer.class) Long sessionId,
        @JsonSerialize(using = ToStringSerializer.class) Long turnId,
        String answer,
        AiTriageResultResponse triageResult) {}
