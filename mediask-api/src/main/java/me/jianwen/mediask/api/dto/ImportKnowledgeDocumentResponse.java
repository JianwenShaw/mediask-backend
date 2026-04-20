package me.jianwen.mediask.api.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

public record ImportKnowledgeDocumentResponse(
        @JsonSerialize(using = ToStringSerializer.class) Long documentId,
        String documentUuid,
        Integer chunkCount,
        String documentStatus) {}
