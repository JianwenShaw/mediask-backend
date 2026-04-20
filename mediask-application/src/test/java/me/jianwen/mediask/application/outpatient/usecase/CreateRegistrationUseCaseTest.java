package me.jianwen.mediask.application.outpatient.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import me.jianwen.mediask.application.ai.usecase.AiRegistrationHandoffSupport;
import me.jianwen.mediask.domain.clinical.model.VisitEncounter;
import me.jianwen.mediask.domain.clinical.model.VisitEncounterStatus;
import me.jianwen.mediask.domain.clinical.port.VisitEncounterRepository;
import me.jianwen.mediask.application.outpatient.command.CreateRegistrationCommand;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.ai.exception.AiErrorCode;
import me.jianwen.mediask.domain.ai.model.AiSessionDetail;
import me.jianwen.mediask.domain.ai.model.AiSessionListItem;
import me.jianwen.mediask.domain.ai.model.AiSessionTriageResultView;
import me.jianwen.mediask.domain.ai.model.AiTriageResultStatus;
import me.jianwen.mediask.domain.ai.model.AiTriageStage;
import me.jianwen.mediask.domain.ai.model.GuardrailAction;
import me.jianwen.mediask.domain.ai.model.RecommendedDepartment;
import me.jianwen.mediask.domain.ai.model.RiskLevel;
import me.jianwen.mediask.domain.ai.port.AiSessionQueryRepository;
import me.jianwen.mediask.domain.outpatient.exception.OutpatientErrorCode;
import me.jianwen.mediask.domain.outpatient.model.ClinicSlotReservation;
import me.jianwen.mediask.domain.outpatient.model.RegistrationDetail;
import me.jianwen.mediask.domain.outpatient.model.RegistrationOrder;
import me.jianwen.mediask.domain.outpatient.port.ClinicSlotReservationRepository;
import me.jianwen.mediask.domain.outpatient.port.RegistrationOrderRepository;
import org.junit.jupiter.api.Test;

class CreateRegistrationUseCaseTest {

    @Test
    void handle_WhenOpenSessionAndAvailableSlot_CreatePendingPaymentOrder() {
        StubClinicSlotReservationRepository reservationRepository = new StubClinicSlotReservationRepository();
        CapturingRegistrationOrderRepository orderRepository = new CapturingRegistrationOrderRepository();
        CapturingVisitEncounterRepository visitEncounterRepository = new CapturingVisitEncounterRepository();
        CreateRegistrationUseCase useCase = new CreateRegistrationUseCase(
                reservationRepository,
                orderRepository,
                visitEncounterRepository,
                new AiRegistrationHandoffSupport(new AllowingAiSessionQueryRepository()));

        CreateRegistrationResult result =
                useCase.handle(new CreateRegistrationCommand(2003L, 4101L, 5101L, 7101L));

        assertTrue(reservationRepository.existsOpenSessionCalled);
        assertTrue(reservationRepository.reserveAvailableSlotCalled);
        assertTrue(reservationRepository.refreshSessionRemainingCountCalled);
        assertEquals(2003L, orderRepository.savedOrder.patientId());
        assertEquals(2101L, orderRepository.savedOrder.doctorId());
        assertEquals(3101L, orderRepository.savedOrder.departmentId());
        assertEquals(4101L, orderRepository.savedOrder.sessionId());
        assertEquals(5101L, orderRepository.savedOrder.slotId());
        assertEquals(7101L, orderRepository.savedOrder.sourceAiSessionId());
        assertEquals(orderRepository.savedOrder.registrationId(), visitEncounterRepository.savedEncounter.registrationId());
        assertEquals(2003L, visitEncounterRepository.savedEncounter.patientUserId());
        assertEquals(2101L, visitEncounterRepository.savedEncounter.doctorId());
        assertEquals(3101L, visitEncounterRepository.savedEncounter.departmentId());
        assertEquals(VisitEncounterStatus.SCHEDULED, visitEncounterRepository.savedEncounter.status());
        assertEquals("PENDING_PAYMENT", result.status().name());
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
                new CapturingVisitEncounterRepository(),
                new AiRegistrationHandoffSupport(new AllowingAiSessionQueryRepository()));

        BizException exception = assertThrows(
                BizException.class, () -> useCase.handle(new CreateRegistrationCommand(2003L, 4101L, 5101L, null)));

