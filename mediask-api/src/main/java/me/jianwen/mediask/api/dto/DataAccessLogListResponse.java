package me.jianwen.mediask.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.OffsetDateTime;
import java.util.List;

public record DataAccessLogListResponse(
        List<Item> items,
        long pageNum,
        long pageSize,
        long total,
        long totalPages,
        boolean hasNext) {

    public record Item(
            @JsonSerialize(using = ToStringSerializer.class) Long id,
            String requestId,
            @JsonSerialize(using = ToStringSerializer.class) Long operatorUserId,
            String operatorUsername,
            String operatorRoleCode,
            @JsonSerialize(using = ToStringSerializer.class) Long actorDepartmentId,
            @JsonSerialize(using = ToStringSerializer.class) Long patientUserId,
            @JsonSerialize(using = ToStringSerializer.class) Long encounterId,
            String accessAction,
            String accessPurposeCode,
            String resourceType,
            String resourceId,
            String accessResult,
            String denyReasonCode,
            String clientIp,
            String userAgent,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
            OffsetDateTime occurredAt) {}
}
