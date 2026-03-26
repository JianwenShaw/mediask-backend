package me.jianwen.mediask.infra.security.authz;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.function.Function;
import me.jianwen.mediask.infra.persistence.mapper.DataScopeRuleMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

class DataScopeCustomRuleStartupGuardTest {

    @Test
    void run_WhenNoActiveCustomRule_DoNothing() {
        DataScopeCustomRuleStartupGuard guard = new DataScopeCustomRuleStartupGuard(
                proxy(DataScopeRuleMapper.class, Map.of("countActiveCustomRules", arguments -> 0L)));

        assertDoesNotThrow(() -> guard.run(new DefaultApplicationArguments(new String[0])));
    }

    @Test
    void run_WhenActiveCustomRuleExists_ThrowException() {
        DataScopeCustomRuleStartupGuard guard = new DataScopeCustomRuleStartupGuard(
                proxy(DataScopeRuleMapper.class, Map.of("countActiveCustomRules", arguments -> 2L)));

        assertThrows(IllegalStateException.class, () -> guard.run(new DefaultApplicationArguments(new String[0])));
    }

    private static <T> T proxy(Class<T> type, Map<String, Function<Object[], Object>> handlers) {
        Object proxyInstance = Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return handleObjectMethod(proxy, method.getName(), args);
            }
            Function<Object[], Object> handler = handlers.get(method.getName());
            if (handler == null) {
                throw new UnsupportedOperationException(type.getSimpleName() + "#" + method.getName());
            }
            return handler.apply(args == null ? new Object[0] : args);
        });
        return type.cast(proxyInstance);
    }

    private static Object handleObjectMethod(Object proxy, String methodName, Object[] args) {
        return switch (methodName) {
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            case "toString" -> proxy.getClass().getInterfaces()[0].getSimpleName() + "TestProxy";
            default -> throw new UnsupportedOperationException(methodName);
        };
    }
}
