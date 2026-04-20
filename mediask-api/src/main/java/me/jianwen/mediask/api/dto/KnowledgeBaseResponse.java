package me.jianwen.mediask.api.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

public record KnowledgeBaseResponse(
        @JsonSerialize(using = ToStringSerializer.class) Long id,
        String kbCode,
        String name,
        String ownerType,
        @JsonSerialize(using = ToStringSerializer.class) Long ownerDeptId,
        String visibility,
        String status,
        Long docCount) {}
