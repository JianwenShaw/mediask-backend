package me.jianwen.mediask.application.audit.usecase;

import me.jianwen.mediask.common.pagination.PageData;
import me.jianwen.mediask.application.audit.query.QueryAuditEvents;
import me.jianwen.mediask.domain.audit.model.AuditEventItem;
import me.jianwen.mediask.domain.audit.port.AuditQueryRepository;

public class QueryAuditEventsUseCase {

    private final AuditQueryRepository auditQueryRepository;

    public QueryAuditEventsUseCase(AuditQueryRepository auditQueryRepository) {
        this.auditQueryRepository = auditQueryRepository;
    }

    public PageData<AuditEventItem> handle(QueryAuditEvents query) {
        return auditQueryRepository.queryAuditEvents(
                query.from(),
                query.to(),
                query.actionCode(),
                query.operatorUserId(),
                query.patientUserId(),
                query.encounterId(),
                query.resourceType(),
                query.resourceId(),
                query.successFlag(),
                query.requestId(),
                query.pageQuery());
    }
}
