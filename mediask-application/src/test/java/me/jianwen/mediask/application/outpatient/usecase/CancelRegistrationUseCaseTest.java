package me.jianwen.mediask.application.outpatient.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import me.jianwen.mediask.application.outpatient.command.CancelRegistrationCommand;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.clinical.model.VisitEncounter;
import me.jianwen.mediask.domain.clinical.port.VisitEncounterRepository;
import me.jianwen.mediask.domain.outpatient.exception.OutpatientErrorCode;
import me.jianwen.mediask.domain.outpatient.model.RegistrationOrder;
import me.jianwen.mediask.domain.outpatient.model.RegistrationStatus;
import me.jianwen.mediask.domain.outpatient.port.ClinicSlotReservationRepository;
import me.jianwen.mediask.domain.outpatient.port.RegistrationOrderRepository;
import org.junit.jupiter.api.Test;

class CancelRegistrationUseCaseTest {

    @Test
    void handle_WhenRegistrationOwnedByPatient_CancelRegistration() {
        StubRegistrationOrderRepository registrationOrderRepository = new StubRegistrationOrderRepository();
        StubClinicSlotReservationRepository clinicSlotReservationRepository = new StubClinicSlotReservationRepository();
        StubVisitEncounterRepository visitEncounterRepository = new StubVisitEncounterRepository();
        CancelRegistrationUseCase useCase = new CancelRegistrationUseCase(
                registrationOrderRepository, clinicSlotReservationRepository, visitEncounterRepository);

        CancelRegistrationResult result = useCase.handle(new CancelRegistrationCommand(6101L, 2003L));

        assertEquals(6101L, registrationOrderRepository.lastRegistrationId);
        assertEquals(2003L, registrationOrderRepository.lastPatientUserId);
        assertTrue(visitEncounterRepository.cancelScheduledCalled);
        assertTrue(clinicSlotReservationRepository.releaseReservedSlotCalled);
        assertEquals("BOOKED", clinicSlotReservationRepository.lastExpectedCurrentStatus);
        assertTrue(clinicSlotReservationRepository.refreshSessionRemainingCountCalled);
        assertEquals(RegistrationStatus.CANCELLED, registrationOrderRepository.updatedOrder.status());
        assertNotNull(registrationOrderRepository.updatedOrder.cancelledAt());
        assertEquals(RegistrationStatus.CANCELLED, result.status());
    }

