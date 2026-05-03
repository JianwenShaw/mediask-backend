package me.jianwen.mediask.application.audit.usecase;

import me.jianwen.mediask.common.pagination.PageData;
import me.jianwen.mediask.application.audit.query.QueryDataAccessLogs;
import me.jianwen.mediask.domain.audit.model.DataAccessLogItem;
import me.jianwen.mediask.domain.audit.port.AuditQueryRepository;

public class QueryDataAccessLogsUseCase {

    private final AuditQueryRepository auditQueryRepository;

    public QueryDataAccessLogsUseCase(AuditQueryRepository auditQueryRepository) {
        this.auditQueryRepository = auditQueryRepository;
    }

    public PageData<DataAccessLogItem> handle(QueryDataAccessLogs query) {
        return auditQueryRepository.queryDataAccessLogs(
                query.from(),
                query.to(),
                query.resourceType(),
                query.resourceId(),
                query.operatorUserId(),
                query.patientUserId(),
                query.encounterId(),
                query.accessAction(),
                query.accessResult(),
                query.requestId(),
                query.pageQuery());
    }
}
