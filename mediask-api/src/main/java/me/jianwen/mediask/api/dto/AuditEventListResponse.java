package me.jianwen.mediask.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.OffsetDateTime;
import java.util.List;

public record AuditEventListResponse(
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
            String actionCode,
            String resourceType,
            String resourceId,
            @JsonSerialize(using = ToStringSerializer.class) Long patientUserId,
            @JsonSerialize(using = ToStringSerializer.class) Long encounterId,
            boolean successFlag,
            String errorCode,
            String errorMessage,
            String reasonText,
            String clientIp,
            String userAgent,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
            OffsetDateTime occurredAt) {}
}
