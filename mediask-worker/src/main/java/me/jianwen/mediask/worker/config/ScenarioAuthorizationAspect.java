package me.jianwen.mediask.worker.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import me.jianwen.mediask.application.authz.AuthzDecision;
import me.jianwen.mediask.application.authz.AuthzInvocationContext;
import me.jianwen.mediask.application.authz.AuthzSubject;
import me.jianwen.mediask.application.authz.AuthzSubjectPrincipal;
import me.jianwen.mediask.application.authz.AuthorizeScenario;
import me.jianwen.mediask.application.authz.AuthorizationDecisionService;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.exception.ErrorCode;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
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
        AuthzSubject subject = resolveSubject(joinPoint.getArgs()).orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));
        AuthzDecision decision = authorizationDecisionService.decide(new AuthzInvocationContext(
                authorizeScenario.value(),
                subject,
                resolveArguments(joinPoint)));
        if (!decision.allowed()) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        return joinPoint.proceed();
    }

    private Optional<AuthzSubject> resolveSubject(Object[] arguments) {
        for (Object argument : arguments) {
            if (argument instanceof AuthzSubjectPrincipal principal) {
                return Optional.of(principal.toAuthzSubject());
            }
            if (argument instanceof AuthzSubject subject) {
                return Optional.of(subject);
            }
        }
        return Optional.empty();
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
