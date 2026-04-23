package me.jianwen.mediask.infra.persistence.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import me.jianwen.mediask.domain.outpatient.model.RegistrationOrder;
import me.jianwen.mediask.infra.persistence.dataobject.RegistrationOrderDO;
import me.jianwen.mediask.infra.persistence.mapper.RegistrationOrderMapper;
import me.jianwen.mediask.infra.persistence.mapper.StatusTransitionLogMapper;
import org.junit.jupiter.api.Test;

class RegistrationOrderRepositoryAdapterTest {

    @Test
    void save_WhenCalled_InsertMappedRegistrationOrder() {
        CapturingHandler handler = new CapturingHandler();
        RegistrationOrderRepositoryAdapter adapter =
                new RegistrationOrderRepositoryAdapter(proxy(RegistrationOrderMapper.class, Map.of(
                        "insert", handler::insert,
                        "selectOne", handler::selectOne,
                        "selectById", handler::selectById,
                        "updateById", handler::updateById)), proxy(StatusTransitionLogMapper.class, Map.of("insert", handler::insertLog)));

        RegistrationOrder registrationOrder =
                RegistrationOrder.createConfirmed(2201L, 2101L, 3101L, 4101L, 5101L, new BigDecimal("18.00"));
        adapter.save(registrationOrder);

        assertEquals(registrationOrder.registrationId(), handler.inserted.getId());
        assertEquals(registrationOrder.orderNo(), handler.inserted.getOrderNo());
        assertEquals(registrationOrder.patientId(), handler.inserted.getPatientId());
        assertEquals(registrationOrder.doctorId(), handler.inserted.getDoctorId());
        assertEquals(registrationOrder.departmentId(), handler.inserted.getDepartmentId());
        assertEquals(registrationOrder.sessionId(), handler.inserted.getSessionId());
        assertEquals(registrationOrder.slotId(), handler.inserted.getSlotId());
        assertEquals("CONFIRMED", handler.inserted.getOrderStatus());
        assertEquals(new BigDecimal("18.00"), handler.inserted.getFee());
    }

    @Test
    void findByRegistrationIdAndPatientId_WhenFound_ReturnMappedOrder() {
        CapturingHandler handler = new CapturingHandler();
        RegistrationOrderRepositoryAdapter adapter =
                new RegistrationOrderRepositoryAdapter(proxy(RegistrationOrderMapper.class, Map.of(
                        "insert", handler::insert,
                        "selectOne", handler::selectOne,
                        "selectById", handler::selectById,
                        "updateById", handler::updateById)), proxy(StatusTransitionLogMapper.class, Map.of("insert", handler::insertLog)));

        Optional<RegistrationOrder> result = adapter.findByRegistrationIdAndPatientId(6101L, 2003L);

        assertTrue(result.isPresent());
        assertEquals(RegistrationOrder.reconstitute(
                        6101L,
                        "REG6101",
                        2003L,
                        2101L,
                        3101L,
                        4101L,
                        5101L,
                        me.jianwen.mediask.domain.outpatient.model.RegistrationStatus.CONFIRMED,
                        new BigDecimal("18.00"),
                        null,
                        null).status(),
                result.orElseThrow().status());
    }

    @Test
    void update_WhenCalled_UpdateMappedRegistrationOrder() {
        CapturingHandler handler = new CapturingHandler();
        RegistrationOrderRepositoryAdapter adapter =
                new RegistrationOrderRepositoryAdapter(proxy(RegistrationOrderMapper.class, Map.of(
                        "insert", handler::insert,
                        "selectOne", handler::selectOne,
                        "selectById", handler::selectById,
                        "updateById", handler::updateById)), proxy(StatusTransitionLogMapper.class, Map.of("insert", handler::insertLog)));

        RegistrationOrder registrationOrder = RegistrationOrder.reconstitute(
                6101L,
                "REG6101",
                2003L,
                2101L,
                3101L,
                4101L,
                5101L,
                me.jianwen.mediask.domain.outpatient.model.RegistrationStatus.CANCELLED,
                new BigDecimal("18.00"),
                OffsetDateTime.parse("2026-04-03T11:00:00+08:00"),
                null);
        adapter.update(registrationOrder);

        assertEquals("CANCELLED", handler.updated.getOrderStatus());
        assertEquals(7, handler.updated.getVersion());
        assertEquals(OffsetDateTime.parse("2026-04-03T11:00:00+08:00"), handler.updated.getCancelledAt());
    }

    @Test
    void completeConfirmedByRegistrationId_WhenConfirmed_UpdateCompletedStatus() {
        CapturingHandler handler = new CapturingHandler();
        RegistrationOrderRepositoryAdapter adapter =
                new RegistrationOrderRepositoryAdapter(proxy(RegistrationOrderMapper.class, Map.of(
                        "insert", handler::insert,
                        "selectOne", handler::selectOne,
                        "selectById", handler::selectById,
                        "updateById", handler::updateById)), proxy(StatusTransitionLogMapper.class, Map.of("insert", handler::insertLog)));

        boolean result = adapter.completeConfirmedByRegistrationId(6101L);

        assertTrue(result);
        assertEquals("COMPLETED", handler.updated.getOrderStatus());
        assertEquals(7, handler.updated.getVersion());
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
        private RegistrationOrderDO updated;

        private Object insert(Object[] arguments) {
            this.inserted = (RegistrationOrderDO) arguments[0];
            return 1;
        }

        private Object selectOne(Object[] arguments) {
            return buildExisting();
        }

        private Object selectById(Object[] arguments) {
            return buildExisting();
        }

        private Object updateById(Object[] arguments) {
            this.updated = (RegistrationOrderDO) arguments[0];
            return 1;
        }

        private Object insertLog(Object[] arguments) {
            return 1;
        }

        private RegistrationOrderDO buildExisting() {
            RegistrationOrderDO dataObject = new RegistrationOrderDO();
            dataObject.setId(6101L);
            dataObject.setVersion(7);
            dataObject.setOrderNo("REG6101");
            dataObject.setPatientId(2003L);
            dataObject.setDoctorId(2101L);
            dataObject.setDepartmentId(3101L);
            dataObject.setSessionId(4101L);
            dataObject.setSlotId(5101L);
            dataObject.setOrderStatus("CONFIRMED");
            dataObject.setFee(new BigDecimal("18.00"));
            return dataObject;
        }
    }
}
