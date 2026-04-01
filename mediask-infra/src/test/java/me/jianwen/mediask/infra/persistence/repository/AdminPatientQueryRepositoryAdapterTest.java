package me.jianwen.mediask.infra.persistence.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import me.jianwen.mediask.common.pagination.PageData;
import me.jianwen.mediask.common.pagination.PageQuery;
import me.jianwen.mediask.domain.user.model.AdminPatientDetail;
import me.jianwen.mediask.domain.user.model.AdminPatientListItem;
import me.jianwen.mediask.infra.persistence.mapper.AdminPatientRow;
import me.jianwen.mediask.infra.persistence.mapper.PatientProfileMapper;
import org.junit.jupiter.api.Test;

class AdminPatientQueryRepositoryAdapterTest {

    @Test
    void pageByKeyword_WhenRowsReturned_MapListItems() {
        AdminPatientQueryRepositoryAdapter adapter = new AdminPatientQueryRepositoryAdapter(proxy(PatientProfileMapper.class, Map.of(
                "selectAdminPatientsByKeywordPage", arguments -> new Page<AdminPatientRow>(1L, 20L, 1L)
                        .setRecords(List.of(createRow())))));

        PageData<AdminPatientListItem> result = adapter.pageByKeyword("patient", new PageQuery(1L, 20L));

        assertEquals(1, result.items().size());
        assertEquals("patient_new", result.items().getFirst().username());
        assertEquals("ACTIVE", result.items().getFirst().accountStatus());
    }

    @Test
    void pageByKeyword_WhenPagedResultReturned_MapPagingMetadata() {
        AdminPatientQueryRepositoryAdapter adapter = new AdminPatientQueryRepositoryAdapter(proxy(PatientProfileMapper.class, Map.of(
                "selectAdminPatientsByKeywordPage", arguments -> new Page<AdminPatientRow>(2L, 20L, 45L)
                        .setRecords(List.of(createRow())))));

        PageData<AdminPatientListItem> result = adapter.pageByKeyword("patient", new PageQuery(2L, 20L));

        assertEquals(2L, result.pageNum());
        assertEquals(20L, result.pageSize());
        assertEquals(45L, result.total());
        assertEquals(3L, result.totalPages());
        assertTrue(result.hasNext());
    }

    @Test
    void findDetailByPatientId_WhenRowReturned_MapDetail() {
        AdminPatientQueryRepositoryAdapter adapter = new AdminPatientQueryRepositoryAdapter(proxy(PatientProfileMapper.class, Map.of(
                "selectAdminPatientByPatientId", arguments -> createRow())));

        Optional<AdminPatientDetail> result = adapter.findDetailByPatientId(2208L);

        assertTrue(result.isPresent());
        assertEquals("Peanut", result.orElseThrow().allergySummary());
    }

    private static AdminPatientRow createRow() {
        AdminPatientRow row = new AdminPatientRow();
        row.setPatientId(2208L);
        row.setUserId(2008L);
        row.setPatientNo("P20260008");
        row.setCreatedAt(OffsetDateTime.parse("2026-03-31T10:15:30+08:00"));
        row.setUsername("patient_new");
        row.setDisplayName("李新患者");
        row.setMobileMasked("137****1234");
        row.setGender("FEMALE");
        row.setBirthDate(LocalDate.of(1995, 6, 1));
        row.setBloodType("A");
        row.setAllergySummary("Peanut");
        row.setAccountStatus("ACTIVE");
        return row;
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
}
