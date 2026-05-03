package me.jianwen.mediask.infra.persistence.repository;

import me.jianwen.mediask.common.pagination.PageData;
import me.jianwen.mediask.common.pagination.PageQuery;
import me.jianwen.mediask.domain.audit.model.AuditEventItem;
import me.jianwen.mediask.domain.audit.model.AuditEventRecord;
import me.jianwen.mediask.domain.audit.model.DataAccessAction;
import me.jianwen.mediask.domain.audit.model.DataAccessLogItem;
import me.jianwen.mediask.domain.audit.model.DataAccessLogRecord;
import me.jianwen.mediask.domain.audit.model.DataAccessPurposeCode;
import me.jianwen.mediask.domain.audit.port.AuditEventRepository;
import me.jianwen.mediask.domain.audit.port.AuditQueryRepository;
import me.jianwen.mediask.domain.audit.port.DataAccessLogRepository;
import me.jianwen.mediask.infra.persistence.dataobject.AuditEventDO;
import me.jianwen.mediask.infra.persistence.dataobject.DataAccessLogDO;
import me.jianwen.mediask.infra.persistence.mapper.AuditEventMapper;
import me.jianwen.mediask.infra.persistence.mapper.DataAccessLogMapper;
import org.springframework.stereotype.Component;

@Component
public class AuditRepositoryAdapter implements AuditEventRepository, DataAccessLogRepository, AuditQueryRepository {

    private final AuditEventMapper auditEventMapper;
    private final DataAccessLogMapper dataAccessLogMapper;

    public AuditRepositoryAdapter(AuditEventMapper auditEventMapper, DataAccessLogMapper dataAccessLogMapper) {
        this.auditEventMapper = auditEventMapper;
        this.dataAccessLogMapper = dataAccessLogMapper;
    }

    @Override
    public void save(AuditEventRecord record) {
        AuditEventDO dataObject = new AuditEventDO();
        dataObject.setId(record.id());
        dataObject.setRequestId(record.requestId());
        dataObject.setTraceId(record.traceId());
        dataObject.setActorType(record.actorType().name());
        dataObject.setOperatorUserId(record.operatorUserId());
        dataObject.setOperatorUsername(record.operatorUsername());
        dataObject.setOperatorRoleCode(record.operatorRoleCode());
        dataObject.setActorDepartmentId(record.actorDepartmentId());
        dataObject.setActionCode(record.actionCode());
        dataObject.setResourceType(record.resourceType());
        dataObject.setResourceId(record.resourceId());
        dataObject.setPatientUserId(record.patientUserId());
        dataObject.setEncounterId(record.encounterId());
        dataObject.setSuccessFlag(record.successFlag());
        dataObject.setErrorCode(record.errorCode());
        dataObject.setErrorMessage(record.errorMessage());
        dataObject.setClientIp(record.clientIp());
        dataObject.setUserAgent(record.userAgent());
        dataObject.setReasonText(record.reasonText());
        dataObject.setOccurredAt(record.occurredAt());
        dataObject.setCreatedAt(record.occurredAt());
        auditEventMapper.insert(dataObject);
    }

    @Override
    public void save(DataAccessLogRecord record) {
        DataAccessLogDO dataObject = new DataAccessLogDO();
        dataObject.setId(record.id());
        dataObject.setRequestId(record.requestId());
        dataObject.setTraceId(record.traceId());
        dataObject.setActorType(record.actorType().name());
        dataObject.setOperatorUserId(record.operatorUserId());
        dataObject.setOperatorUsername(record.operatorUsername());
        dataObject.setOperatorRoleCode(record.operatorRoleCode());
        dataObject.setActorDepartmentId(record.actorDepartmentId());
        dataObject.setPatientUserId(record.patientUserId());
        dataObject.setEncounterId(record.encounterId());
        dataObject.setAccessAction(record.accessAction().name());
        dataObject.setAccessPurposeCode(record.accessPurposeCode().name());
        dataObject.setResourceType(record.resourceType());
        dataObject.setResourceId(record.resourceId());
        dataObject.setAccessResult(record.accessResult());
        dataObject.setDenyReasonCode(record.denyReasonCode());
        dataObject.setClientIp(record.clientIp());
        dataObject.setUserAgent(record.userAgent());
        dataObject.setOccurredAt(record.occurredAt());
        dataObject.setCreatedAt(record.occurredAt());
        dataAccessLogMapper.insert(dataObject);
    }

