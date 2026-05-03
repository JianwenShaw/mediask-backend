package me.jianwen.mediask.application.audit.query;

import java.time.OffsetDateTime;
import me.jianwen.mediask.common.pagination.PageQuery;
import me.jianwen.mediask.common.util.ArgumentChecks;

public record QueryAuditEvents(
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
        PageQuery pageQuery) {

    public QueryAuditEvents {
        actionCode = ArgumentChecks.blankToNull(actionCode);
        resourceType = ArgumentChecks.blankToNull(resourceType);
        resourceId = ArgumentChecks.blankToNull(resourceId);
        requestId = ArgumentChecks.blankToNull(requestId);
        if (pageQuery == null) {
            throw new IllegalArgumentException("pageQuery must not be null");
        }
    }
}
