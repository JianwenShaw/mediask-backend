package me.jianwen.mediask.application.clinical.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import me.jianwen.mediask.application.TestAuditSupport;
import me.jianwen.mediask.application.clinical.command.CreatePrescriptionCommand;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.clinical.exception.ClinicalErrorCode;
import me.jianwen.mediask.domain.clinical.model.EncounterDetail;
import me.jianwen.mediask.domain.clinical.model.EncounterPatientSummary;
import me.jianwen.mediask.domain.clinical.model.PrescriptionOrder;
import me.jianwen.mediask.domain.clinical.model.PrescriptionStatus;
import me.jianwen.mediask.domain.clinical.model.VisitEncounterStatus;
import me.jianwen.mediask.domain.clinical.port.EmrRecordQueryRepository;
import me.jianwen.mediask.domain.clinical.port.EncounterQueryRepository;
import me.jianwen.mediask.domain.clinical.port.PrescriptionOrderRepository;
import org.junit.jupiter.api.Test;

class CreatePrescriptionUseCaseTest {

    @Test
    void handle_ValidCommand_CreatesPrescription() {
        StubRepositories repositories = new StubRepositories();
        CreatePrescriptionUseCase useCase = new CreatePrescriptionUseCase(
                repositories.prescriptionOrderRepository,
                repositories.encounterQueryRepository,
                repositories.emrRecordQueryRepository,
                TestAuditSupport.auditTrailService());

        PrescriptionOrder result = useCase.handle(command(), TestAuditSupport.auditContext());

        assertNotNull(result);
        assertEquals(PrescriptionStatus.DRAFT, result.prescriptionStatus());
        assertEquals(2, result.items().size());
        assertTrue(result.prescriptionNo().startsWith("RX"));
        assertEquals(1L, repositories.prescriptionOrderRepository.savedPrescription.encounterId());
        assertEquals(200L, repositories.prescriptionOrderRepository.savedPrescription.doctorId());
        assertEquals(7101L, repositories.prescriptionOrderRepository.savedPrescription.recordId());
    }

    @Test
    void handle_EncounterNotFound_ThrowsException() {
        StubRepositories repositories = new StubRepositories();
        repositories.encounterQueryRepository.returnEmpty = true;
        CreatePrescriptionUseCase useCase = new CreatePrescriptionUseCase(
                repositories.prescriptionOrderRepository,
                repositories.encounterQueryRepository,
                repositories.emrRecordQueryRepository,
                TestAuditSupport.auditTrailService());

        BizException exception = assertThrows(
                BizException.class, () -> useCase.handle(command(), TestAuditSupport.auditContext()));

        assertEquals(ClinicalErrorCode.PRESCRIPTION_ENCOUNTER_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void handle_EncounterBelongsToAnotherDoctor_ThrowsException() {
        StubRepositories repositories = new StubRepositories();
        repositories.encounterQueryRepository.doctorId = 999L;
        CreatePrescriptionUseCase useCase = new CreatePrescriptionUseCase(
                repositories.prescriptionOrderRepository,
                repositories.encounterQueryRepository,
                repositories.emrRecordQueryRepository,
                TestAuditSupport.auditTrailService());

        BizException exception = assertThrows(
                BizException.class, () -> useCase.handle(command(), TestAuditSupport.auditContext()));

        assertEquals(ClinicalErrorCode.PRESCRIPTION_ENCOUNTER_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void handle_EmrMissing_ThrowsException() {
        StubRepositories repositories = new StubRepositories();
        repositories.emrRecordQueryRepository.returnEmpty = true;
        CreatePrescriptionUseCase useCase = new CreatePrescriptionUseCase(
                repositories.prescriptionOrderRepository,
                repositories.encounterQueryRepository,
                repositories.emrRecordQueryRepository,
                TestAuditSupport.auditTrailService());

        BizException exception = assertThrows(
                BizException.class, () -> useCase.handle(command(), TestAuditSupport.auditContext()));

        assertEquals(ClinicalErrorCode.PRESCRIPTION_EMR_RECORD_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void handle_PrescriptionAlreadyExists_ThrowsException() {
        StubRepositories repositories = new StubRepositories();
        repositories.prescriptionOrderRepository.prescriptionExists = true;
        CreatePrescriptionUseCase useCase = new CreatePrescriptionUseCase(
                repositories.prescriptionOrderRepository,
                repositories.encounterQueryRepository,
                repositories.emrRecordQueryRepository,
                TestAuditSupport.auditTrailService());

        BizException exception = assertThrows(
                BizException.class, () -> useCase.handle(command(), TestAuditSupport.auditContext()));

        assertEquals(ClinicalErrorCode.PRESCRIPTION_ALREADY_EXISTS.getCode(), exception.getCode());
    }

    private CreatePrescriptionCommand command() {
        return new CreatePrescriptionCommand(
                1L,
                200L,
                List.of(
                        new CreatePrescriptionCommand.PrescriptionItemCommand(
                                0, "阿莫西林胶囊", "0.25g*24粒", "每次2粒", "每日3次", "5天", new BigDecimal("30"), "粒", "口服"),
                        new CreatePrescriptionCommand.PrescriptionItemCommand(
                                1, "对乙酰氨基酚片", null, "每次1片", "必要时", "3天", new BigDecimal("10"), "片", "口服")));
    }

    private static class StubRepositories {
        private final StubPrescriptionOrderRepository prescriptionOrderRepository = new StubPrescriptionOrderRepository();
        private final StubEncounterQueryRepository encounterQueryRepository = new StubEncounterQueryRepository();
        private final StubEmrRecordQueryRepository emrRecordQueryRepository = new StubEmrRecordQueryRepository();
    }

    private static class StubPrescriptionOrderRepository implements PrescriptionOrderRepository {
        private PrescriptionOrder savedPrescription;
        private boolean prescriptionExists;

        @Override
        public void save(PrescriptionOrder prescriptionOrder) {
            this.savedPrescription = prescriptionOrder;
        }

        @Override
        public boolean existsByEncounterId(Long encounterId) {
            return prescriptionExists;
        }
    }

    private static class StubEncounterQueryRepository implements EncounterQueryRepository {
        private Long doctorId = 200L;
        private boolean returnEmpty;

        @Override
        public List<me.jianwen.mediask.domain.clinical.model.EncounterListItem> listByDoctorId(
                Long doctorId, VisitEncounterStatus status) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<EncounterDetail> findDetailByEncounterId(Long encounterId) {
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
                            LocalDate.parse("2002-03-18"))));
        }
    }

    private static class StubEmrRecordQueryRepository implements EmrRecordQueryRepository {
        private boolean returnEmpty;

        @Override
        public Optional<me.jianwen.mediask.domain.clinical.model.EmrRecord> findByEncounterId(Long encounterId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Long> findRecordIdByEncounterId(Long encounterId) {
            if (returnEmpty) {
                return Optional.empty();
            }
            return Optional.of(7101L);
        }

        @Override
        public Optional<me.jianwen.mediask.domain.clinical.model.EmrRecordAccess> findAccessByRecordId(Long recordId) {
            throw new UnsupportedOperationException();
        }
    }
}
