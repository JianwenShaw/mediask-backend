package me.jianwen.mediask.application.audit.query;

import java.time.OffsetDateTime;
import me.jianwen.mediask.common.pagination.PageQuery;
import me.jianwen.mediask.common.util.ArgumentChecks;
import me.jianwen.mediask.domain.audit.model.DataAccessAction;

public record QueryDataAccessLogs(
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
        PageQuery pageQuery) {

    public QueryDataAccessLogs {
        resourceType = ArgumentChecks.blankToNull(resourceType);
        resourceId = ArgumentChecks.blankToNull(resourceId);
        accessResult = ArgumentChecks.blankToNull(accessResult);
        requestId = ArgumentChecks.blankToNull(requestId);
        if (pageQuery == null) {
            throw new IllegalArgumentException("pageQuery must not be null");
        }
    }
}
