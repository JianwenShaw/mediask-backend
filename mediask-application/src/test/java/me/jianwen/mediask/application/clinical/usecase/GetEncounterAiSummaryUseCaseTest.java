package me.jianwen.mediask.application.clinical.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import me.jianwen.mediask.application.clinical.query.GetEncounterAiSummaryQuery;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.ai.model.AiCitation;
import me.jianwen.mediask.domain.ai.model.RecommendedDepartment;
import me.jianwen.mediask.domain.ai.model.RiskLevel;
import me.jianwen.mediask.domain.clinical.exception.ClinicalErrorCode;
import me.jianwen.mediask.domain.clinical.model.EncounterAiSummary;
import me.jianwen.mediask.domain.clinical.model.EncounterDetail;
import me.jianwen.mediask.domain.clinical.model.EncounterListItem;
import me.jianwen.mediask.domain.clinical.model.EncounterPatientSummary;
import me.jianwen.mediask.domain.clinical.model.VisitEncounterStatus;
import me.jianwen.mediask.domain.clinical.port.EncounterQueryRepository;
import org.junit.jupiter.api.Test;

class GetEncounterAiSummaryUseCaseTest {

    @Test
    void handle_WhenQueryProvided_ReturnEncounterAiSummary() {
        StubEncounterQueryRepository repository = new StubEncounterQueryRepository();
        GetEncounterAiSummaryUseCase useCase = new GetEncounterAiSummaryUseCase(repository);

        EncounterAiSummary result = useCase.handle(new GetEncounterAiSummaryQuery(8101L, 2101L));

        assertEquals(8101L, repository.lastDetailEncounterId);
        assertEquals(8101L, repository.lastAiSummaryEncounterId);
        assertEquals(8101L, result.encounterId());
        assertEquals(9001L, result.sessionId());
        assertEquals("头痛三天", result.chiefComplaintSummary());
        assertEquals("患者自述头痛三天伴低热", result.structuredSummary());
    }

    @Test
    void handle_WhenEncounterMissing_ThrowNotFound() {
        StubEncounterQueryRepository repository = new StubEncounterQueryRepository();
        repository.returnEmptyDetail = true;
        GetEncounterAiSummaryUseCase useCase = new GetEncounterAiSummaryUseCase(repository);

        BizException exception =
                assertThrows(BizException.class, () -> useCase.handle(new GetEncounterAiSummaryQuery(9999L, 2101L)));

        assertEquals(ClinicalErrorCode.ENCOUNTER_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void handle_WhenEncounterBelongsToAnotherDoctor_ThrowAccessDenied() {
        StubEncounterQueryRepository repository = new StubEncounterQueryRepository();
        repository.doctorId = 2102L;
        GetEncounterAiSummaryUseCase useCase = new GetEncounterAiSummaryUseCase(repository);

        BizException exception =
                assertThrows(BizException.class, () -> useCase.handle(new GetEncounterAiSummaryQuery(8101L, 2101L)));

        assertEquals(ClinicalErrorCode.ENCOUNTER_ACCESS_DENIED.getCode(), exception.getCode());
    }

    @Test
    void handle_WhenAiSummaryMissing_ThrowAiSummaryNotFound() {
        StubEncounterQueryRepository repository = new StubEncounterQueryRepository();
        repository.returnEmptyAiSummary = true;
        GetEncounterAiSummaryUseCase useCase = new GetEncounterAiSummaryUseCase(repository);

        BizException exception =
                assertThrows(BizException.class, () -> useCase.handle(new GetEncounterAiSummaryQuery(8101L, 2101L)));

        assertEquals(ClinicalErrorCode.ENCOUNTER_AI_SUMMARY_NOT_FOUND.getCode(), exception.getCode());
    }

    private static final class StubEncounterQueryRepository implements EncounterQueryRepository {

        private Long lastDetailEncounterId;
        private Long lastAiSummaryEncounterId;
        private Long doctorId = 2101L;
        private boolean returnEmptyDetail;
        private boolean returnEmptyAiSummary;

        @Override
        public List<EncounterListItem> listByDoctorId(Long doctorId, VisitEncounterStatus status) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<EncounterDetail> findDetailByEncounterId(Long encounterId) {
            this.lastDetailEncounterId = encounterId;
            if (returnEmptyDetail) {
                return Optional.empty();
            }
            return Optional.of(new EncounterDetail(
                    8101L,
                    6101L,
                    doctorId,
                    new EncounterPatientSummary(
                            2003L,
                            "李患者",
                            3101L,
                            "心内科",
                            LocalDate.parse("2026-04-03"),
                            "MORNING",
                            VisitEncounterStatus.SCHEDULED,
                            OffsetDateTime.parse("2026-04-03T09:00:00+08:00"),
                            null)));
        }

        @Override
        public Optional<EncounterAiSummary> findAiSummaryByEncounterId(Long encounterId) {
            this.lastAiSummaryEncounterId = encounterId;
            if (returnEmptyAiSummary) {
                return Optional.empty();
            }
            return Optional.of(new EncounterAiSummary(
                    8101L,
                    9001L,
                    "头痛三天",
                    "患者自述头痛三天伴低热",
                    RiskLevel.MEDIUM,
                    List.of(new RecommendedDepartment(3101L, "心内科", 1, "持续头痛需线下评估")),
                    List.of(new AiCitation(7001L, 1, 0.82D, "引用片段-1"))));
        }
    }
}
