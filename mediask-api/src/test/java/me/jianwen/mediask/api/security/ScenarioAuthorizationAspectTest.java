package me.jianwen.mediask.api.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static me.jianwen.mediask.api.TestAuditSupport.auditApiSupport;
import static me.jianwen.mediask.api.TestAuditSupport.emptyEncounterQueryRepository;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import me.jianwen.mediask.api.audit.AuditApiSupport;
import me.jianwen.mediask.application.audit.usecase.AuditTrailService;
import me.jianwen.mediask.application.audit.usecase.RecordAuditEventUseCase;
import me.jianwen.mediask.application.audit.usecase.RecordDataAccessLogUseCase;
import me.jianwen.mediask.application.authz.AuthzDecision;
import me.jianwen.mediask.application.authz.AuthzDecisionReason;
import me.jianwen.mediask.application.authz.AuthzInvocationContext;
import me.jianwen.mediask.application.authz.AuthorizeScenario;
import me.jianwen.mediask.application.authz.AuthorizationDecisionService;
import me.jianwen.mediask.application.authz.ScenarioCode;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.exception.ErrorCode;
import me.jianwen.mediask.domain.audit.model.AuditEventRecord;
import me.jianwen.mediask.domain.user.model.AuthenticatedUser;
import me.jianwen.mediask.domain.user.model.RoleCode;
import me.jianwen.mediask.domain.user.model.UserType;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.SourceLocation;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class ScenarioAuthorizationAspectTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authorize_WhenPrincipalIsNotAuthenticatedUserPrincipal_ThrowUnauthorized() throws Throwable {
        StubAuthorizationDecisionService decisionService = new StubAuthorizationDecisionService(AuthzDecision.allow());
        ScenarioAuthorizationAspect aspect =
                new ScenarioAuthorizationAspect(
                        decisionService,
                        auditApiSupport(),
                        emptyEncounterQueryRepository(),
                        me.jianwen.mediask.api.TestAuditSupport.emptyAdminPatientQueryRepository());
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("plain-principal", null, "ROLE_USER"));

        BizException exception = assertThrows(
                BizException.class,
                () -> aspect.authorize(new StubProceedingJoinPoint(1001L, "id", "ok"), authorizeScenario()));

        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
    }

    @Test
    void authorize_WhenDecisionDenied_ThrowForbidden() throws Throwable {
        StubAuthorizationDecisionService decisionService =
                new StubAuthorizationDecisionService(AuthzDecision.deny(AuthzDecisionReason.OBJECT_SCOPE_DENIED));
        ScenarioAuthorizationAspect aspect =
                new ScenarioAuthorizationAspect(
                        decisionService,
                        auditApiSupport(),
                        emptyEncounterQueryRepository(),
                        me.jianwen.mediask.api.TestAuditSupport.emptyAdminPatientQueryRepository());
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(principal(), null, "ROLE_PATIENT"));

        BizException exception = assertThrows(
                BizException.class,
                () -> aspect.authorize(mockJoinPoint(1001L, "id", "ok"), authorizeScenario()));

        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
    }

    @Test
    void authorize_WhenDecisionAllowed_ProceedAndMapAuthzSubject() throws Throwable {
        StubAuthorizationDecisionService decisionService = new StubAuthorizationDecisionService(AuthzDecision.allow());
        ScenarioAuthorizationAspect aspect =
                new ScenarioAuthorizationAspect(
                        decisionService,
                        auditApiSupport(),
                        emptyEncounterQueryRepository(),
                        me.jianwen.mediask.api.TestAuditSupport.emptyAdminPatientQueryRepository());
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(principal(), null, "ROLE_PATIENT"));

        Object result = aspect.authorize(mockJoinPoint(1001L, "id", "ok"), authorizeScenario());

        assertEquals("ok", result);
        AuthzInvocationContext invocationContext = decisionService.lastInvocationContext();
        assertEquals(ScenarioCode.EMR_RECORD_READ, invocationContext.scenarioCode());
        assertEquals(2003L, invocationContext.subject().userId());
        assertEquals(true, invocationContext.subject().hasPermission("emr:read"));
        assertEquals(Map.of("id", 1001L), invocationContext.arguments());
    }

    @Test
    void authorize_WhenAuditQueryDenied_RecordSummaryReasonText() throws Throwable {
        List<AuditEventRecord> auditEvents = new ArrayList<>();
        AuditApiSupport auditApiSupport = new AuditApiSupport(new AuditTrailService(
                new RecordAuditEventUseCase(auditEvents::add),
                new RecordDataAccessLogUseCase(record -> {})));
        StubAuthorizationDecisionService decisionService =
                new StubAuthorizationDecisionService(AuthzDecision.deny(AuthzDecisionReason.MISSING_PERMISSION));
        ScenarioAuthorizationAspect aspect =
                new ScenarioAuthorizationAspect(
                        decisionService,
                        auditApiSupport,
                        emptyEncounterQueryRepository(),
                        me.jianwen.mediask.api.TestAuditSupport.emptyAdminPatientQueryRepository());
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(principal(), null, "ROLE_PATIENT"));

        BizException exception = assertThrows(
                BizException.class,
                () -> aspect.authorize(
                        new StubAuditQueryProceedingJoinPoint(
                                OffsetDateTime.parse("2026-05-01T00:00:00Z"),
                                OffsetDateTime.parse("2026-05-02T00:00:00Z"),
                                "EMR_CREATE",
                                2101L,
                                2201L,
                                8101L,
                                "EMR_RECORD",
                                "8101",
                                Boolean.TRUE,
                                "req-1"),
                        auditEventQueryScenario()));

        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
        assertEquals("AUDIT_QUERY_DENIED", auditEvents.getLast().actionCode());
        assertEquals(
                "scope=events, action=EMR_CREATE, resourceType=EMR_RECORD, resourceId=8101, operatorUserId=2101, patientUserId=2201, encounterId=8101, result=true, from=2026-05-01T00:00Z, to=2026-05-02T00:00Z, requestId=req-1",
                auditEvents.getLast().reasonText());
    }

    private static ProceedingJoinPoint mockJoinPoint(Long resourceId, String parameterName, Object proceedResult)
            throws Throwable {
        return new StubProceedingJoinPoint(resourceId, parameterName, proceedResult);
    }

    private static AuthorizeScenario authorizeScenario() {
        try {
            Method method = TestScenarioController.class.getDeclaredMethod("readEmr", Long.class);
            return method.getAnnotation(AuthorizeScenario.class);
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException("failed to resolve @AuthorizeScenario for test method", exception);
        }
    }

    private static AuthorizeScenario auditEventQueryScenario() {
        try {
            Method method = TestScenarioController.class.getDeclaredMethod(
                    "queryAuditEvents",
                    OffsetDateTime.class,
                    OffsetDateTime.class,
                    String.class,
                    Long.class,
                    Long.class,
                    Long.class,
                    String.class,
                    String.class,
                    Boolean.class,
                    String.class);
            return method.getAnnotation(AuthorizeScenario.class);
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException("failed to resolve audit query scenario", exception);
        }
    }

    private static AuthenticatedUserPrincipal principal() {
        return AuthenticatedUserPrincipal.from(new AuthenticatedUser(
                2003L,
                "patient_li",
                "李患者",
                UserType.PATIENT,
                new java.util.LinkedHashSet<>(List.of(RoleCode.PATIENT)),
                java.util.Set.of("emr:read"),
                java.util.Set.of(),
                2201L,
                null,
                null));
    }

    private static class TestScenarioController {

        @AuthorizeScenario(ScenarioCode.EMR_RECORD_READ)
        void readEmr(Long id) {}

        @AuthorizeScenario(ScenarioCode.AUDIT_EVENT_QUERY)
        void queryAuditEvents(
                OffsetDateTime from,
                OffsetDateTime to,
                String actionCode,
                Long operatorUserId,
                Long patientUserId,
                Long encounterId,
                String resourceType,
                String resourceId,
                Boolean successFlag,
                String requestId) {}
    }

    private static final class StubAuthorizationDecisionService extends AuthorizationDecisionService {

        private final AuthzDecision decision;
        private AuthzInvocationContext lastInvocationContext;

        private StubAuthorizationDecisionService(AuthzDecision decision) {
            super(List.of(), List.of());
            this.decision = decision;
        }

        @Override
        public AuthzDecision decide(AuthzInvocationContext invocationContext) {
            lastInvocationContext = invocationContext;
            return decision;
        }

        private AuthzInvocationContext lastInvocationContext() {
            return lastInvocationContext;
        }
    }

    private static final class StubProceedingJoinPoint implements ProceedingJoinPoint {

        private final Object[] arguments;
        private final MethodSignature signature;
        private final Object proceedResult;

        private StubProceedingJoinPoint(Long resourceId, String parameterName, Object proceedResult) {
            this.arguments = new Object[] {resourceId};
            this.signature = new StubMethodSignature(parameterName);
            this.proceedResult = proceedResult;
        }

        @Override
        public Object proceed() {
            return proceedResult;
        }

        @Override
        public Object proceed(Object[] args) {
            return proceedResult;
        }

        @Override
        public Object[] getArgs() {
            return arguments;
        }

        @Override
        public Signature getSignature() {
            return signature;
        }

        @Override
        public SourceLocation getSourceLocation() {
            return null;
        }

        @Override
        public Object getThis() {
            return null;
        }

        @Override
        public Object getTarget() {
            return null;
        }

        @Override
        public String toShortString() {
            return "StubProceedingJoinPoint";
        }

        @Override
        public String toLongString() {
            return "StubProceedingJoinPoint";
        }

        @Override
        public void set$AroundClosure(org.aspectj.runtime.internal.AroundClosure arc) {
        }

        @Override
        public JoinPoint.StaticPart getStaticPart() {
            return null;
        }

        @Override
        public String getKind() {
            return ProceedingJoinPoint.METHOD_EXECUTION;
        }
    }

    private static final class StubAuditQueryProceedingJoinPoint implements ProceedingJoinPoint {

        private final Object[] arguments;
        private final MethodSignature signature;

        private StubAuditQueryProceedingJoinPoint(
                OffsetDateTime from,
                OffsetDateTime to,
                String actionCode,
                Long operatorUserId,
                Long patientUserId,
                Long encounterId,
                String resourceType,
                String resourceId,
                Boolean successFlag,
                String requestId) {
            this.arguments = new Object[] {
                from, to, actionCode, operatorUserId, patientUserId, encounterId, resourceType, resourceId, successFlag, requestId
            };
            this.signature = new StubAuditQueryMethodSignature();
        }

        @Override
        public Object proceed() {
            return null;
        }

        @Override
        public Object proceed(Object[] args) {
            return null;
        }

        @Override
        public Object[] getArgs() {
            return arguments;
        }

        @Override
        public Signature getSignature() {
            return signature;
        }

        @Override
        public SourceLocation getSourceLocation() {
            return null;
        }

        @Override
        public Object getThis() {
            return null;
        }

        @Override
        public Object getTarget() {
            return null;
        }

        @Override
        public String toShortString() {
            return "StubAuditQueryProceedingJoinPoint";
        }

        @Override
        public String toLongString() {
            return "StubAuditQueryProceedingJoinPoint";
        }

        @Override
        public void set$AroundClosure(org.aspectj.runtime.internal.AroundClosure arc) {
        }

        @Override
        public JoinPoint.StaticPart getStaticPart() {
            return null;
        }

        @Override
        public String getKind() {
            return ProceedingJoinPoint.METHOD_EXECUTION;
        }
    }

    private static final class StubMethodSignature implements MethodSignature {

        private final String[] parameterNames;

        private StubMethodSignature(String parameterName) {
            this.parameterNames = new String[] {parameterName};
        }

        @Override
        public String[] getParameterNames() {
            return parameterNames;
        }

        @Override
        public Class<?>[] getParameterTypes() {
            return new Class<?>[] {Long.class};
        }

        @Override
        public Class<?>[] getExceptionTypes() {
            return new Class<?>[0];
        }

        @Override
        public Method getMethod() {
            try {
                return TestScenarioController.class.getDeclaredMethod("readEmr", Long.class);
            } catch (NoSuchMethodException exception) {
                throw new IllegalStateException(exception);
            }
        }

        @Override
        public Class<?> getReturnType() {
            return Object.class;
        }

        @Override
        public String getName() {
            return "readEmr";
        }

        @Override
        public int getModifiers() {
            return getMethod().getModifiers();
        }

        @Override
        public Class<?> getDeclaringType() {
            return TestScenarioController.class;
        }

        @Override
        public String getDeclaringTypeName() {
            return TestScenarioController.class.getName();
        }

        @Override
        public String toShortString() {
            return "readEmr";
        }

        @Override
        public String toLongString() {
            return "readEmr";
        }

        @Override
        public String toString() {
            return "readEmr";
        }
    }

    private static final class StubAuditQueryMethodSignature implements MethodSignature {

        @Override
        public String[] getParameterNames() {
            return new String[] {
                "from",
                "to",
                "actionCode",
                "operatorUserId",
                "patientUserId",
                "encounterId",
                "resourceType",
                "resourceId",
                "successFlag",
                "requestId"
            };
        }

        @Override
        public Class<?>[] getParameterTypes() {
            return new Class<?>[] {
                OffsetDateTime.class,
                OffsetDateTime.class,
                String.class,
                Long.class,
                Long.class,
                Long.class,
                String.class,
                String.class,
                Boolean.class,
                String.class
            };
        }

        @Override
        public Class<?>[] getExceptionTypes() {
            return new Class<?>[0];
        }

        @Override
        public Method getMethod() {
            try {
                return TestScenarioController.class.getDeclaredMethod(
                        "queryAuditEvents",
                        OffsetDateTime.class,
                        OffsetDateTime.class,
                        String.class,
                        Long.class,
                        Long.class,
                        Long.class,
                        String.class,
                        String.class,
                        Boolean.class,
                        String.class);
            } catch (NoSuchMethodException exception) {
                throw new IllegalStateException(exception);
            }
        }

        @Override
        public Class<?> getReturnType() {
            return Object.class;
        }

        @Override
        public String toShortString() {
            return "StubAuditQueryMethodSignature";
        }

        @Override
        public String toLongString() {
            return "StubAuditQueryMethodSignature";
        }

        @Override
        public String getName() {
            return "queryAuditEvents";
        }

        @Override
        public int getModifiers() {
            return 0;
        }

        @Override
        public Class<?> getDeclaringType() {
            return TestScenarioController.class;
        }

        @Override
        public String getDeclaringTypeName() {
            return TestScenarioController.class.getName();
        }
    }
}
