package me.jianwen.mediask.application.audit.usecase;

import me.jianwen.mediask.application.audit.command.RecordAuditEventCommand;
import me.jianwen.mediask.common.id.SnowflakeIdGenerator;
import me.jianwen.mediask.domain.audit.model.AuditEventRecord;
import me.jianwen.mediask.domain.audit.port.AuditEventRepository;

public class RecordAuditEventUseCase {

    private final AuditEventRepository auditEventRepository;

    public RecordAuditEventUseCase(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    public void handle(RecordAuditEventCommand command) {
        auditEventRepository.save(new AuditEventRecord(
                SnowflakeIdGenerator.nextId(),
                command.requestId(),
                command.traceId(),
                command.actorType(),
                command.operatorUserId(),
                command.operatorUsername(),
                command.operatorRoleCode(),
                command.actorDepartmentId(),
                command.actionCode(),
                command.resourceType(),
                command.resourceId(),
                command.patientUserId(),
                command.encounterId(),
                command.successFlag(),
                command.errorCode(),
                command.errorMessage(),
                command.clientIp(),
                command.userAgent(),
                command.reasonText(),
                command.occurredAt()));
    }
}
