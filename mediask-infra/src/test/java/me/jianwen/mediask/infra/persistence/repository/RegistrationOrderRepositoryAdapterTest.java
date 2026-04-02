package me.jianwen.mediask.infra.persistence.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.Map;
import java.util.function.Function;
import me.jianwen.mediask.domain.outpatient.model.RegistrationOrder;
import me.jianwen.mediask.infra.persistence.dataobject.RegistrationOrderDO;
import me.jianwen.mediask.infra.persistence.mapper.RegistrationOrderMapper;
import org.junit.jupiter.api.Test;

class RegistrationOrderRepositoryAdapterTest {

    @Test
    void save_WhenCalled_InsertMappedRegistrationOrder() {
        CapturingHandler handler = new CapturingHandler();
        RegistrationOrderRepositoryAdapter adapter =
                new RegistrationOrderRepositoryAdapter(proxy(RegistrationOrderMapper.class, Map.of("insert", handler::insert)));

        RegistrationOrder registrationOrder = RegistrationOrder.createPendingPayment(
                2201L, 2101L, 3101L, 4101L, 5101L, 7101L, new BigDecimal("18.00"));
        adapter.save(registrationOrder);

        assertEquals(registrationOrder.registrationId(), handler.inserted.getId());
        assertEquals(registrationOrder.orderNo(), handler.inserted.getOrderNo());
        assertEquals(registrationOrder.patientId(), handler.inserted.getPatientId());
        assertEquals(registrationOrder.doctorId(), handler.inserted.getDoctorId());
        assertEquals(registrationOrder.departmentId(), handler.inserted.getDepartmentId());
        assertEquals(registrationOrder.sessionId(), handler.inserted.getSessionId());
        assertEquals(registrationOrder.slotId(), handler.inserted.getSlotId());
        assertEquals(registrationOrder.sourceAiSessionId(), handler.inserted.getSourceAiSessionId());
        assertEquals("PENDING_PAYMENT", handler.inserted.getOrderStatus());
        assertEquals(new BigDecimal("18.00"), handler.inserted.getFee());
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

        private RegistrationOrderDO inserted;

        private Object insert(Object[] arguments) {
            this.inserted = (RegistrationOrderDO) arguments[0];
            return 1;
        }
    }
}
