package me.jianwen.mediask.application.clinical.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import me.jianwen.mediask.application.clinical.query.GetPrescriptionDetailQuery;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.clinical.exception.ClinicalErrorCode;
import me.jianwen.mediask.domain.clinical.model.EncounterDetail;
import me.jianwen.mediask.domain.clinical.model.EncounterPatientSummary;
import me.jianwen.mediask.domain.clinical.model.PrescriptionItem;
import me.jianwen.mediask.domain.clinical.model.PrescriptionOrder;
import me.jianwen.mediask.domain.clinical.model.PrescriptionStatus;
import me.jianwen.mediask.domain.clinical.model.VisitEncounterStatus;
import me.jianwen.mediask.domain.clinical.port.EncounterQueryRepository;
import me.jianwen.mediask.domain.clinical.port.PrescriptionOrderQueryRepository;
import org.junit.jupiter.api.Test;

class GetPrescriptionDetailUseCaseTest {

    @Test
    void handle_WhenDoctorOwnsEncounter_ReturnsPrescription() {
        StubEncounterQueryRepository encounterQueryRepository = new StubEncounterQueryRepository();
        StubPrescriptionOrderQueryRepository prescriptionOrderQueryRepository = new StubPrescriptionOrderQueryRepository();
        GetPrescriptionDetailUseCase useCase = new GetPrescriptionDetailUseCase(
                prescriptionOrderQueryRepository, encounterQueryRepository);

        PrescriptionOrder result = useCase.handle(new GetPrescriptionDetailQuery(8101L, 2101L));

        assertEquals(8101L, result.encounterId());
        assertEquals("阿莫西林胶囊", result.items().getFirst().drugName());
    }

    @Test
    void handle_WhenEncounterNotFound_ThrowsException() {
        StubEncounterQueryRepository encounterQueryRepository = new StubEncounterQueryRepository();
        encounterQueryRepository.returnEmpty = true;
        GetPrescriptionDetailUseCase useCase = new GetPrescriptionDetailUseCase(
                new StubPrescriptionOrderQueryRepository(), encounterQueryRepository);

        BizException exception = assertThrows(BizException.class,
                () -> useCase.handle(new GetPrescriptionDetailQuery(8101L, 2101L)));

        assertEquals(ClinicalErrorCode.PRESCRIPTION_ENCOUNTER_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void handle_WhenDoctorMismatch_ThrowsException() {
        StubEncounterQueryRepository encounterQueryRepository = new StubEncounterQueryRepository();
        encounterQueryRepository.doctorId = 9999L;
        GetPrescriptionDetailUseCase useCase = new GetPrescriptionDetailUseCase(
                new StubPrescriptionOrderQueryRepository(), encounterQueryRepository);

        BizException exception = assertThrows(BizException.class,
                () -> useCase.handle(new GetPrescriptionDetailQuery(8101L, 2101L)));

        assertEquals(ClinicalErrorCode.PRESCRIPTION_ENCOUNTER_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void handle_WhenPrescriptionMissing_ThrowsException() {
        StubPrescriptionOrderQueryRepository prescriptionOrderQueryRepository = new StubPrescriptionOrderQueryRepository();
        prescriptionOrderQueryRepository.returnEmpty = true;
        GetPrescriptionDetailUseCase useCase = new GetPrescriptionDetailUseCase(
                prescriptionOrderQueryRepository, new StubEncounterQueryRepository());

        BizException exception = assertThrows(BizException.class,
                () -> useCase.handle(new GetPrescriptionDetailQuery(8101L, 2101L)));

        assertEquals(ClinicalErrorCode.PRESCRIPTION_NOT_FOUND.getCode(), exception.getCode());
    }

    private static class StubEncounterQueryRepository implements EncounterQueryRepository {
        private Long doctorId = 2101L;
        private boolean returnEmpty;

        @Override
        public List<me.jianwen.mediask.domain.clinical.model.EncounterListItem> listByDoctorId(Long doctorId, VisitEncounterStatus status) {
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
                            1001L,
                            "张患者",
                            "FEMALE",
                            3101L,
                            "内科",
                            LocalDate.parse("2026-04-18"),
                            "MORNING",
                            VisitEncounterStatus.IN_PROGRESS,
                            OffsetDateTime.parse("2026-04-18T10:00:00+08:00"),
                            null,
                            LocalDate.parse("2000-01-01"))));
        }

        @Override
        public Optional<me.jianwen.mediask.domain.clinical.model.EncounterAiSummary> findAiSummaryByEncounterId(Long encounterId) {
            throw new UnsupportedOperationException();
        }
    }

    private static class StubPrescriptionOrderQueryRepository implements PrescriptionOrderQueryRepository {
        private boolean returnEmpty;

        @Override
        public Optional<PrescriptionOrder> findByEncounterId(Long encounterId) {
            if (returnEmpty) {
                return Optional.empty();
            }
            return Optional.of(new PrescriptionOrder(
                    7101L,
                    "RX123456",
                    6102L,
                    encounterId,
                    1001L,
                    2101L,
                    PrescriptionStatus.DRAFT,
                    List.of(new PrescriptionItem(
                            8101L, 0, "阿莫西林胶囊", null, "每次2粒", "每日3次", "5天", new BigDecimal("30"), "粒", "口服")),
                    0,
                    Instant.parse("2026-04-18T02:00:00Z"),
                    Instant.parse("2026-04-18T02:00:00Z")));
        }
    }
}
