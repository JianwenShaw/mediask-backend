package me.jianwen.mediask.api.controller;

import java.time.OffsetDateTime;
import me.jianwen.mediask.api.audit.AuditActionCodes;
import me.jianwen.mediask.api.audit.AuditApiSupport;
import me.jianwen.mediask.api.audit.AuditResourceTypes;
import me.jianwen.mediask.api.dto.AuditEventListResponse;
import me.jianwen.mediask.api.dto.DataAccessLogListResponse;
import me.jianwen.mediask.api.security.AuthenticatedUserPrincipal;
import me.jianwen.mediask.application.audit.query.QueryAuditEvents;
import me.jianwen.mediask.application.audit.query.QueryDataAccessLogs;
import me.jianwen.mediask.application.audit.usecase.QueryAuditEventsUseCase;
import me.jianwen.mediask.application.audit.usecase.QueryDataAccessLogsUseCase;
import me.jianwen.mediask.application.authz.AuthorizeScenario;
import me.jianwen.mediask.application.authz.ScenarioCode;
import me.jianwen.mediask.common.pagination.PageQuery;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.exception.ErrorCode;
import me.jianwen.mediask.common.result.Result;
import me.jianwen.mediask.domain.audit.model.DataAccessAction;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final QueryAuditEventsUseCase queryAuditEventsUseCase;
    private final QueryDataAccessLogsUseCase queryDataAccessLogsUseCase;
    private final AuditApiSupport auditApiSupport;

    public AuditController(
            QueryAuditEventsUseCase queryAuditEventsUseCase,
            QueryDataAccessLogsUseCase queryDataAccessLogsUseCase,
            AuditApiSupport auditApiSupport) {
        this.queryAuditEventsUseCase = queryAuditEventsUseCase;
        this.queryDataAccessLogsUseCase = queryDataAccessLogsUseCase;
        this.auditApiSupport = auditApiSupport;
    }

    @GetMapping("/events")
    @AuthorizeScenario(ScenarioCode.AUDIT_EVENT_QUERY)
    public Result<AuditEventListResponse> queryEvents(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(required = false) String actionCode,
            @RequestParam(required = false) Long operatorUserId,
            @RequestParam(required = false) Long patientUserId,
            @RequestParam(required = false) Long encounterId,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceId,
            @RequestParam(required = false) Boolean successFlag,
            @RequestParam(required = false) String requestId,
            @RequestParam(required = false) Long pageNo,
            @RequestParam(required = false) Long pageSize,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        ensurePrincipal(principal);
        var items = queryAuditEventsUseCase.handle(new QueryAuditEvents(
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
                PageQuery.of(pageNo, pageSize)));
        auditApiSupport.recordAuditSuccess(
                AuditActionCodes.AUDIT_QUERY,
                AuditResourceTypes.AUDIT_EVENT,
                null,
                principal,
                patientUserId,
                encounterId,
                auditApiSupport.summarizeAuditQuery(
                        "events",
                        actionCode,
                        resourceType,
                        resourceId,
                        operatorUserId,
                        patientUserId,
                        encounterId,
                        successFlag == null ? null : String.valueOf(successFlag),
                        from,
                        to,
                        requestId));
        return Result.ok(new AuditEventListResponse(items.items().stream()
                .map(item -> new AuditEventListResponse.Item(
                        item.id(),
                        item.requestId(),
                        item.operatorUserId(),
                        item.operatorUsername(),
                        item.operatorRoleCode(),
                        item.actorDepartmentId(),
                        item.actionCode(),
                        item.resourceType(),
                        item.resourceId(),
                        item.patientUserId(),
                        item.encounterId(),
                        item.successFlag(),
                        item.errorCode(),
                        item.errorMessage(),
                        item.reasonText(),
                        item.clientIp(),
                        item.userAgent(),
                        item.occurredAt()))
                .toList(),
                items.pageNum(),
                items.pageSize(),
                items.total(),
                items.totalPages(),
                items.hasNext()));
    }

    @GetMapping("/data-access")
    @AuthorizeScenario(ScenarioCode.AUDIT_DATA_ACCESS_QUERY)
    public Result<DataAccessLogListResponse> queryDataAccessLogs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceId,
            @RequestParam(required = false) Long operatorUserId,
            @RequestParam(required = false) Long patientUserId,
            @RequestParam(required = false) Long encounterId,
            @RequestParam(required = false) DataAccessAction accessAction,
            @RequestParam(required = false) String accessResult,
            @RequestParam(required = false) String requestId,
            @RequestParam(required = false) Long pageNo,
            @RequestParam(required = false) Long pageSize,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        ensurePrincipal(principal);
        var items = queryDataAccessLogsUseCase.handle(new QueryDataAccessLogs(
                from,
                to,
                resourceType,
                resourceId,
                operatorUserId,
                patientUserId,
                encounterId,
                accessAction,
                accessResult,
                requestId,
                PageQuery.of(pageNo, pageSize)));
        auditApiSupport.recordAuditSuccess(
                AuditActionCodes.AUDIT_QUERY,
                AuditResourceTypes.DATA_ACCESS_LOG,
                null,
                principal,
                patientUserId,
                encounterId,
                auditApiSupport.summarizeAuditQuery(
                        "data-access",
                        accessAction == null ? null : accessAction.name(),
                        resourceType,
                        resourceId,
                        operatorUserId,
                        patientUserId,
                        encounterId,
                        accessResult,
                        from,
                        to,
                        requestId));
        return Result.ok(new DataAccessLogListResponse(items.items().stream()
                .map(item -> new DataAccessLogListResponse.Item(
                        item.id(),
                        item.requestId(),
                        item.operatorUserId(),
                        item.operatorUsername(),
                        item.operatorRoleCode(),
                        item.actorDepartmentId(),
                        item.patientUserId(),
                        item.encounterId(),
                        item.accessAction().name(),
                        item.accessPurposeCode().name(),
                        item.resourceType(),
                        item.resourceId(),
                        item.accessResult(),
                        item.denyReasonCode(),
                        item.clientIp(),
                        item.userAgent(),
                        item.occurredAt()))
                .toList(),
                items.pageNum(),
                items.pageSize(),
                items.total(),
                items.totalPages(),
                items.hasNext()));
    }

    private void ensurePrincipal(AuthenticatedUserPrincipal principal) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
    }
}
