package me.jianwen.mediask.application.audit.usecase;

import me.jianwen.mediask.application.audit.command.RecordDataAccessLogCommand;
import me.jianwen.mediask.common.id.SnowflakeIdGenerator;
import me.jianwen.mediask.domain.audit.model.DataAccessLogRecord;
import me.jianwen.mediask.domain.audit.port.DataAccessLogRepository;

public class RecordDataAccessLogUseCase {

    private final DataAccessLogRepository dataAccessLogRepository;

    public RecordDataAccessLogUseCase(DataAccessLogRepository dataAccessLogRepository) {
        this.dataAccessLogRepository = dataAccessLogRepository;
    }

    public void handle(RecordDataAccessLogCommand command) {
        dataAccessLogRepository.save(new DataAccessLogRecord(
                SnowflakeIdGenerator.nextId(),
                command.requestId(),
                command.traceId(),
                command.actorType(),
                command.operatorUserId(),
                command.operatorUsername(),
                command.operatorRoleCode(),
                command.actorDepartmentId(),
                command.patientUserId(),
                command.encounterId(),
                command.accessAction(),
                command.accessPurposeCode(),
                command.resourceType(),
                command.resourceId(),
                command.accessResult(),
                command.denyReasonCode(),
                command.clientIp(),
                command.userAgent(),
                command.occurredAt()));
    }
}