        assertEquals(OutpatientErrorCode.SESSION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void handle_WhenSlotUnavailable_ThrowSlotNotAvailable() {
        StubClinicSlotReservationRepository reservationRepository = new StubClinicSlotReservationRepository();
        reservationRepository.reservation = Optional.empty();
        CreateRegistrationUseCase useCase = new CreateRegistrationUseCase(
                reservationRepository,
                new CapturingRegistrationOrderRepository(),
                new CapturingVisitEncounterRepository(),
                new AiRegistrationHandoffSupport(new AllowingAiSessionQueryRepository()));

        BizException exception = assertThrows(
                BizException.class, () -> useCase.handle(new CreateRegistrationCommand(2003L, 4101L, 5101L, null)));

        assertEquals(OutpatientErrorCode.SLOT_NOT_AVAILABLE, exception.getErrorCode());
    }

    @Test
    void handle_WhenAiSessionOwnedByAnotherPatient_ThrowAccessDenied() {
        StubClinicSlotReservationRepository reservationRepository = new StubClinicSlotReservationRepository();
        CreateRegistrationUseCase useCase = new CreateRegistrationUseCase(
                reservationRepository,
                new CapturingRegistrationOrderRepository(),
                new CapturingVisitEncounterRepository(),
                new AiRegistrationHandoffSupport(new ForeignAiSessionQueryRepository()));

        BizException exception = assertThrows(
                BizException.class, () -> useCase.handle(new CreateRegistrationCommand(2003L, 4101L, 5101L, 7101L)));

        assertEquals(AiErrorCode.AI_SESSION_ACCESS_DENIED, exception.getErrorCode());
        assertTrue(!reservationRepository.existsOpenSessionCalled);
    }

    @Test
    void handle_WhenAiSessionTriageStillCollecting_ThrowNotReady() {
        StubClinicSlotReservationRepository reservationRepository = new StubClinicSlotReservationRepository();
        CreateRegistrationUseCase useCase = new CreateRegistrationUseCase(
                reservationRepository,
                new CapturingRegistrationOrderRepository(),
                new CapturingVisitEncounterRepository(),
                new AiRegistrationHandoffSupport(new CollectingAiSessionQueryRepository()));

        BizException exception = assertThrows(
                BizException.class, () -> useCase.handle(new CreateRegistrationCommand(2003L, 4101L, 5101L, 7101L)));

        assertEquals(AiErrorCode.AI_SESSION_TRIAGE_RESULT_NOT_READY, exception.getErrorCode());
        assertTrue(!reservationRepository.existsOpenSessionCalled);
    }

    @Test
    void handle_WhenAiSessionBlockedForRegistration_ThrowUnavailable() {
        StubClinicSlotReservationRepository reservationRepository = new StubClinicSlotReservationRepository();
        CreateRegistrationUseCase useCase = new CreateRegistrationUseCase(
                reservationRepository,
                new CapturingRegistrationOrderRepository(),
                new CapturingVisitEncounterRepository(),
                new AiRegistrationHandoffSupport(new HighRiskAiSessionQueryRepository()));

        BizException exception = assertThrows(
                BizException.class, () -> useCase.handle(new CreateRegistrationCommand(2003L, 4101L, 5101L, 7101L)));

        assertEquals(AiErrorCode.AI_SESSION_REGISTRATION_HANDOFF_UNAVAILABLE, exception.getErrorCode());
        assertTrue(!reservationRepository.existsOpenSessionCalled);
    }

    @Test
    void handle_WhenAiSessionHasNoDepartment_ThrowUnavailable() {
        StubClinicSlotReservationRepository reservationRepository = new StubClinicSlotReservationRepository();
        CreateRegistrationUseCase useCase = new CreateRegistrationUseCase(
                reservationRepository,
                new CapturingRegistrationOrderRepository(),
                new CapturingVisitEncounterRepository(),
                new AiRegistrationHandoffSupport(new NoDepartmentAiSessionQueryRepository()));

        BizException exception = assertThrows(
                BizException.class, () -> useCase.handle(new CreateRegistrationCommand(2003L, 4101L, 5101L, 7101L)));

        assertEquals(AiErrorCode.AI_SESSION_REGISTRATION_HANDOFF_UNAVAILABLE, exception.getErrorCode());
        assertTrue(!reservationRepository.existsOpenSessionCalled);
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
    }

    private static class AllowingAiSessionQueryRepository implements AiSessionQueryRepository {
        @Override
        public java.util.List<AiSessionListItem> listSessionsByPatientUserId(Long patientUserId) {
            return java.util.List.of();
        }

        @Override
        public Optional<AiSessionDetail> findSessionDetailById(Long sessionId) {
            return Optional.empty();
        }

        @Override
        public Optional<AiSessionTriageResultView> findLatestTriageResultBySessionId(Long sessionId) {
            return Optional.of(new AiSessionTriageResultView(
                    7101L,
                    2003L,
                    AiTriageResultStatus.CURRENT,
                    AiTriageStage.READY,
                    8101L,
                    OffsetDateTime.parse("2026-04-12T09:31:00+08:00"),
                    false,
                    null,
                    "头痛三天",
                    RiskLevel.MEDIUM,
                    GuardrailAction.CAUTION,
                    java.util.List.of(new RecommendedDepartment(3101L, "神经内科", 1, "持续头痛")),
                    "建议线下就诊",
                    java.util.List.of()));
        }

        @Override
        public Optional<AiTriageStage> findLatestTriageStageBySessionId(Long sessionId) {
            return Optional.of(AiTriageStage.READY);
        }

        @Override
        public boolean hasAccessibleTriageSession(Long patientUserId, Long sessionId) {
            return patientUserId.equals(2003L) && sessionId.equals(7101L);
        }
    }

    private static final class ForeignAiSessionQueryRepository extends AllowingAiSessionQueryRepository {
        @Override
        public Optional<AiSessionTriageResultView> findLatestTriageResultBySessionId(Long sessionId) {
            return Optional.of(new AiSessionTriageResultView(
                    7101L,
                    9999L,
                    AiTriageResultStatus.CURRENT,
                    AiTriageStage.READY,
                    8101L,
                    OffsetDateTime.parse("2026-04-12T09:31:00+08:00"),
                    false,
                    null,
                    "头痛三天",
                    RiskLevel.MEDIUM,
                    GuardrailAction.CAUTION,
                    java.util.List.of(new RecommendedDepartment(3101L, "神经内科", 1, "持续头痛")),
                    "建议线下就诊",
                    java.util.List.of()));
        }

        @Override
        public boolean hasAccessibleTriageSession(Long patientUserId, Long sessionId) {
            return false;
        }
    }

    private static final class CollectingAiSessionQueryRepository extends AllowingAiSessionQueryRepository {
        @Override
        public Optional<AiSessionTriageResultView> findLatestTriageResultBySessionId(Long sessionId) {
            return Optional.empty();
        }

        @Override
        public Optional<AiTriageStage> findLatestTriageStageBySessionId(Long sessionId) {
            return Optional.of(AiTriageStage.COLLECTING);
        }
    }

    private static final class HighRiskAiSessionQueryRepository extends AllowingAiSessionQueryRepository {
        @Override
        public Optional<AiSessionTriageResultView> findLatestTriageResultBySessionId(Long sessionId) {
            return Optional.of(new AiSessionTriageResultView(
                    7101L,
                    2003L,
                    AiTriageResultStatus.CURRENT,
                    AiTriageStage.BLOCKED,
                    8101L,
                    OffsetDateTime.parse("2026-04-12T09:31:00+08:00"),
                    false,
                    null,
                    "胸痛一小时",
                    RiskLevel.HIGH,
                    GuardrailAction.CAUTION,
                    java.util.List.of(new RecommendedDepartment(3101L, "急诊科", 1, "胸痛高风险")),
                    "立即线下就医",
                    java.util.List.of()));
        }
    }

    private static final class NoDepartmentAiSessionQueryRepository extends AllowingAiSessionQueryRepository {
        @Override
        public Optional<AiSessionTriageResultView> findLatestTriageResultBySessionId(Long sessionId) {
            return Optional.of(new AiSessionTriageResultView(
                    7101L,
                    2003L,
                    AiTriageResultStatus.CURRENT,
                    AiTriageStage.READY,
                    8101L,
                    OffsetDateTime.parse("2026-04-12T09:31:00+08:00"),
                    false,
                    null,
                    "头痛三天",
                    RiskLevel.MEDIUM,
                    GuardrailAction.CAUTION,
                    java.util.List.of(),
                    "建议线下就诊",
                    java.util.List.of()));
        }
    }
}
