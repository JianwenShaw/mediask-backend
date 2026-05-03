package me.jianwen.mediask.domain.audit.port;

import java.time.OffsetDateTime;
import me.jianwen.mediask.common.pagination.PageData;
import me.jianwen.mediask.common.pagination.PageQuery;
import me.jianwen.mediask.domain.audit.model.AuditEventItem;
import me.jianwen.mediask.domain.audit.model.DataAccessAction;
import me.jianwen.mediask.domain.audit.model.DataAccessLogItem;

public interface AuditQueryRepository {

    PageData<AuditEventItem> queryAuditEvents(
            OffsetDateTime from,
            OffsetDateTime to,
            String actionCode,
            Long operatorUserId,
            Long patientUserId,
            Long encounterId,
            String resourceType,
            String resourceId,
            Boolean successFlag,
            String requestId,
            PageQuery pageQuery);

    PageData<DataAccessLogItem> queryDataAccessLogs(
            OffsetDateTime from,
            OffsetDateTime to,
            String resourceType,
            String resourceId,
            Long operatorUserId,
            Long patientUserId,
            Long encounterId,
            DataAccessAction accessAction,
            String accessResult,
            String requestId,
            PageQuery pageQuery);
}
