package me.jianwen.mediask.api.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.List;
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
import org.aspectj.lang.ProceedingJoinPoint;
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
        AuthorizationDecisionService decisionService = mock(AuthorizationDecisionService.class);
        ScenarioAuthorizationAspect aspect = new ScenarioAuthorizationAspect(decisionService);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("plain-principal", null, "ROLE_USER"));

        BizException exception = assertThrows(
                BizException.class,
                () -> aspect.authorize(mock(ProceedingJoinPoint.class), authorizeScenario()));

        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
    }

    @Test
    void authorize_WhenDecisionDenied_ThrowForbidden() throws Throwable {
        AuthorizationDecisionService decisionService = mock(AuthorizationDecisionService.class);
        ScenarioAuthorizationAspect aspect = new ScenarioAuthorizationAspect(decisionService);
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(principal(), null, "ROLE_PATIENT"));
        when(decisionService.decide(any(AuthzInvocationContext.class)))
                .thenReturn(AuthzDecision.deny(AuthzDecisionReason.OBJECT_SCOPE_DENIED));

        BizException exception = assertThrows(
                BizException.class,
                () -> aspect.authorize(mockJoinPoint(1001L, "id", "ok"), authorizeScenario()));

        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
    }

    @Test
    void authorize_WhenDecisionAllowed_ProceedAndMapAuthzSubject() throws Throwable {
        AuthorizationDecisionService decisionService = mock(AuthorizationDecisionService.class);
        ScenarioAuthorizationAspect aspect = new ScenarioAuthorizationAspect(decisionService);
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(principal(), null, "ROLE_PATIENT"));
        when(decisionService.decide(any(AuthzInvocationContext.class))).thenReturn(AuthzDecision.allow());

        Object result = aspect.authorize(mockJoinPoint(1001L, "id", "ok"), authorizeScenario());

        assertEquals("ok", result);
        verify(decisionService)
                .decide(argThat(context -> context.scenarioCode() == ScenarioCode.EMR_RECORD_READ
                        && context.subject().userId().equals(2003L)
                        && context.subject().hasPermission("emr:read")
                        && context.arguments().containsKey("id")
                        && context.arguments().get("id").equals(1001L)));
    }

    private static ProceedingJoinPoint mockJoinPoint(Long resourceId, String parameterName, Object proceedResult)
            throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getParameterNames()).thenReturn(new String[] {parameterName});
        when(joinPoint.getArgs()).thenReturn(new Object[] {resourceId});
        when(joinPoint.proceed()).thenReturn(proceedResult);
        return joinPoint;
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
}
