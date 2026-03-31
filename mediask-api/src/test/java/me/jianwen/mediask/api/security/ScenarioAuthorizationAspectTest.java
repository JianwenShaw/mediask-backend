package me.jianwen.mediask.api.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import me.jianwen.mediask.application.authz.AuthzDecision;
import me.jianwen.mediask.application.authz.AuthzDecisionReason;
import me.jianwen.mediask.application.authz.AuthzInvocationContext;
import me.jianwen.mediask.application.authz.AuthorizeScenario;
import me.jianwen.mediask.application.authz.AuthorizationDecisionService;
import me.jianwen.mediask.application.authz.ScenarioCode;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.exception.ErrorCode;
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
        ScenarioAuthorizationAspect aspect = new ScenarioAuthorizationAspect(decisionService);
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
        ScenarioAuthorizationAspect aspect = new ScenarioAuthorizationAspect(decisionService);
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
        ScenarioAuthorizationAspect aspect = new ScenarioAuthorizationAspect(decisionService);
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
}
