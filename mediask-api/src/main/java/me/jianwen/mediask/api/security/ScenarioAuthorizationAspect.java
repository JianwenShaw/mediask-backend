package me.jianwen.mediask.api.security;

import me.jianwen.mediask.api.audit.AuditApiSupport;
import me.jianwen.mediask.api.audit.AuditActionCodes;
import me.jianwen.mediask.api.audit.AuditResourceTypes;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;
import me.jianwen.mediask.application.authz.AuthzDecision;
import me.jianwen.mediask.application.authz.AuthzInvocationContext;
import me.jianwen.mediask.application.authz.AuthorizeScenario;
import me.jianwen.mediask.application.authz.AuthorizationDecisionService;
import me.jianwen.mediask.application.authz.ScenarioCode;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.exception.ErrorCode;
import me.jianwen.mediask.domain.audit.model.DataAccessPurposeCode;
import me.jianwen.mediask.domain.clinical.port.EncounterQueryRepository;
import me.jianwen.mediask.domain.user.port.AdminPatientQueryRepository;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ScenarioAuthorizationAspect {

    private final AuthorizationDecisionService authorizationDecisionService;
    private final AuditApiSupport auditApiSupport;
    private final EncounterQueryRepository encounterQueryRepository;
    private final AdminPatientQueryRepository adminPatientQueryRepository;

    public ScenarioAuthorizationAspect(
            AuthorizationDecisionService authorizationDecisionService,
            AuditApiSupport auditApiSupport,
            EncounterQueryRepository encounterQueryRepository,
            AdminPatientQueryRepository adminPatientQueryRepository) {
        this.authorizationDecisionService = authorizationDecisionService;
        this.auditApiSupport = auditApiSupport;
        this.encounterQueryRepository = encounterQueryRepository;
        this.adminPatientQueryRepository = adminPatientQueryRepository;
    }

    @Around("@annotation(authorizeScenario)")
    public Object authorize(ProceedingJoinPoint joinPoint, AuthorizeScenario authorizeScenario) throws Throwable {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (!(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        AuthzDecision decision = authorizationDecisionService.decide(new AuthzInvocationContext(
                authorizeScenario.value(),
                principal.toAuthzSubject(),
                resolveArguments(joinPoint)));
        if (!decision.allowed()) {
            recordDeniedAudit(authorizeScenario.value(), principal, resolveArguments(joinPoint), decision.reason().name());
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        return joinPoint.proceed();
    }

    private void recordDeniedAudit(
            ScenarioCode scenarioCode,
            AuthenticatedUserPrincipal principal,
            Map<String, Object> arguments,
            String denyReasonCode) {
        if (scenarioCode == ScenarioCode.AUDIT_EVENT_QUERY || scenarioCode == ScenarioCode.AUDIT_DATA_ACCESS_QUERY) {
            auditApiSupport.recordAuditFailure(
                    AuditActionCodes.AUDIT_QUERY_DENIED,
                    scenarioCode == ScenarioCode.AUDIT_EVENT_QUERY
                            ? AuditResourceTypes.AUDIT_EVENT
                            : AuditResourceTypes.DATA_ACCESS_LOG,
                    null,
                    principal,
                    String.valueOf(ErrorCode.FORBIDDEN.getCode()),
                    ErrorCode.FORBIDDEN.getMessage(),
                    null,
                    null,
                    summarizeDeniedAuditQuery(scenarioCode, arguments));
            return;
        }
        if (scenarioCode == ScenarioCode.PATIENT_SELF_PROFILE_VIEW) {
            auditApiSupport.recordDeniedDataAccess(
                    AuditResourceTypes.PATIENT_PROFILE,
                    principal.patientId() == null ? null : String.valueOf(principal.patientId()),
                    principal,
                    principal.userId(),
                    null,
                    DataAccessPurposeCode.SELF_SERVICE,
                    denyReasonCode);
            return;
        }
        if (scenarioCode == ScenarioCode.ADMIN_PATIENT_VIEW) {
            Object patientIdArgument = arguments.get("patientId");
            Long patientId = patientIdArgument instanceof Long id ? id : null;
            Long patientUserId = patientId == null
                    ? null
                    : adminPatientQueryRepository.findDetailByPatientId(patientId)
                            .map(detail -> detail.userId())
                            .orElse(null);
            auditApiSupport.recordDeniedDataAccess(
                    AuditResourceTypes.PATIENT_PROFILE,
                    auditApiSupport.resourceIdOf(patientId),
                    principal,
                    patientUserId,
                    null,
                    DataAccessPurposeCode.ADMIN_OPERATION,
                    denyReasonCode);
            return;
        }
        if (scenarioCode == ScenarioCode.EMR_RECORD_LIST) {
            auditApiSupport.recordDeniedDataAccess(
                    AuditResourceTypes.EMR_SUMMARY,
                    auditApiSupport.resourceIdOf(principal.userId()),
                    principal,
                    principal.userId(),
                    null,
                    DataAccessPurposeCode.SELF_SERVICE,
                    denyReasonCode);
            return;
        }
        if (scenarioCode != ScenarioCode.EMR_RECORD_READ
                && scenarioCode != ScenarioCode.EMR_RECORD_HISTORY_READ
                && scenarioCode != ScenarioCode.PRESCRIPTION_READ) {
            return;
        }
        Object encounterIdArgument = arguments.get("encounterId");
        if (!(encounterIdArgument instanceof Long encounterId)) {
            return;
        }
        var encounter = encounterQueryRepository.findDetailByEncounterId(encounterId).orElse(null);
        Long patientUserId = encounter == null ? null : encounter.patientSummary().patientUserId();
        DataAccessPurposeCode purposeCode =
                principal.patientId() != null ? DataAccessPurposeCode.SELF_SERVICE : DataAccessPurposeCode.TREATMENT;
        auditApiSupport.recordDeniedDataAccess(
                scenarioCode == ScenarioCode.PRESCRIPTION_READ
                        ? AuditResourceTypes.PRESCRIPTION_DETAIL
                        : scenarioCode == ScenarioCode.EMR_RECORD_READ
                                ? AuditResourceTypes.EMR_CONTENT
                                : AuditResourceTypes.EMR_SUMMARY,
                auditApiSupport.resourceIdOf(encounterId),
                principal,
                patientUserId,
                encounterId,
                purposeCode,
                denyReasonCode);
    }

    private Map<String, Object> resolveArguments(ProceedingJoinPoint joinPoint) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = methodSignature.getParameterNames();
        Object[] arguments = joinPoint.getArgs();
        LinkedHashMap<String, Object> mappedArguments = new LinkedHashMap<>();
        for (int i = 0; i < arguments.length; i++) {
            String parameterName = parameterNames != null && i < parameterNames.length
                    ? parameterNames[i]
                    : "arg" + i;
            mappedArguments.put(parameterName, arguments[i]);
        }
        return Collections.unmodifiableMap(mappedArguments);
    }

    private String summarizeDeniedAuditQuery(ScenarioCode scenarioCode, Map<String, Object> arguments) {
        return auditApiSupport.summarizeAuditQuery(
                scenarioCode == ScenarioCode.AUDIT_EVENT_QUERY ? "events" : "data-access",
                stringValue(arguments.get(scenarioCode == ScenarioCode.AUDIT_EVENT_QUERY ? "actionCode" : "accessAction")),
                stringValue(arguments.get("resourceType")),
                stringValue(arguments.get("resourceId")),
                longValue(arguments.get("operatorUserId")),
                longValue(arguments.get("patientUserId")),
                longValue(arguments.get("encounterId")),
                stringValue(arguments.get(scenarioCode == ScenarioCode.AUDIT_EVENT_QUERY ? "successFlag" : "accessResult")),
                arguments.get("from") instanceof java.time.OffsetDateTime from ? from : null,
                arguments.get("to") instanceof java.time.OffsetDateTime to ? to : null,
                stringValue(arguments.get("requestId")));
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long longValue(Object value) {
        return value instanceof Long longValue ? longValue : null;
    }
}
