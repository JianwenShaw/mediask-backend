package me.jianwen.mediask.application.audit.model;

import java.time.OffsetDateTime;
import me.jianwen.mediask.domain.audit.model.AuditActorType;

public record AuditContext(
        String requestId,
        String traceId,
        AuditActorType actorType,
        Long operatorUserId,
        String operatorUsername,
        String operatorRoleCode,
        Long actorDepartmentId,
        String clientIp,
        String userAgent,
        OffsetDateTime occurredAt) {}
