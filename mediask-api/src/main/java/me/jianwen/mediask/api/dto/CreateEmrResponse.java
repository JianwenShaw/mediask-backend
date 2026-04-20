package me.jianwen.mediask.api.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.List;

public record CreateEmrResponse(
        @JsonSerialize(using = ToStringSerializer.class) Long recordId,
        String recordNo,
        @JsonSerialize(using = ToStringSerializer.class) Long encounterId,
        String recordStatus,
        int version) {
}
