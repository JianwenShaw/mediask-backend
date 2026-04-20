package me.jianwen.mediask.infra.persistence.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import me.jianwen.mediask.domain.outpatient.model.ClinicSessionListItem;
import me.jianwen.mediask.domain.outpatient.model.ClinicSessionSlotListItem;
import me.jianwen.mediask.infra.persistence.mapper.ClinicSessionListRow;
import me.jianwen.mediask.infra.persistence.mapper.ClinicSessionMapper;
import me.jianwen.mediask.infra.persistence.mapper.ClinicSessionSlotListRow;
import me.jianwen.mediask.infra.persistence.mapper.ClinicSlotMapper;
import org.junit.jupiter.api.Test;

class ClinicSessionQueryRepositoryAdapterTest {

    @Test
    void listOpenSessions_WhenFiltersProvided_MapRowsAndForwardArguments() {
        CapturingHandler handler = new CapturingHandler();
        ClinicSessionQueryRepositoryAdapter adapter =
                new ClinicSessionQueryRepositoryAdapter(
                        proxy(ClinicSessionMapper.class, Map.of("selectOpenClinicSessions", handler::handle)),
                        proxy(ClinicSlotMapper.class, Map.of("selectAvailableSlotsBySessionId", handler::handleSlots)));

        List<ClinicSessionListItem> result =
                adapter.listOpenSessions(3101L, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 7));

        assertEquals(3101L, handler.lastDepartmentId);
        assertEquals(LocalDate.of(2026, 4, 1), handler.lastDateFrom);
        assertEquals(LocalDate.of(2026, 4, 7), handler.lastDateTo);
        assertEquals(2, result.size());
        assertEquals("呼吸内科", result.getFirst().departmentName());
        assertEquals("MORNING", result.getFirst().periodCode().name());
        assertEquals(2, result.getFirst().remainingCount());
        assertEquals("EXPERT", result.get(1).clinicType().name());
        assertEquals(1, result.get(1).remainingCount());
    }

    @Test
    void listAvailableSlotsBySessionId_WhenClinicSessionProvided_MapRowsAndForwardArguments() {
        CapturingHandler handler = new CapturingHandler();
        ClinicSessionQueryRepositoryAdapter adapter =
                new ClinicSessionQueryRepositoryAdapter(
                        proxy(ClinicSessionMapper.class, Map.of("selectOpenClinicSessions", handler::handle)),
                        proxy(ClinicSlotMapper.class, Map.of("selectAvailableSlotsBySessionId", handler::handleSlots)));

        List<ClinicSessionSlotListItem> result = adapter.listAvailableSlotsBySessionId(4101L);

        assertEquals(4101L, handler.lastClinicSessionId);
        assertEquals(2, result.size());
        assertEquals(5101L, result.getFirst().clinicSlotId());
        assertEquals(1, result.getFirst().slotSeq());
        assertEquals(OffsetDateTime.parse("2026-04-01T08:30:00+08:00"), result.getFirst().slotStartTime());
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

        private Long lastDepartmentId;
        private LocalDate lastDateFrom;
        private LocalDate lastDateTo;
        private Long lastClinicSessionId;

        private Object handle(Object[] arguments) {
            this.lastDepartmentId = (Long) arguments[0];
            this.lastDateFrom = (LocalDate) arguments[1];
            this.lastDateTo = (LocalDate) arguments[2];
            return List.of(row(4101L, "呼吸内科", "张医生", LocalDate.of(2026, 4, 1), "MORNING", "GENERAL", 2, "18.00"),
                    row(4102L, "呼吸内科", "李主任", LocalDate.of(2026, 4, 1), "AFTERNOON", "EXPERT", 1, "60.00"));
        }

        private Object handleSlots(Object[] arguments) {
            this.lastClinicSessionId = (Long) arguments[0];
            return List.of(slotRow(5101L, 1, "2026-04-01T08:30:00+08:00", "2026-04-01T08:40:00+08:00"),
                    slotRow(5102L, 2, "2026-04-01T08:40:00+08:00", "2026-04-01T08:50:00+08:00"));
        }

        private ClinicSessionListRow row(
                Long clinicSessionId,
                String departmentName,
                String doctorName,
                LocalDate sessionDate,
                String periodCode,
                String clinicType,
                Integer remainingCount,
                String fee) {
            ClinicSessionListRow row = new ClinicSessionListRow();
            row.setClinicSessionId(clinicSessionId);
            row.setDepartmentId(3101L);
            row.setDepartmentName(departmentName);
            row.setDoctorId(2101L);
            row.setDoctorName(doctorName);
            row.setSessionDate(sessionDate);
            row.setPeriodCode(periodCode);
            row.setClinicType(clinicType);
            row.setRemainingCount(remainingCount);
            row.setFee(new BigDecimal(fee));
            return row;
        }

        private ClinicSessionSlotListRow slotRow(Long clinicSlotId, Integer slotSeq, String slotStartTime, String slotEndTime) {
            ClinicSessionSlotListRow row = new ClinicSessionSlotListRow();
            row.setClinicSlotId(clinicSlotId);
            row.setSlotSeq(slotSeq);
            row.setSlotStartTime(OffsetDateTime.parse(slotStartTime));
            row.setSlotEndTime(OffsetDateTime.parse(slotEndTime));
            return row;
        }
    }
}
