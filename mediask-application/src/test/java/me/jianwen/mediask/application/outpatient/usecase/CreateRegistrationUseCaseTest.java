package me.jianwen.mediask.application.outpatient.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Optional;
import me.jianwen.mediask.application.outpatient.command.CreateRegistrationCommand;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.clinical.model.VisitEncounter;
import me.jianwen.mediask.domain.clinical.model.VisitEncounterStatus;
import me.jianwen.mediask.domain.clinical.port.VisitEncounterRepository;
import me.jianwen.mediask.domain.outpatient.exception.OutpatientErrorCode;
import me.jianwen.mediask.domain.outpatient.model.ClinicSlotReservation;
import me.jianwen.mediask.domain.outpatient.model.RegistrationOrder;
import me.jianwen.mediask.domain.outpatient.port.ClinicSlotReservationRepository;
import me.jianwen.mediask.domain.outpatient.port.RegistrationOrderRepository;
import org.junit.jupiter.api.Test;

class CreateRegistrationUseCaseTest {

    @Test
    void handle_WhenOpenSessionAndAvailableSlot_CreateConfirmedOrder() {
        StubClinicSlotReservationRepository reservationRepository = new StubClinicSlotReservationRepository();
        CapturingRegistrationOrderRepository orderRepository = new CapturingRegistrationOrderRepository();
        CapturingVisitEncounterRepository visitEncounterRepository = new CapturingVisitEncounterRepository();
        CreateRegistrationUseCase useCase =
                new CreateRegistrationUseCase(reservationRepository, orderRepository, visitEncounterRepository);

        CreateRegistrationResult result = useCase.handle(new CreateRegistrationCommand(2003L, 4101L, 5101L));

        assertTrue(reservationRepository.existsOpenSessionCalled);
        assertTrue(reservationRepository.reserveAvailableSlotCalled);
        assertTrue(reservationRepository.refreshSessionRemainingCountCalled);
        assertEquals(2003L, orderRepository.savedOrder.patientId());
        assertEquals(2101L, orderRepository.savedOrder.doctorId());
        assertEquals(3101L, orderRepository.savedOrder.departmentId());
        assertEquals(4101L, orderRepository.savedOrder.sessionId());
        assertEquals(5101L, orderRepository.savedOrder.slotId());
        assertEquals(orderRepository.savedOrder.registrationId(), visitEncounterRepository.savedEncounter.registrationId());
        assertEquals(2003L, visitEncounterRepository.savedEncounter.patientUserId());
        assertEquals(2101L, visitEncounterRepository.savedEncounter.doctorId());
        assertEquals(3101L, visitEncounterRepository.savedEncounter.departmentId());
        assertEquals(VisitEncounterStatus.SCHEDULED, visitEncounterRepository.savedEncounter.status());
        assertEquals("CONFIRMED", result.status().name());
        assertEquals(orderRepository.savedOrder.registrationId(), result.registrationId());
        assertEquals(orderRepository.savedOrder.orderNo(), result.orderNo());
    }

    @Test
    void handle_WhenSessionMissing_ThrowSessionNotFound() {
        StubClinicSlotReservationRepository reservationRepository = new StubClinicSlotReservationRepository();
        reservationRepository.existsOpenSession = false;
        CreateRegistrationUseCase useCase = new CreateRegistrationUseCase(
                reservationRepository,
                new CapturingRegistrationOrderRepository(),
                new CapturingVisitEncounterRepository());

        BizException exception = assertThrows(
                BizException.class, () -> useCase.handle(new CreateRegistrationCommand(2003L, 4101L, 5101L)));

        assertEquals(OutpatientErrorCode.SESSION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void handle_WhenSlotUnavailable_ThrowSlotNotAvailable() {
        StubClinicSlotReservationRepository reservationRepository = new StubClinicSlotReservationRepository();
        reservationRepository.reservation = Optional.empty();
        CreateRegistrationUseCase useCase = new CreateRegistrationUseCase(
                reservationRepository,
                new CapturingRegistrationOrderRepository(),
                new CapturingVisitEncounterRepository());

        BizException exception = assertThrows(
                BizException.class, () -> useCase.handle(new CreateRegistrationCommand(2003L, 4101L, 5101L)));

        assertEquals(OutpatientErrorCode.SLOT_NOT_AVAILABLE, exception.getErrorCode());
    }

    private static final class StubClinicSlotReservationRepository implements ClinicSlotReservationRepository {

        private boolean existsOpenSession = true;
        private Optional<ClinicSlotReservation> reservation = Optional.of(
                new ClinicSlotReservation(4101L, 5101L, 2101L, 3101L, new BigDecimal("18.00")));
        private boolean existsOpenSessionCalled;
        private boolean reserveAvailableSlotCalled;
        private boolean refreshSessionRemainingCountCalled;

        @Override
        public boolean existsOpenSession(Long sessionId) {
            existsOpenSessionCalled = true;
            return existsOpenSession;
        }

        @Override
        public Optional<ClinicSlotReservation> reserveAvailableSlot(Long sessionId, Long slotId) {
            reserveAvailableSlotCalled = true;
            return reservation;
        }

        @Override
        public boolean releaseReservedSlot(Long sessionId, Long slotId, String expectedCurrentStatus) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void refreshSessionRemainingCount(Long sessionId) {
            refreshSessionRemainingCountCalled = true;
        }
    }

    private static final class CapturingRegistrationOrderRepository implements RegistrationOrderRepository {

        private RegistrationOrder savedOrder;

        @Override
        public void save(RegistrationOrder registrationOrder) {
            this.savedOrder = registrationOrder;
        }

        @Override
        public Optional<RegistrationOrder> findByRegistrationIdAndPatientId(Long registrationId, Long patientUserId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void update(RegistrationOrder registrationOrder) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean completeConfirmedByRegistrationId(Long registrationId) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class CapturingVisitEncounterRepository implements VisitEncounterRepository {

        private VisitEncounter savedEncounter;

        @Override
        public void save(VisitEncounter visitEncounter) {
            this.savedEncounter = visitEncounter;
        }

        @Override
        public boolean cancelScheduledByRegistrationId(Long registrationId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean startScheduledByEncounterId(Long encounterId, java.time.OffsetDateTime startedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean completeInProgressByEncounterId(Long encounterId, java.time.OffsetDateTime endedAt) {
            throw new UnsupportedOperationException();
        }
    }
}
