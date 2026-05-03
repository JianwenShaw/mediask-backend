package me.jianwen.mediask.application.audit.usecase;

import me.jianwen.mediask.application.audit.command.RecordAuditEventCommand;
import me.jianwen.mediask.application.audit.command.RecordDataAccessLogCommand;
import me.jianwen.mediask.application.audit.model.AuditContext;
import me.jianwen.mediask.domain.audit.model.DataAccessAction;
import me.jianwen.mediask.domain.audit.model.DataAccessPurposeCode;

public class AuditTrailService {

    private final RecordAuditEventUseCase recordAuditEventUseCase;
    private final RecordDataAccessLogUseCase recordDataAccessLogUseCase;

    public AuditTrailService(
            RecordAuditEventUseCase recordAuditEventUseCase,
            RecordDataAccessLogUseCase recordDataAccessLogUseCase) {
        this.recordAuditEventUseCase = recordAuditEventUseCase;
        this.recordDataAccessLogUseCase = recordDataAccessLogUseCase;
    }

    public void recordAuditSuccess(
            AuditContext context,
            String actionCode,
            String resourceType,
            String resourceId,
            Long patientUserId,
            Long encounterId,
            String reasonText) {
        recordAuditEventUseCase.handle(new RecordAuditEventCommand(
                context.requestId(),
                context.traceId(),
                context.actorType(),
                context.operatorUserId(),
                context.operatorUsername(),
                context.operatorRoleCode(),
                context.actorDepartmentId(),
                actionCode,
                resourceType,
                resourceId,
                patientUserId,
                encounterId,
                true,
                null,
                null,
                context.clientIp(),
                context.userAgent(),
                reasonText,
                context.occurredAt()));
    }

    public void recordAuditFailure(
            AuditContext context,
            String actionCode,
            String resourceType,
            String resourceId,
            Long patientUserId,
            Long encounterId,
            String errorCode,
            String errorMessage,
            String reasonText) {
        recordAuditEventUseCase.handle(new RecordAuditEventCommand(
                context.requestId(),
                context.traceId(),
                context.actorType(),
                context.operatorUserId(),
                context.operatorUsername(),
                context.operatorRoleCode(),
                context.actorDepartmentId(),
                actionCode,
                resourceType,
                resourceId,
                patientUserId,
                encounterId,
                false,
                errorCode,
                errorMessage,
                context.clientIp(),
                context.userAgent(),
                reasonText,
                context.occurredAt()));
    }

    public void recordAllowedDataAccess(
            AuditContext context,
            String resourceType,
            String resourceId,
            Long patientUserId,
            Long encounterId,
            DataAccessPurposeCode purposeCode) {
        recordDataAccess(context, resourceType, resourceId, patientUserId, encounterId, purposeCode, "ALLOWED", null);
    }

    public void recordDeniedDataAccess(
            AuditContext context,
            String resourceType,
            String resourceId,
            Long patientUserId,
            Long encounterId,
            DataAccessPurposeCode purposeCode,
            String denyReasonCode) {
        recordDataAccess(context, resourceType, resourceId, patientUserId, encounterId, purposeCode, "DENIED", denyReasonCode);
    }

    private void recordDataAccess(
            AuditContext context,
            String resourceType,
            String resourceId,
            Long patientUserId,
            Long encounterId,
            DataAccessPurposeCode purposeCode,
            String accessResult,
            String denyReasonCode) {
        recordDataAccessLogUseCase.handle(new RecordDataAccessLogCommand(
                context.requestId(),
                context.traceId(),
                context.actorType(),
                context.operatorUserId(),
                context.operatorUsername(),
                context.operatorRoleCode(),
                context.actorDepartmentId(),
                patientUserId,
                encounterId,
                DataAccessAction.VIEW,
                purposeCode,
                resourceType,
                resourceId,
                accessResult,
                denyReasonCode,
                context.clientIp(),
                context.userAgent(),
                context.occurredAt()));
    }
}
