package me.jianwen.mediask.infra.persistence.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import me.jianwen.mediask.domain.outpatient.model.RegistrationDetail;
import me.jianwen.mediask.domain.outpatient.model.RegistrationListItem;
import me.jianwen.mediask.domain.outpatient.model.RegistrationStatus;
import me.jianwen.mediask.infra.persistence.mapper.RegistrationDetailRow;
import me.jianwen.mediask.infra.persistence.dataobject.RegistrationOrderDO;
import me.jianwen.mediask.infra.persistence.mapper.RegistrationOrderMapper;
import org.junit.jupiter.api.Test;

class RegistrationOrderQueryRepositoryAdapterTest {

    @Test
    void listByPatientUserId_WhenStatusProvided_ReturnMappedItems() {
        CapturingHandler handler = new CapturingHandler();
        RegistrationOrderQueryRepositoryAdapter adapter = new RegistrationOrderQueryRepositoryAdapter(
                proxy(RegistrationOrderMapper.class, Map.of(
                        "selectList", handler::selectList,
                        "selectRegistrationDetail", handler::selectRegistrationDetail)));

        List<RegistrationListItem> result = adapter.listByPatientUserId(2003L, RegistrationStatus.CONFIRMED);

        assertEquals(1, result.size());
        assertEquals(6101L, result.getFirst().registrationId());
        assertEquals("REG6101", result.getFirst().orderNo());
        assertEquals(RegistrationStatus.CONFIRMED, result.getFirst().status());
        assertEquals(1, handler.selectListInvocations);
    }

    @Test
    void listByPatientUserId_WhenStatusAbsent_ReturnMappedItems() {
        CapturingHandler handler = new CapturingHandler();
        RegistrationOrderQueryRepositoryAdapter adapter = new RegistrationOrderQueryRepositoryAdapter(
                proxy(RegistrationOrderMapper.class, Map.of(
                        "selectList", handler::selectList,
                        "selectRegistrationDetail", handler::selectRegistrationDetail)));

        List<RegistrationListItem> result = adapter.listByPatientUserId(2003L, null);

        assertEquals(1, result.size());
        assertEquals(6101L, result.getFirst().registrationId());
        assertEquals(RegistrationStatus.CONFIRMED, result.getFirst().status());
        assertEquals(1, handler.selectListInvocations);
    }

    @Test
    void findDetailByPatientUserIdAndRegistrationId_WhenFound_ReturnMappedDetail() {
        CapturingHandler handler = new CapturingHandler();
        RegistrationOrderQueryRepositoryAdapter adapter = new RegistrationOrderQueryRepositoryAdapter(
                proxy(RegistrationOrderMapper.class, Map.of(
                        "selectList", handler::selectList,
                        "selectRegistrationDetail", handler::selectRegistrationDetail)));

        Optional<RegistrationDetail> result = adapter.findDetailByPatientUserIdAndRegistrationId(2003L, 6101L);

        assertEquals("REG6101", result.orElseThrow().orderNo());
        assertEquals("张医生", result.orElseThrow().doctorName());
        assertEquals(RegistrationStatus.CONFIRMED, result.orElseThrow().status());
    }

    @Test
    void findDetailByPatientUserIdAndRegistrationId_WhenRelatedRowsSoftDeleted_ReturnMappedDetailWithNullableDisplayFields() {
        CapturingHandler handler = new CapturingHandler();
        handler.returnSoftDeletedProjection = true;
        RegistrationOrderQueryRepositoryAdapter adapter = new RegistrationOrderQueryRepositoryAdapter(
                proxy(RegistrationOrderMapper.class, Map.of(
                        "selectList", handler::selectList,
                        "selectRegistrationDetail", handler::selectRegistrationDetail)));

        Optional<RegistrationDetail> result = adapter.findDetailByPatientUserIdAndRegistrationId(2003L, 6101L);

        assertEquals("REG6101", result.orElseThrow().orderNo());
        assertEquals(3101L, result.orElseThrow().departmentId());
        assertEquals(2101L, result.orElseThrow().doctorId());
        assertEquals(null, result.orElseThrow().departmentName());
        assertEquals(null, result.orElseThrow().doctorName());
        assertEquals(null, result.orElseThrow().sessionDate());
        assertEquals(null, result.orElseThrow().periodCode());
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
        private boolean returnSoftDeletedProjection;

        private Object selectList(Object[] arguments) {
            this.selectListInvocations++;

            RegistrationOrderDO dataObject = new RegistrationOrderDO();
            dataObject.setId(6101L);
            dataObject.setOrderNo("REG6101");
            dataObject.setOrderStatus("CONFIRMED");
            dataObject.setCreatedAt(OffsetDateTime.parse("2026-04-02T10:00:00+08:00"));
            dataObject.setFee(new BigDecimal("18.00"));
            return List.of(dataObject);
        }

        private Object selectRegistrationDetail(Object[] arguments) {
            RegistrationDetailRow row = new RegistrationDetailRow();
            row.setRegistrationId(6101L);
            row.setPatientUserId(2003L);
            row.setOrderNo("REG6101");
            row.setOrderStatus("CONFIRMED");
            row.setCreatedAt(OffsetDateTime.parse("2026-04-02T10:00:00+08:00"));
            row.setClinicSessionId(4101L);
            row.setClinicSlotId(5101L);
            row.setDepartmentId(3101L);
            row.setDoctorId(2101L);
            if (!returnSoftDeletedProjection) {
                row.setDepartmentName("神经内科");
                row.setDoctorName("张医生");
                row.setSessionDate(java.time.LocalDate.parse("2026-04-03"));
                row.setPeriodCode("MORNING");
            }
            row.setFee(new BigDecimal("18.00"));
            return row;
        }
    }
}
