package me.jianwen.mediask.infra.persistence.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.clinical.exception.ClinicalErrorCode;
import me.jianwen.mediask.domain.clinical.model.PrescriptionItem;
import me.jianwen.mediask.domain.clinical.model.PrescriptionOrder;
import me.jianwen.mediask.domain.clinical.model.PrescriptionStatus;
import me.jianwen.mediask.infra.persistence.dataobject.PrescriptionItemDO;
import me.jianwen.mediask.infra.persistence.dataobject.PrescriptionOrderDO;
import me.jianwen.mediask.infra.persistence.mapper.PrescriptionOrderMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

class PrescriptionOrderRepositoryAdapterTest {

    @Test
    void save_WhenCalled_InsertsOrderAndItems() {
        MapperHandler handler = new MapperHandler();
        PrescriptionOrderRepositoryAdapter adapter = new PrescriptionOrderRepositoryAdapter(
                proxy(PrescriptionOrderMapper.class, Map.of(
                        "insert", handler::insertOrder,
                        "insertItems", handler::insertItems)));

        adapter.save(prescriptionOrder());

        assertEquals("RX123456", handler.insertedOrder.getPrescriptionNo());
        assertEquals(1L, handler.insertedOrder.getEncounterId());
        assertEquals("DRAFT", handler.insertedOrder.getPrescriptionStatus());
        assertEquals(2, handler.insertedItems.size());
        assertEquals("阿莫西林胶囊", handler.insertedItems.getFirst().getDrugName());
    }

    @Test
    void existsByEncounterId_WhenExists_ReturnsTrue() {
        MapperHandler handler = new MapperHandler();
        handler.existsResult = true;
        PrescriptionOrderRepositoryAdapter adapter = new PrescriptionOrderRepositoryAdapter(
                proxy(PrescriptionOrderMapper.class, Map.of("existsByEncounterId", handler::existsByEncounterId)));

        assertTrue(adapter.existsByEncounterId(1L));
    }

    @Test
    void existsByEncounterId_WhenNotExists_ReturnsFalse() {
        MapperHandler handler = new MapperHandler();
        PrescriptionOrderRepositoryAdapter adapter = new PrescriptionOrderRepositoryAdapter(
                proxy(PrescriptionOrderMapper.class, Map.of("existsByEncounterId", handler::existsByEncounterId)));

        assertFalse(adapter.existsByEncounterId(1L));
    }

    @Test
    void findByEncounterId_WhenExists_ReturnsPrescriptionWithItems() {
        MapperHandler handler = new MapperHandler();
        handler.selectedOrder = new PrescriptionOrderDO();
        handler.selectedOrder.setId(7101L);
        handler.selectedOrder.setPrescriptionNo("RX123456");
        handler.selectedOrder.setRecordId(6102L);
        handler.selectedOrder.setEncounterId(8101L);
        handler.selectedOrder.setPatientId(1001L);
        handler.selectedOrder.setDoctorId(2101L);
        handler.selectedOrder.setPrescriptionStatus("DRAFT");
        handler.selectedOrder.setVersion(0);
        handler.selectedOrder.setCreatedAt(OffsetDateTime.parse("2026-04-18T10:00:00+08:00"));
        handler.selectedOrder.setUpdatedAt(OffsetDateTime.parse("2026-04-18T10:00:00+08:00"));

        PrescriptionItemDO item = new PrescriptionItemDO();
        item.setId(8101L);
        item.setPrescriptionId(7101L);
        item.setSortOrder(0);
        item.setDrugName("阿莫西林胶囊");
        item.setDosageText("每次2粒");
        item.setFrequencyText("每日3次");
        item.setDurationText("5天");
        item.setQuantity(new BigDecimal("30"));
        item.setUnit("粒");
        item.setRoute("口服");
        handler.selectedItems = List.of(item);

        PrescriptionOrderRepositoryAdapter adapter = new PrescriptionOrderRepositoryAdapter(
                proxy(PrescriptionOrderMapper.class, Map.of(
                        "selectByEncounterId", handler::selectByEncounterId,
                        "selectItemsByPrescriptionId", handler::selectItemsByPrescriptionId)));

        PrescriptionOrder result = adapter.findByEncounterId(8101L).orElseThrow();

        assertEquals(PrescriptionStatus.DRAFT, result.prescriptionStatus());
        assertEquals("阿莫西林胶囊", result.items().getFirst().drugName());
    }

    @Test
    void save_WhenEncounterDuplicate_ThrowsConflictBizException() {
        PrescriptionOrderRepositoryAdapter adapter = new PrescriptionOrderRepositoryAdapter(
                proxy(PrescriptionOrderMapper.class, Map.of(
                        "insert", arguments -> {
                            throw new DuplicateKeyException("duplicate key value violates unique constraint \"uk_prescription_order_encounter\"");
                        })));

        BizException exception = assertThrows(BizException.class, () -> adapter.save(prescriptionOrder()));

        assertEquals(ClinicalErrorCode.PRESCRIPTION_ALREADY_EXISTS.getCode(), exception.getCode());
    }

    private PrescriptionOrder prescriptionOrder() {
        return new PrescriptionOrder(
                7101L,
                "RX123456",
                6102L,
                1L,
                100L,
                200L,
                PrescriptionStatus.DRAFT,
                List.of(
                        new PrescriptionItem(8101L, 0, "阿莫西林胶囊", "0.25g*24粒", "每次2粒", "每日3次", "5天", new BigDecimal("30"), "粒", "口服"),
                        new PrescriptionItem(8102L, 1, "对乙酰氨基酚片", null, "每次1片", "必要时", "3天", new BigDecimal("10"), "片", "口服")),
                0,
                java.time.Instant.parse("2026-04-18T02:00:00Z"),
                java.time.Instant.parse("2026-04-18T02:00:00Z"));
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
        private PrescriptionOrderDO insertedOrder;
        private List<PrescriptionItemDO> insertedItems = List.of();
        private boolean existsResult;
        private PrescriptionOrderDO selectedOrder;
        private List<PrescriptionItemDO> selectedItems = List.of();

        private Object insertOrder(Object[] arguments) {
            insertedOrder = (PrescriptionOrderDO) arguments[0];
            return 1;
        }

        @SuppressWarnings("unchecked")
        private Object insertItems(Object[] arguments) {
            insertedItems = (List<PrescriptionItemDO>) arguments[0];
            return insertedItems.size();
        }

        private Object existsByEncounterId(Object[] arguments) {
            return existsResult;
        }

        private Object selectByEncounterId(Object[] arguments) {
            return Optional.ofNullable(selectedOrder);
        }

        private Object selectItemsByPrescriptionId(Object[] arguments) {
            return selectedItems;
        }
    }
}