    @Override
    public PageData<AuditEventItem> queryAuditEvents(
            java.time.OffsetDateTime from,
            java.time.OffsetDateTime to,
            String actionCode,
            Long operatorUserId,
            Long patientUserId,
            Long encounterId,
            String resourceType,
            String resourceId,
            Boolean successFlag,
            String requestId,
            PageQuery pageQuery) {
        long total = auditEventMapper.countAuditEvents(
                from, to, actionCode, operatorUserId, patientUserId, encounterId, resourceType, resourceId, successFlag, requestId);
        var records = auditEventMapper
                .selectAuditEvents(
                        from,
                        to,
                        actionCode,
                        operatorUserId,
                        patientUserId,
                        encounterId,
                        resourceType,
                        resourceId,
                        successFlag,
                        requestId,
                        pageQuery.pageSize(),
                        pageQuery.offset())
                .stream()
                .map(item -> new AuditEventItem(
                        item.getId(),
                        item.getRequestId(),
                        item.getOperatorUserId(),
                        item.getOperatorUsername(),
                        item.getOperatorRoleCode(),
                        item.getActorDepartmentId(),
                        item.getActionCode(),
                        item.getResourceType(),
                        item.getResourceId(),
                        item.getPatientUserId(),
                        item.getEncounterId(),
                        Boolean.TRUE.equals(item.getSuccessFlag()),
                        item.getErrorCode(),
                        item.getErrorMessage(),
                        item.getReasonText(),
                        item.getClientIp(),
                        item.getUserAgent(),
                        item.getOccurredAt()))
                .toList();
        return PageQuery.toPageData(pageQuery, total, records);
    }

    @Override
    public PageData<DataAccessLogItem> queryDataAccessLogs(
            java.time.OffsetDateTime from,
            java.time.OffsetDateTime to,
            String resourceType,
            String resourceId,
            Long operatorUserId,
            Long patientUserId,
            Long encounterId,
            DataAccessAction accessAction,
            String accessResult,
            String requestId,
            PageQuery pageQuery) {
        long total = dataAccessLogMapper.countDataAccessLogs(
                from,
                to,
                resourceType,
                resourceId,
                operatorUserId,
                patientUserId,
                encounterId,
                accessAction == null ? null : accessAction.name(),
                accessResult,
                requestId);
        var records = dataAccessLogMapper
                .selectDataAccessLogs(
                        from,
                        to,
                        resourceType,
                        resourceId,
                        operatorUserId,
                        patientUserId,
                        encounterId,
                        accessAction == null ? null : accessAction.name(),
                        accessResult,
                        requestId,
                        pageQuery.pageSize(),
                        pageQuery.offset())
                .stream()
                .map(item -> new DataAccessLogItem(
                        item.getId(),
                        item.getRequestId(),
                        item.getOperatorUserId(),
                        item.getOperatorUsername(),
                        item.getOperatorRoleCode(),
                        item.getActorDepartmentId(),
                        item.getPatientUserId(),
                        item.getEncounterId(),
                        DataAccessAction.valueOf(item.getAccessAction()),
                        DataAccessPurposeCode.valueOf(item.getAccessPurposeCode()),
                        item.getResourceType(),
                        item.getResourceId(),
                        item.getAccessResult(),
                        item.getDenyReasonCode(),
                        item.getClientIp(),
                        item.getUserAgent(),
                        item.getOccurredAt()))
                .toList();
        return PageQuery.toPageData(pageQuery, total, records);
    }
}
