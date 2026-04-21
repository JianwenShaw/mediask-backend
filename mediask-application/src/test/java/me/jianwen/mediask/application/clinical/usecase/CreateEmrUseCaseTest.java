package me.jianwen.mediask.application.clinical.usecase;

import me.jianwen.mediask.application.clinical.command.CreateEmrCommand;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.clinical.exception.ClinicalErrorCode;
import me.jianwen.mediask.domain.clinical.model.EmrDiagnosis;
import me.jianwen.mediask.domain.clinical.model.EmrRecord;
import me.jianwen.mediask.domain.clinical.model.EmrRecordStatus;
import me.jianwen.mediask.domain.clinical.model.EncounterDetail;
import me.jianwen.mediask.domain.clinical.model.EncounterPatientSummary;
import me.jianwen.mediask.domain.clinical.model.VisitEncounterStatus;
import me.jianwen.mediask.domain.clinical.port.EmrRecordRepository;
import me.jianwen.mediask.domain.clinical.port.EncounterQueryRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CreateEmrUseCaseTest {

    @Test
    void handle_ValidCommand_CreatesEmrRecord() {
        StubRepositories repositories = new StubRepositories();
        CreateEmrUseCase useCase = new CreateEmrUseCase(
                repositories.emrRecordRepository,
                repositories.encounterQueryRepository
        );

        CreateEmrCommand command = new CreateEmrCommand(
                1L,
                200L,
                "Headache and nasal congestion",
                "Patient presents with persistent headache for 3 days...",
                List.of(
                        new CreateEmrCommand.EmrDiagnosisCommand(EmrDiagnosis.DiagnosisType.PRIMARY, "J01.90", "Acute sinusitis", true, 0),
                        new CreateEmrCommand.EmrDiagnosisCommand(EmrDiagnosis.DiagnosisType.SECONDARY, "J06.9", "Acute upper respiratory infection", false, 1)
                )
        );

        EmrRecord result = useCase.handle(command);

        assertNotNull(result);
        assertEquals(EmrRecordStatus.DRAFT, result.recordStatus());
        assertEquals("Headache and nasal congestion", result.chiefComplaintSummary());
        assertEquals("Patient presents with persistent headache for 3 days...", result.content());
        assertEquals(2, result.diagnoses().size());
        assertEquals("Acute sinusitis", result.diagnoses().get(0).diagnosisName());
        assertTrue(result.diagnoses().get(0).isPrimary());
        assertEquals("Acute upper respiratory infection", result.diagnoses().get(1).diagnosisName());
        assertFalse(result.diagnoses().get(1).isPrimary());
        assertTrue(result.recordNo().startsWith("EMR"));
        assertEquals(1L, repositories.emrRecordRepository.savedEmrRecord.encounterId());
        assertEquals(200L, repositories.emrRecordRepository.savedEmrRecord.doctorId());
    }

    @Test
    void handle_EncounterNotFound_ThrowsException() {
        StubRepositories repositories = new StubRepositories();
        repositories.encounterQueryRepository.returnEmpty = true;

        CreateEmrUseCase useCase = new CreateEmrUseCase(
                repositories.emrRecordRepository,
                repositories.encounterQueryRepository
        );

        CreateEmrCommand command = new CreateEmrCommand(
                999L,
                200L,
                "Summary",
                "Content...",
                List.of(new CreateEmrCommand.EmrDiagnosisCommand(EmrDiagnosis.DiagnosisType.PRIMARY, null, "Diagnosis", true, 0))
        );

        BizException exception = assertThrows(BizException.class, () -> useCase.handle(command));

        assertEquals(ClinicalErrorCode.EMR_ENCOUNTER_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void handle_EncounterBelongsToAnotherDoctor_ThrowsException() {
        StubRepositories repositories = new StubRepositories();
        repositories.encounterQueryRepository.doctorId = 999L;

        CreateEmrUseCase useCase = new CreateEmrUseCase(
                repositories.emrRecordRepository,
                repositories.encounterQueryRepository
        );

        CreateEmrCommand command = new CreateEmrCommand(
                1L,
                200L,
                "Summary",
                "Content...",
                List.of(new CreateEmrCommand.EmrDiagnosisCommand(EmrDiagnosis.DiagnosisType.PRIMARY, null, "Diagnosis", true, 0))
        );

        BizException exception = assertThrows(BizException.class, () -> useCase.handle(command));

        assertEquals(ClinicalErrorCode.EMR_ENCOUNTER_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void handle_EmrRecordAlreadyExists_ThrowsException() {
        StubRepositories repositories = new StubRepositories();
        repositories.emrRecordRepository.emrRecordExists = true;

        CreateEmrUseCase useCase = new CreateEmrUseCase(
                repositories.emrRecordRepository,
                repositories.encounterQueryRepository
        );

        CreateEmrCommand command = new CreateEmrCommand(
                1L,
                200L,
                "Summary",
                "Content...",
                List.of(new CreateEmrCommand.EmrDiagnosisCommand(EmrDiagnosis.DiagnosisType.PRIMARY, null, "Diagnosis", true, 0))
        );

        BizException exception = assertThrows(BizException.class, () -> useCase.handle(command));

        assertEquals(ClinicalErrorCode.EMR_RECORD_ALREADY_EXISTS.getCode(), exception.getCode());
    }

    @Test
    void handle_WithEmptyDiagnosisList_CreatesEmrRecord() {
        StubRepositories repositories = new StubRepositories();

        CreateEmrUseCase useCase = new CreateEmrUseCase(
                repositories.emrRecordRepository,
                repositories.encounterQueryRepository
        );

        CreateEmrCommand command = new CreateEmrCommand(
                1L,
                200L,
                "Summary",
                "Content...",
                List.of()
        );

        EmrRecord result = useCase.handle(command);

        assertNotNull(result);
        assertTrue(result.diagnoses().isEmpty());
    }

    private static class StubRepositories {
        StubEmrRecordRepository emrRecordRepository = new StubEmrRecordRepository();
        StubEncounterQueryRepository encounterQueryRepository = new StubEncounterQueryRepository();
    }

    private static class StubEmrRecordRepository implements EmrRecordRepository {
        EmrRecord savedEmrRecord;
        boolean emrRecordExists = false;

        @Override
        public void save(EmrRecord emrRecord) {
            this.savedEmrRecord = emrRecord;
        }

        @Override
        public boolean existsByEncounterId(Long encounterId) {
            return emrRecordExists;
        }
    }

    private static class StubEncounterQueryRepository implements EncounterQueryRepository {
        Long doctorId = 200L;
        boolean returnEmpty = false;
        Long lastQueriedEncounterId;

        @Override
        public List<me.jianwen.mediask.domain.clinical.model.EncounterListItem> listByDoctorId(Long doctorId, me.jianwen.mediask.domain.clinical.model.VisitEncounterStatus status) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<EncounterDetail> findDetailByEncounterId(Long encounterId) {
            this.lastQueriedEncounterId = encounterId;
            if (returnEmpty) {
                return Optional.empty();
            }
            return Optional.of(new EncounterDetail(
                    encounterId,
                    100L,
                    this.doctorId,
                    new EncounterPatientSummary(
                            300L,
                            "Test Patient",
                            "FEMALE",
                            400L,
                            "Test Department",
                            LocalDate.parse("2026-04-18"),
                            "MORNING",
                            VisitEncounterStatus.IN_PROGRESS,
                            OffsetDateTime.parse("2026-04-18T10:00:00+08:00"),
                            null,
                            LocalDate.parse("2002-03-18")
                    )
            ));
        }

        @Override
        public Optional<me.jianwen.mediask.domain.clinical.model.EncounterAiSummary> findAiSummaryByEncounterId(Long encounterId) {
            throw new UnsupportedOperationException();
        }
    }
}
