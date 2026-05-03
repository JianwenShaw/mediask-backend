package me.jianwen.mediask.domain.audit.model;

import java.time.OffsetDateTime;

public record AuditEventRecord(
        Long id,
        String requestId,
        String traceId,
        AuditActorType actorType,
        Long operatorUserId,
        String operatorUsername,
        String operatorRoleCode,
        Long actorDepartmentId,
        String actionCode,
        String resourceType,
        String resourceId,
        Long patientUserId,
        Long encounterId,
        boolean successFlag,
        String errorCode,
        String errorMessage,
        String clientIp,
        String userAgent,
        String reasonText,
        OffsetDateTime occurredAt) {}
