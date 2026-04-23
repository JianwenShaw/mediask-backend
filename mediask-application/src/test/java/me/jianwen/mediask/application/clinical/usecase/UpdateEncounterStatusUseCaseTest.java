package me.jianwen.mediask.application.clinical.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import me.jianwen.mediask.application.clinical.command.UpdateEncounterStatusCommand;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.clinical.exception.ClinicalErrorCode;
import me.jianwen.mediask.domain.clinical.model.EncounterDetail;
import me.jianwen.mediask.domain.clinical.model.EncounterListItem;
import me.jianwen.mediask.domain.clinical.model.EncounterPatientSummary;
import me.jianwen.mediask.domain.clinical.model.VisitEncounterStatus;
import me.jianwen.mediask.domain.clinical.port.EncounterQueryRepository;
import me.jianwen.mediask.domain.clinical.port.VisitEncounterRepository;
import me.jianwen.mediask.domain.outpatient.port.RegistrationOrderRepository;
import org.junit.jupiter.api.Test;

class UpdateEncounterStatusUseCaseTest {

    @Test
    void handle_WhenStartFromScheduled_ReturnInProgress() {
        StubEncounterQueryRepository encounterQueryRepository = new StubEncounterQueryRepository();
        StubVisitEncounterRepository visitEncounterRepository = new StubVisitEncounterRepository();
        StubRegistrationOrderRepository registrationOrderRepository = new StubRegistrationOrderRepository();
        encounterQueryRepository.status = VisitEncounterStatus.SCHEDULED;
        UpdateEncounterStatusUseCase useCase = new UpdateEncounterStatusUseCase(
                encounterQueryRepository, visitEncounterRepository, registrationOrderRepository);

        UpdateEncounterStatusResult result = useCase.handle(
                new UpdateEncounterStatusCommand(8101L, 2101L, UpdateEncounterStatusCommand.Action.START));

        assertEquals(VisitEncounterStatus.IN_PROGRESS, result.encounterStatus());
        assertNotNull(result.startedAt());
        assertEquals(8101L, visitEncounterRepository.startEncounterId);
    }

    @Test
    void handle_WhenCompleteFromInProgress_ReturnCompleted() {
        StubEncounterQueryRepository encounterQueryRepository = new StubEncounterQueryRepository();
        StubVisitEncounterRepository visitEncounterRepository = new StubVisitEncounterRepository();
        StubRegistrationOrderRepository registrationOrderRepository = new StubRegistrationOrderRepository();
        encounterQueryRepository.status = VisitEncounterStatus.IN_PROGRESS;
        UpdateEncounterStatusUseCase useCase = new UpdateEncounterStatusUseCase(
                encounterQueryRepository, visitEncounterRepository, registrationOrderRepository);

        UpdateEncounterStatusResult result = useCase.handle(
                new UpdateEncounterStatusCommand(8101L, 2101L, UpdateEncounterStatusCommand.Action.COMPLETE));

        assertEquals(VisitEncounterStatus.COMPLETED, result.encounterStatus());
        assertNotNull(result.endedAt());
        assertEquals(8101L, visitEncounterRepository.completeEncounterId);
        assertEquals(6101L, registrationOrderRepository.completedRegistrationId);
    }