    @Test
    void handle_WhenRegistrationMissing_ThrowNotFound() {
        StubRegistrationOrderRepository registrationOrderRepository = new StubRegistrationOrderRepository();
        registrationOrderRepository.returnEmpty = true;
        CancelRegistrationUseCase useCase = new CancelRegistrationUseCase(
                registrationOrderRepository,
                new StubClinicSlotReservationRepository(),
                new StubVisitEncounterRepository());

        BizException exception =
                assertThrows(BizException.class, () -> useCase.handle(new CancelRegistrationCommand(9999L, 2003L)));

        assertEquals(OutpatientErrorCode.REGISTRATION_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void handle_WhenEncounterCannotCancel_ThrowConflict() {
        StubRegistrationOrderRepository registrationOrderRepository = new StubRegistrationOrderRepository();
        StubClinicSlotReservationRepository clinicSlotReservationRepository = new StubClinicSlotReservationRepository();
        StubVisitEncounterRepository visitEncounterRepository = new StubVisitEncounterRepository();
        visitEncounterRepository.cancelScheduledResult = false;
        CancelRegistrationUseCase useCase = new CancelRegistrationUseCase(
                registrationOrderRepository, clinicSlotReservationRepository, visitEncounterRepository);

        BizException exception =
                assertThrows(BizException.class, () -> useCase.handle(new CancelRegistrationCommand(6101L, 2003L)));

        assertEquals(OutpatientErrorCode.REGISTRATION_CANCEL_NOT_ALLOWED.getCode(), exception.getCode());
        assertFalse(clinicSlotReservationRepository.releaseReservedSlotCalled);
    }

    @Test
    void handle_WhenSlotCannotRelease_ThrowConflict() {
        StubRegistrationOrderRepository registrationOrderRepository = new StubRegistrationOrderRepository();
        StubClinicSlotReservationRepository clinicSlotReservationRepository = new StubClinicSlotReservationRepository();
        clinicSlotReservationRepository.releaseReservedSlotResult = false;
        StubVisitEncounterRepository visitEncounterRepository = new StubVisitEncounterRepository();
        CancelRegistrationUseCase useCase = new CancelRegistrationUseCase(
                registrationOrderRepository, clinicSlotReservationRepository, visitEncounterRepository);

        BizException exception =
                assertThrows(BizException.class, () -> useCase.handle(new CancelRegistrationCommand(6101L, 2003L)));

        assertEquals(OutpatientErrorCode.REGISTRATION_CANCEL_NOT_ALLOWED.getCode(), exception.getCode());
        assertFalse(clinicSlotReservationRepository.refreshSessionRemainingCountCalled);
    }

    @Test
    void handle_WhenConfirmedRegistrationButSlotStateMismatch_ThrowConflict() {
        StubRegistrationOrderRepository registrationOrderRepository = new StubRegistrationOrderRepository();
        StubClinicSlotReservationRepository clinicSlotReservationRepository = new StubClinicSlotReservationRepository();
        clinicSlotReservationRepository.releaseReservedSlotResult = false;
        StubVisitEncounterRepository visitEncounterRepository = new StubVisitEncounterRepository();
        CancelRegistrationUseCase useCase = new CancelRegistrationUseCase(
                registrationOrderRepository, clinicSlotReservationRepository, visitEncounterRepository);

        BizException exception =
                assertThrows(BizException.class, () -> useCase.handle(new CancelRegistrationCommand(6101L, 2003L)));

        assertEquals("BOOKED", clinicSlotReservationRepository.lastExpectedCurrentStatus);
        assertEquals(OutpatientErrorCode.REGISTRATION_CANCEL_NOT_ALLOWED.getCode(), exception.getCode());
    }

    @Test
    void handle_WhenRegistrationAlreadyCompleted_ThrowInvalidStatusTransition() {
        StubRegistrationOrderRepository registrationOrderRepository = new StubRegistrationOrderRepository();
        registrationOrderRepository.registrationOrder = RegistrationOrder.reconstitute(
                6101L,
                "REG6101",
                2003L,
                2101L,
                3101L,
                4101L,
                5101L,
                7101L,
                RegistrationStatus.COMPLETED,
                new BigDecimal("18.00"),
                null,
                null);
        CancelRegistrationUseCase useCase = new CancelRegistrationUseCase(
                registrationOrderRepository,
                new StubClinicSlotReservationRepository(),
                new StubVisitEncounterRepository());

        BizException exception =
                assertThrows(BizException.class, () -> useCase.handle(new CancelRegistrationCommand(6101L, 2003L)));

        assertEquals(OutpatientErrorCode.INVALID_STATUS_TRANSITION.getCode(), exception.getCode());
    }

    private static final class StubRegistrationOrderRepository implements RegistrationOrderRepository {

        private Long lastRegistrationId;
        private Long lastPatientUserId;
        private boolean returnEmpty;
        private RegistrationOrder updatedOrder;
        private RegistrationOrder registrationOrder = RegistrationOrder.reconstitute(
                6101L,
                "REG6101",
                2003L,
                2101L,
                3101L,
                4101L,
                5101L,
                7101L,
                RegistrationStatus.CONFIRMED,
                new BigDecimal("18.00"),
                null,
                null);

        @Override
        public void save(RegistrationOrder registrationOrder) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<RegistrationOrder> findByRegistrationIdAndPatientId(Long registrationId, Long patientUserId) {
            this.lastRegistrationId = registrationId;
            this.lastPatientUserId = patientUserId;
            return returnEmpty ? Optional.empty() : Optional.of(registrationOrder);
        }

        @Override
        public void update(RegistrationOrder registrationOrder) {
            this.updatedOrder = registrationOrder;
        }

        @Override
        public boolean completeConfirmedByRegistrationId(Long registrationId) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class StubClinicSlotReservationRepository implements ClinicSlotReservationRepository {

        private boolean releaseReservedSlotCalled;
        private boolean refreshSessionRemainingCountCalled;
        private boolean releaseReservedSlotResult = true;
        private String lastExpectedCurrentStatus;

        @Override
        public boolean existsOpenSession(Long sessionId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<me.jianwen.mediask.domain.outpatient.model.ClinicSlotReservation> reserveAvailableSlot(
                Long sessionId, Long slotId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean releaseReservedSlot(Long sessionId, Long slotId, String expectedCurrentStatus) {
            this.releaseReservedSlotCalled = true;
            this.lastExpectedCurrentStatus = expectedCurrentStatus;
            return releaseReservedSlotResult;
        }

        @Override
        public void refreshSessionRemainingCount(Long sessionId) {
            this.refreshSessionRemainingCountCalled = true;
        }
    }

    private static final class StubVisitEncounterRepository implements VisitEncounterRepository {

        private boolean cancelScheduledCalled;
        private boolean cancelScheduledResult = true;

        @Override
        public void save(VisitEncounter visitEncounter) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean cancelScheduledByRegistrationId(Long registrationId) {
            this.cancelScheduledCalled = true;
            return cancelScheduledResult;
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
