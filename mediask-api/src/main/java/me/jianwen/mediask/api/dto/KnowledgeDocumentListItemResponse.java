package me.jianwen.mediask.api.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

public record KnowledgeDocumentListItemResponse(
        @JsonSerialize(using = ToStringSerializer.class) Long id,
        String documentUuid,
        String title,
        String sourceType,
        String documentStatus,
        Long chunkCount) {}
