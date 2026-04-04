package me.jianwen.mediask.infra.persistence.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import me.jianwen.mediask.domain.clinical.model.EncounterListItem;
import me.jianwen.mediask.domain.clinical.model.VisitEncounterStatus;
import me.jianwen.mediask.infra.persistence.mapper.VisitEncounterListRow;
import me.jianwen.mediask.infra.persistence.mapper.VisitEncounterMapper;
import org.junit.jupiter.api.Test;

class EncounterQueryRepositoryAdapterTest {

    @Test
    void listByDoctorId_WhenStatusProvided_MapRowsAndForwardFilter() {
        MapperHandler handler = new MapperHandler();
        EncounterQueryRepositoryAdapter adapter =
                new EncounterQueryRepositoryAdapter(proxy(VisitEncounterMapper.class, Map.of(
                        "selectDoctorEncounters", handler::selectDoctorEncounters)));

        List<EncounterListItem> result = adapter.listByDoctorId(2101L, VisitEncounterStatus.SCHEDULED);

        assertEquals(2101L, handler.lastDoctorId);
        assertEquals("SCHEDULED", handler.lastStatus);
        assertEquals(1, result.size());
        assertEquals(8101L, result.getFirst().encounterId());
        assertEquals(6101L, result.getFirst().registrationId());
        assertEquals("李患者", result.getFirst().patientName());
        assertEquals(VisitEncounterStatus.SCHEDULED, result.getFirst().encounterStatus());
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

    private static final class MapperHandler {

        private Long lastDoctorId;
        private String lastStatus;

        private Object selectDoctorEncounters(Object[] arguments) {
            this.lastDoctorId = (Long) arguments[0];
            this.lastStatus = (String) arguments[1];
            VisitEncounterListRow row = new VisitEncounterListRow();
            row.setEncounterId(8101L);
            row.setRegistrationId(6101L);
            row.setPatientUserId(2003L);
            row.setPatientName("李患者");
            row.setDepartmentId(3101L);
            row.setDepartmentName("心内科");
            row.setSessionDate(LocalDate.parse("2026-04-03"));
            row.setPeriodCode("MORNING");
            row.setEncounterStatus("SCHEDULED");
            row.setStartedAt(OffsetDateTime.parse("2026-04-03T09:00:00+08:00"));
            return List.of(row);
        }
    }
}
