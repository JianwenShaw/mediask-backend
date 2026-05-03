package me.jianwen.mediask.domain.audit.model;

import java.time.OffsetDateTime;

public record DataAccessLogRecord(
        Long id,
        String requestId,
        String traceId,
        AuditActorType actorType,
        Long operatorUserId,
        String operatorUsername,
        String operatorRoleCode,
        Long actorDepartmentId,
        Long patientUserId,
        Long encounterId,
        DataAccessAction accessAction,
        DataAccessPurposeCode accessPurposeCode,
        String resourceType,
        String resourceId,
        String accessResult,
        String denyReasonCode,
        String clientIp,
        String userAgent,
        OffsetDateTime occurredAt) {}