    @Test
    void handle_WhenEncounterMissing_ThrowNotFound() {
        StubEncounterQueryRepository encounterQueryRepository = new StubEncounterQueryRepository();
        encounterQueryRepository.returnEmpty = true;
        UpdateEncounterStatusUseCase useCase = new UpdateEncounterStatusUseCase(
                encounterQueryRepository, new StubVisitEncounterRepository(), new StubRegistrationOrderRepository());

        BizException exception = assertThrows(
                BizException.class,
                () -> useCase.handle(new UpdateEncounterStatusCommand(9999L, 2101L, UpdateEncounterStatusCommand.Action.START)));

        assertEquals(ClinicalErrorCode.ENCOUNTER_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void handle_WhenEncounterBelongsToAnotherDoctor_ThrowAccessDenied() {
        StubEncounterQueryRepository encounterQueryRepository = new StubEncounterQueryRepository();
        encounterQueryRepository.doctorId = 2102L;
        UpdateEncounterStatusUseCase useCase = new UpdateEncounterStatusUseCase(
                encounterQueryRepository, new StubVisitEncounterRepository(), new StubRegistrationOrderRepository());

        BizException exception = assertThrows(
                BizException.class,
                () -> useCase.handle(new UpdateEncounterStatusCommand(8101L, 2101L, UpdateEncounterStatusCommand.Action.START)));

        assertEquals(ClinicalErrorCode.ENCOUNTER_ACCESS_DENIED.getCode(), exception.getCode());
    }

    @Test
    void handle_WhenTransitionInvalid_ThrowConflict() {
        StubEncounterQueryRepository encounterQueryRepository = new StubEncounterQueryRepository();
        encounterQueryRepository.status = VisitEncounterStatus.COMPLETED;
        UpdateEncounterStatusUseCase useCase = new UpdateEncounterStatusUseCase(
                encounterQueryRepository, new StubVisitEncounterRepository(), new StubRegistrationOrderRepository());

        BizException exception = assertThrows(
                BizException.class,
                () -> useCase.handle(new UpdateEncounterStatusCommand(8101L, 2101L, UpdateEncounterStatusCommand.Action.START)));

        assertEquals(ClinicalErrorCode.ENCOUNTER_STATUS_TRANSITION_NOT_ALLOWED.getCode(), exception.getCode());
    }

    @Test
    void handle_WhenEncounterUpdateConflict_ThrowConflict() {
        StubEncounterQueryRepository encounterQueryRepository = new StubEncounterQueryRepository();
        StubVisitEncounterRepository visitEncounterRepository = new StubVisitEncounterRepository();
        encounterQueryRepository.status = VisitEncounterStatus.SCHEDULED;
        visitEncounterRepository.startResult = false;
        UpdateEncounterStatusUseCase useCase = new UpdateEncounterStatusUseCase(
                encounterQueryRepository, visitEncounterRepository, new StubRegistrationOrderRepository());

        BizException exception = assertThrows(
                BizException.class,
                () -> useCase.handle(new UpdateEncounterStatusCommand(8101L, 2101L, UpdateEncounterStatusCommand.Action.START)));

        assertEquals(ClinicalErrorCode.ENCOUNTER_STATUS_UPDATE_CONFLICT.getCode(), exception.getCode());
    }

    @Test
    void handle_WhenRegistrationSyncConflict_ThrowConflict() {
        StubEncounterQueryRepository encounterQueryRepository = new StubEncounterQueryRepository();
        StubVisitEncounterRepository visitEncounterRepository = new StubVisitEncounterRepository();
        StubRegistrationOrderRepository registrationOrderRepository = new StubRegistrationOrderRepository();
        encounterQueryRepository.status = VisitEncounterStatus.IN_PROGRESS;
        registrationOrderRepository.completeResult = false;
        UpdateEncounterStatusUseCase useCase = new UpdateEncounterStatusUseCase(
                encounterQueryRepository, visitEncounterRepository, registrationOrderRepository);

        BizException exception = assertThrows(
                BizException.class,
                () -> useCase.handle(
                        new UpdateEncounterStatusCommand(8101L, 2101L, UpdateEncounterStatusCommand.Action.COMPLETE)));

        assertEquals(ClinicalErrorCode.ENCOUNTER_REGISTRATION_SYNC_CONFLICT.getCode(), exception.getCode());
    }

    private static final class StubEncounterQueryRepository implements EncounterQueryRepository {

        private Long doctorId = 2101L;
        private VisitEncounterStatus status = VisitEncounterStatus.SCHEDULED;
        private boolean returnEmpty;

        @Override
        public List<EncounterListItem> listByDoctorId(Long doctorId, VisitEncounterStatus status) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<EncounterDetail> findDetailByEncounterId(Long encounterId) {
            if (returnEmpty) {
                return Optional.empty();
            }
            return Optional.of(new EncounterDetail(
                    encounterId,
                    6101L,
                    doctorId,
                    new EncounterPatientSummary(
                            2003L,
                            "李患者",
                            "FEMALE",
                            3101L,
                            "心内科",
                            LocalDate.parse("2026-04-03"),
                            "MORNING",
                            status,
                            OffsetDateTime.parse("2026-04-03T09:00:00+08:00"),
                            null,
                            LocalDate.parse("1990-05-15"))));
        }
    }

    private static final class StubVisitEncounterRepository implements VisitEncounterRepository {

        private Long startEncounterId;
        private Long completeEncounterId;
        private boolean startResult = true;
        private boolean completeResult = true;

        @Override
        public void save(me.jianwen.mediask.domain.clinical.model.VisitEncounter visitEncounter) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean cancelScheduledByRegistrationId(Long registrationId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean startScheduledByEncounterId(Long encounterId, OffsetDateTime startedAt) {
            this.startEncounterId = encounterId;
            return startResult;
        }

        @Override
        public boolean completeInProgressByEncounterId(Long encounterId, OffsetDateTime endedAt) {
            this.completeEncounterId = encounterId;
            return completeResult;
        }
    }

    private static final class StubRegistrationOrderRepository implements RegistrationOrderRepository {

        private Long completedRegistrationId;
        private boolean completeResult = true;

        @Override
        public void save(me.jianwen.mediask.domain.outpatient.model.RegistrationOrder registrationOrder) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<me.jianwen.mediask.domain.outpatient.model.RegistrationOrder> findByRegistrationIdAndPatientId(
                Long registrationId, Long patientUserId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void update(me.jianwen.mediask.domain.outpatient.model.RegistrationOrder registrationOrder) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean completeConfirmedByRegistrationId(Long registrationId) {
            this.completedRegistrationId = registrationId;
            return completeResult;
        }
    }
}
