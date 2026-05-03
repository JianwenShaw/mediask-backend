package me.jianwen.mediask.application.audit.command;

import java.time.OffsetDateTime;
import me.jianwen.mediask.domain.audit.model.AuditActorType;
import me.jianwen.mediask.domain.audit.model.DataAccessAction;
import me.jianwen.mediask.domain.audit.model.DataAccessPurposeCode;

public record RecordDataAccessLogCommand(
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
