package me.jianwen.mediask.infra.persistence.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import me.jianwen.mediask.domain.outpatient.model.RegistrationListItem;
import me.jianwen.mediask.domain.outpatient.model.RegistrationStatus;
import me.jianwen.mediask.infra.persistence.dataobject.RegistrationOrderDO;
import me.jianwen.mediask.infra.persistence.mapper.RegistrationOrderMapper;
import org.junit.jupiter.api.Test;

class RegistrationOrderQueryRepositoryAdapterTest {

    @Test
    void listByPatientId_WhenStatusProvided_ReturnMappedItems() {
        CapturingHandler handler = new CapturingHandler();
        RegistrationOrderQueryRepositoryAdapter adapter = new RegistrationOrderQueryRepositoryAdapter(
                proxy(RegistrationOrderMapper.class, Map.of("selectList", handler::selectList)));

        List<RegistrationListItem> result = adapter.listByPatientId(2201L, RegistrationStatus.CONFIRMED);

        assertEquals(1, result.size());
        assertEquals(6101L, result.getFirst().registrationId());
        assertEquals("REG6101", result.getFirst().orderNo());
        assertEquals(RegistrationStatus.CONFIRMED, result.getFirst().status());
        assertEquals(7101L, result.getFirst().sourceAiSessionId());
        assertEquals(1, handler.selectListInvocations);
    }

    private static <T> T proxy(Class<T> type, Map<String, Function<Object[], Object>> handlers) {
        Object proxyInstance = Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> type.getSimpleName() + "TestProxy";
                    default -> throw new UnsupportedOperationException(method.getName());
                };
            }
            Function<Object[], Object> handler = handlers.get(method.getName());
            if (handler == null) {
                throw new AssertionError("No test handler registered for " + type.getSimpleName() + "#" + method.getName());
            }
            return handler.apply(args == null ? new Object[0] : args);
        });
        return type.cast(proxyInstance);
    }

    private static final class CapturingHandler {

        private int selectListInvocations;

        private Object selectList(Object[] arguments) {
            this.selectListInvocations++;

            RegistrationOrderDO dataObject = new RegistrationOrderDO();
            dataObject.setId(6101L);
            dataObject.setOrderNo("REG6101");
            dataObject.setOrderStatus("CONFIRMED");
            dataObject.setCreatedAt(OffsetDateTime.parse("2026-04-02T10:00:00+08:00"));
            dataObject.setSourceAiSessionId(7101L);
            dataObject.setFee(new BigDecimal("18.00"));
            return List.of(dataObject);
        }
    }
}
