package me.jianwen.mediask.infra.persistence.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.outpatient.exception.OutpatientErrorCode;
import me.jianwen.mediask.domain.outpatient.model.ClinicSlotReservation;
import me.jianwen.mediask.infra.persistence.dataobject.ClinicSessionDO;
import me.jianwen.mediask.infra.persistence.dataobject.ClinicSlotDO;
import me.jianwen.mediask.infra.persistence.mapper.ClinicSessionMapper;
import me.jianwen.mediask.infra.persistence.mapper.ClinicSlotMapper;
import me.jianwen.mediask.infra.persistence.mapper.ClinicSlotReservationRow;
import me.jianwen.mediask.infra.persistence.mapper.StatusTransitionLogMapper;
import org.junit.jupiter.api.Test;

class ClinicSlotReservationRepositoryAdapterTest {

    @Test
    void existsOpenSession_WhenCountPositive_ReturnTrue() {
        CountingSessionHandler sessionHandler = new CountingSessionHandler();
        ClinicSlotReservationRepositoryAdapter adapter =
                new ClinicSlotReservationRepositoryAdapter(proxy(ClinicSessionMapper.class, Map.of(
                        "selectCount", sessionHandler::selectCount)), proxy(ClinicSlotMapper.class, Map.of()),
                        proxy(StatusTransitionLogMapper.class, Map.of("insert", sessionHandler::insertLog)));

        assertTrue(adapter.existsOpenSession(4101L));
    }

    @Test
    void reserveAvailableSlot_WhenSlotCanBeLocked_ReturnReservation() {
        SessionMapperHandler sessionHandler = new SessionMapperHandler();
        SlotMapperHandler slotHandler = new SlotMapperHandler();
        ClinicSlotReservationRepositoryAdapter adapter = new ClinicSlotReservationRepositoryAdapter(
                proxy(ClinicSessionMapper.class, Map.of(
                        "selectCount", sessionHandler::selectCount,
                        "selectOne", sessionHandler::selectOne,
                        "updateById", sessionHandler::updateById)),
                proxy(ClinicSlotMapper.class, Map.of(
                        "selectReservableSlot", slotHandler::selectReservableSlot,
                        "selectOne", slotHandler::selectOne,
                        "updateById", slotHandler::updateById,
                        "countAvailableSlots", slotHandler::countAvailableSlots)),
                proxy(StatusTransitionLogMapper.class, Map.of("insert", slotHandler::insertLog)));

        Optional<ClinicSlotReservation> result = adapter.reserveAvailableSlot(4101L, 5101L);

        assertTrue(result.isPresent());
        assertEquals(4101L, result.orElseThrow().sessionId());
        assertEquals(2101L, result.orElseThrow().doctorId());
        assertEquals("BOOKED", slotHandler.updatedSlotStatus);
        assertEquals(0, slotHandler.updatedRemainingCount);
    }

    @Test
    void reserveAvailableSlot_WhenOptimisticLockFails_ReturnEmpty() {
        SessionMapperHandler sessionHandler = new SessionMapperHandler();
        SlotMapperHandler slotHandler = new SlotMapperHandler();
        slotHandler.updateRows = 0;
        ClinicSlotReservationRepositoryAdapter adapter = new ClinicSlotReservationRepositoryAdapter(
                proxy(ClinicSessionMapper.class, Map.of(
                        "selectCount", sessionHandler::selectCount,
                        "selectOne", sessionHandler::selectOne,
                        "updateById", sessionHandler::updateById)),
                proxy(ClinicSlotMapper.class, Map.of(
                        "selectReservableSlot", slotHandler::selectReservableSlot,
                        "selectOne", slotHandler::selectOne,
                        "updateById", slotHandler::updateById,
                        "countAvailableSlots", slotHandler::countAvailableSlots)),
                proxy(StatusTransitionLogMapper.class, Map.of("insert", slotHandler::insertLog)));

        assertFalse(adapter.reserveAvailableSlot(4101L, 5101L).isPresent());
    }

    @Test
    void refreshSessionRemainingCount_WhenSessionUpdateConflicts_ThrowBizException() {
        SessionMapperHandler sessionHandler = new SessionMapperHandler();
        sessionHandler.updateRows = 0;
        SlotMapperHandler slotHandler = new SlotMapperHandler();
        ClinicSlotReservationRepositoryAdapter adapter = new ClinicSlotReservationRepositoryAdapter(
                proxy(ClinicSessionMapper.class, Map.of(
                        "selectCount", sessionHandler::selectCount,
                        "selectOne", sessionHandler::selectOne,
                        "updateById", sessionHandler::updateById)),
                proxy(ClinicSlotMapper.class, Map.of(
                        "selectReservableSlot", slotHandler::selectReservableSlot,
                        "selectOne", slotHandler::selectOne,
                        "updateById", slotHandler::updateById,
                        "countAvailableSlots", slotHandler::countAvailableSlots)),
                proxy(StatusTransitionLogMapper.class, Map.of("insert", slotHandler::insertLog)));

        BizException exception = assertThrows(BizException.class, () -> adapter.refreshSessionRemainingCount(4101L));

        assertEquals(OutpatientErrorCode.SESSION_UPDATE_CONFLICT, exception.getErrorCode());
    }

