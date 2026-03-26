package me.jianwen.mediask.api.security;

import java.util.LinkedHashMap;
import java.util.Map;
import me.jianwen.mediask.application.authz.AuthzDecision;
import me.jianwen.mediask.application.authz.AuthzInvocationContext;
import me.jianwen.mediask.application.authz.AuthorizeScenario;
import me.jianwen.mediask.application.authz.AuthorizationDecisionService;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.exception.ErrorCode;
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

    public ScenarioAuthorizationAspect(AuthorizationDecisionService authorizationDecisionService) {
        this.authorizationDecisionService = authorizationDecisionService;
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
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        return joinPoint.proceed();
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
        return Map.copyOf(mappedArguments);
    }
}
