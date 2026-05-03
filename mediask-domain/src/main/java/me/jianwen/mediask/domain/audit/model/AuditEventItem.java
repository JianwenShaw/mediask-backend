package me.jianwen.mediask.domain.audit.model;

import java.time.OffsetDateTime;

public record AuditEventItem(
        Long id,
        String requestId,
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
        String reasonText,
        String clientIp,
        String userAgent,
        OffsetDateTime occurredAt) {}