    @Test
    void releaseReservedSlot_WhenSlotLocked_ReturnTrue() {
        SessionMapperHandler sessionHandler = new SessionMapperHandler();
        SlotMapperHandler slotHandler = new SlotMapperHandler();
        ClinicSlotReservationRepositoryAdapter adapter = new ClinicSlotReservationRepositoryAdapter(
                proxy(ClinicSessionMapper.class, Map.of(
                        "selectCount", sessionHandler::selectCount,
                        "selectOne", sessionHandler::selectOne,
                        "updateById", sessionHandler::updateById)),
                proxy(ClinicSlotMapper.class, Map.of(
                        "selectReservableSlot", slotHandler::selectReservableSlot,
                        "selectOne", slotHandler::selectOne,
                        "updateById", slotHandler::updateById,
                        "countAvailableSlots", slotHandler::countAvailableSlots)),
                proxy(StatusTransitionLogMapper.class, Map.of("insert", slotHandler::insertLog)));

        boolean result = adapter.releaseReservedSlot(4101L, 5101L, "BOOKED");

        assertTrue(result);
        assertEquals("AVAILABLE", slotHandler.updatedSlotStatus);
        assertEquals(1, slotHandler.updatedRemainingCount);
    }

    @Test
    void releaseReservedSlot_WhenSlotBookedAndExpectedBooked_ReturnTrue() {
        SessionMapperHandler sessionHandler = new SessionMapperHandler();
        SlotMapperHandler slotHandler = new SlotMapperHandler();
        slotHandler.currentSlotStatus = "BOOKED";
        ClinicSlotReservationRepositoryAdapter adapter = new ClinicSlotReservationRepositoryAdapter(
                proxy(ClinicSessionMapper.class, Map.of(
                        "selectCount", sessionHandler::selectCount,
                        "selectOne", sessionHandler::selectOne,
                        "updateById", sessionHandler::updateById)),
                proxy(ClinicSlotMapper.class, Map.of(
                        "selectReservableSlot", slotHandler::selectReservableSlot,
                        "selectOne", slotHandler::selectOne,
                        "updateById", slotHandler::updateById,
                        "countAvailableSlots", slotHandler::countAvailableSlots)),
                proxy(StatusTransitionLogMapper.class, Map.of("insert", slotHandler::insertLog)));

        boolean result = adapter.releaseReservedSlot(4101L, 5101L, "BOOKED");

        assertTrue(result);
        assertEquals("AVAILABLE", slotHandler.updatedSlotStatus);
        assertEquals(1, slotHandler.updatedRemainingCount);
    }

    @Test
    void releaseReservedSlot_WhenSlotStatusMismatch_ReturnFalse() {
        SessionMapperHandler sessionHandler = new SessionMapperHandler();
        SlotMapperHandler slotHandler = new SlotMapperHandler();
        slotHandler.currentSlotStatus = "LOCKED";
        ClinicSlotReservationRepositoryAdapter adapter = new ClinicSlotReservationRepositoryAdapter(
                proxy(ClinicSessionMapper.class, Map.of(
                        "selectCount", sessionHandler::selectCount,
                        "selectOne", sessionHandler::selectOne,
                        "updateById", sessionHandler::updateById)),
                proxy(ClinicSlotMapper.class, Map.of(
                        "selectReservableSlot", slotHandler::selectReservableSlot,
                        "selectOne", slotHandler::selectOne,
                        "updateById", slotHandler::updateById,
                        "countAvailableSlots", slotHandler::countAvailableSlots)),
                proxy(StatusTransitionLogMapper.class, Map.of("insert", slotHandler::insertLog)));

        boolean result = adapter.releaseReservedSlot(4101L, 5101L, "BOOKED");

        assertFalse(result);
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

    private static final class CountingSessionHandler {

        private Object selectCount(Object[] arguments) {
            return 1L;
        }

        private Object insertLog(Object[] arguments) {
            return 1;
        }
    }

    private static final class SessionMapperHandler {

        private int updateRows = 1;
        private Integer updatedRemainingCount;

        private Object selectCount(Object[] arguments) {
            return 1L;
        }

        private Object selectOne(Object[] arguments) {
            ClinicSessionDO session = new ClinicSessionDO();
            session.setId(4101L);
            session.setVersion(3);
            return session;
        }

        private Object updateById(Object[] arguments) {
            ClinicSessionDO session = (ClinicSessionDO) arguments[0];
            this.updatedRemainingCount = session.getRemainingCount();
            return updateRows;
        }
    }

    private static final class SlotMapperHandler {

        private int updateRows = 1;
        private String updatedSlotStatus;
        private Integer updatedRemainingCount;
        private String currentSlotStatus = "BOOKED";

        private Object selectReservableSlot(Object[] arguments) {
            ClinicSlotReservationRow row = new ClinicSlotReservationRow();
            row.setSessionId((Long) arguments[0]);
            row.setSlotId((Long) arguments[1]);
            row.setSlotVersion(2);
            row.setDoctorId(2101L);
            row.setDepartmentId(3101L);
            row.setFee(new BigDecimal("18.00"));
            return row;
        }

        private Object updateById(Object[] arguments) {
            ClinicSlotDO slot = (ClinicSlotDO) arguments[0];
            this.updatedSlotStatus = slot.getSlotStatus();
            this.updatedRemainingCount = slot.getRemainingCount();
            return updateRows;
        }

        private Object selectOne(Object[] arguments) {
            ClinicSlotDO slot = new ClinicSlotDO();
            slot.setId(5101L);
            slot.setSessionId(4101L);
            slot.setVersion(2);
            slot.setSlotStatus(currentSlotStatus);
            return slot;
        }

        private Object countAvailableSlots(Object[] arguments) {
            return 5;
        }

        private Object insertLog(Object[] arguments) {
            return 1;
        }
    }
}
