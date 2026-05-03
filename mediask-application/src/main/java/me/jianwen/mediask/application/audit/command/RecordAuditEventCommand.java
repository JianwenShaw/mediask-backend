package me.jianwen.mediask.application.audit.command;

import java.time.OffsetDateTime;
import me.jianwen.mediask.domain.audit.model.AuditActorType;

public record RecordAuditEventCommand(
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
