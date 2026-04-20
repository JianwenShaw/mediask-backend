package me.jianwen.mediask.infra.persistence.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.function.Function;
import me.jianwen.mediask.domain.clinical.model.VisitEncounter;
import me.jianwen.mediask.infra.persistence.dataobject.VisitEncounterDO;
import me.jianwen.mediask.infra.persistence.mapper.VisitEncounterMapper;
import org.junit.jupiter.api.Test;

class VisitEncounterRepositoryAdapterTest {

    @Test
    void save_WhenCalled_InsertMappedVisitEncounter() {
        CapturingHandler handler = new CapturingHandler();
        VisitEncounterRepositoryAdapter adapter =
                new VisitEncounterRepositoryAdapter(proxy(VisitEncounterMapper.class, Map.of(
                        "insert", handler::insert,
                        "selectOne", handler::selectOne,
                        "updateById", handler::updateById)));

        VisitEncounter visitEncounter = VisitEncounter.createScheduled(6101L, 2003L, 2101L, 3101L);
        adapter.save(visitEncounter);

        assertEquals(visitEncounter.encounterId(), handler.inserted.getId());
        assertEquals(visitEncounter.registrationId(), handler.inserted.getOrderId());
        assertEquals(visitEncounter.patientUserId(), handler.inserted.getPatientId());
        assertEquals(visitEncounter.doctorId(), handler.inserted.getDoctorId());
        assertEquals(visitEncounter.departmentId(), handler.inserted.getDepartmentId());
        assertEquals("SCHEDULED", handler.inserted.getEncounterStatus());
    }

    @Test
    void cancelScheduledByRegistrationId_WhenScheduled_UpdateCancelledStatus() {
        CapturingHandler handler = new CapturingHandler();
        VisitEncounterRepositoryAdapter adapter =
                new VisitEncounterRepositoryAdapter(proxy(VisitEncounterMapper.class, Map.of(
                        "insert", handler::insert,
                        "selectOne", handler::selectOne,
                        "updateById", handler::updateById)));

        boolean result = adapter.cancelScheduledByRegistrationId(6101L);

        assertTrue(result);
        assertEquals("CANCELLED", handler.updated.getEncounterStatus());
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

        private VisitEncounterDO inserted;
        private VisitEncounterDO updated;

        private Object insert(Object[] arguments) {
            this.inserted = (VisitEncounterDO) arguments[0];
            return 1;
        }

        private Object selectOne(Object[] arguments) {
            VisitEncounterDO dataObject = new VisitEncounterDO();
            dataObject.setId(8101L);
            dataObject.setVersion(5);
            dataObject.setOrderId(6101L);
            dataObject.setEncounterStatus("SCHEDULED");
            return dataObject;
        }

        private Object updateById(Object[] arguments) {
            this.updated = (VisitEncounterDO) arguments[0];
            return 1;
        }
    }
}
