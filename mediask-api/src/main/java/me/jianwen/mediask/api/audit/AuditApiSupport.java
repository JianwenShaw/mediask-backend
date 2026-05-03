package me.jianwen.mediask.api.audit;

import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import me.jianwen.mediask.api.context.ApiRequestContext;
import me.jianwen.mediask.api.security.AuthenticatedUserPrincipal;
import me.jianwen.mediask.application.audit.model.AuditContext;
import me.jianwen.mediask.application.audit.usecase.AuditTrailService;
import me.jianwen.mediask.domain.audit.model.AuditActorType;
import me.jianwen.mediask.domain.audit.model.DataAccessPurposeCode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class AuditApiSupport {

    private final AuditTrailService auditTrailService;

    public AuditApiSupport(AuditTrailService auditTrailService) {
        this.auditTrailService = auditTrailService;
    }

    public void recordAuditSuccess(
            String actionCode,
            String resourceType,
            String resourceId,
            AuthenticatedUserPrincipal principal,
            Long patientUserId,
            Long encounterId,
            String reasonText) {
        auditTrailService.recordAuditSuccess(
                currentContext(principal), actionCode, resourceType, resourceId, patientUserId, encounterId, reasonText);
    }

    public void recordAuditSuccess(
            AuditContext context,
            String actionCode,
            String resourceType,
            String resourceId,
            Long patientUserId,
            Long encounterId,
            String reasonText) {
        auditTrailService.recordAuditSuccess(
                context, actionCode, resourceType, resourceId, patientUserId, encounterId, reasonText);
    }

    public void recordAuditFailure(
            String actionCode,
            String resourceType,
            String resourceId,
            AuthenticatedUserPrincipal principal,
            String errorCode,
            String errorMessage,
            Long patientUserId,
            Long encounterId,
            String reasonText) {
        auditTrailService.recordAuditFailure(
                currentContext(principal),
                actionCode,
                resourceType,
                resourceId,
                patientUserId,
                encounterId,
                errorCode,
                errorMessage,
                reasonText);
    }

    public void recordAuditFailure(
            AuditContext context,
            String actionCode,
            String resourceType,
            String resourceId,
            String errorCode,
            String errorMessage,
            Long patientUserId,
            Long encounterId,
            String reasonText) {
        auditTrailService.recordAuditFailure(
                context,
                actionCode,
                resourceType,
                resourceId,
                patientUserId,
                encounterId,
                errorCode,
                errorMessage,
                reasonText);
    }

    public void recordAllowedDataAccess(
            String resourceType,
            String resourceId,
            AuthenticatedUserPrincipal principal,
            Long patientUserId,
            Long encounterId,
            DataAccessPurposeCode purposeCode) {
        auditTrailService.recordAllowedDataAccess(
                currentContext(principal), resourceType, resourceId, patientUserId, encounterId, purposeCode);
    }

    public void recordDeniedDataAccess(
            String resourceType,
            String resourceId,
            AuthenticatedUserPrincipal principal,
            Long patientUserId,
            Long encounterId,
            DataAccessPurposeCode purposeCode,
            String denyReasonCode) {
        auditTrailService.recordDeniedDataAccess(
                currentContext(principal),
                resourceType,
                resourceId,
                patientUserId,
                encounterId,
                purposeCode,
                denyReasonCode);
    }

    public AuditContext currentContext(AuthenticatedUserPrincipal principal) {
        return currentContext(principal, principal == null ? null : principal.username());
    }

    public AuditContext currentContext(AuthenticatedUserPrincipal principal, String operatorUsername) {
        return new AuditContext(
                ApiRequestContext.currentRequestIdOrGenerate(),
                null,
                AuditActorType.USER,
                principal == null ? null : principal.userId(),
                operatorUsername,
                principal == null ? null : primaryRoleCode(principal),
                principal == null ? null : principal.primaryDepartmentId(),
                clientIp(),
                userAgent(),
                nowUtc());
    }

    public String resourceIdOf(Long value) {
        return value == null ? null : String.valueOf(value);
    }

    public String summarizeAuditQuery(
            String scope,
            String actionOrAccess,
            String resourceType,
            String resourceId,
            Long operatorUserId,
            Long patientUserId,
            Long encounterId,
            String result,
            OffsetDateTime from,
            OffsetDateTime to,
            String requestId) {
        return "scope=" + nullSafe(scope)
                + ", action=" + nullSafe(actionOrAccess)
                + ", resourceType=" + nullSafe(resourceType)
                + ", resourceId=" + nullSafe(resourceId)
                + ", operatorUserId=" + nullSafe(operatorUserId)
                + ", patientUserId=" + nullSafe(patientUserId)
                + ", encounterId=" + nullSafe(encounterId)
                + ", result=" + nullSafe(result)
                + ", from=" + nullSafe(from)
                + ", to=" + nullSafe(to)
                + ", requestId=" + nullSafe(requestId);
    }

    private String primaryRoleCode(AuthenticatedUserPrincipal principal) {
        return principal.roles().isEmpty() ? null : principal.roles().getFirst();
    }

    private String clientIp() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String userAgent() {
        HttpServletRequest request = currentRequest();
        return request == null ? null : request.getHeader("User-Agent");
    }

    private HttpServletRequest currentRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        return attributes.getRequest();
    }

    private OffsetDateTime nowUtc() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    private String nullSafe(String value) {
        return value == null ? "-" : value;
    }

    private String nullSafe(Long value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private String nullSafe(OffsetDateTime value) {
        return value == null ? "-" : value.toString();
    }
}
