package me.jianwen.mediask.application.clinical.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import me.jianwen.mediask.application.clinical.query.GetEncounterDetailQuery;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.clinical.exception.ClinicalErrorCode;
import me.jianwen.mediask.domain.clinical.model.EncounterDetail;
import me.jianwen.mediask.domain.clinical.model.EncounterListItem;
import me.jianwen.mediask.domain.clinical.model.EncounterPatientSummary;
import me.jianwen.mediask.domain.clinical.model.VisitEncounterStatus;
import me.jianwen.mediask.domain.clinical.port.EncounterQueryRepository;
import org.junit.jupiter.api.Test;

class GetEncounterDetailUseCaseTest {

    @Test
    void handle_WhenQueryProvided_ReturnEncounterDetail() {
        StubEncounterQueryRepository repository = new StubEncounterQueryRepository();
        GetEncounterDetailUseCase useCase = new GetEncounterDetailUseCase(repository);

        EncounterDetail result = useCase.handle(new GetEncounterDetailQuery(8101L, 2101L));

        assertEquals(8101L, repository.lastEncounterId);
        assertEquals(8101L, result.encounterId());
        assertEquals(6101L, result.registrationId());
        assertEquals("李患者", result.patientSummary().patientName());
    }

    @Test
    void handle_WhenEncounterMissing_ThrowNotFound() {
        StubEncounterQueryRepository repository = new StubEncounterQueryRepository();
        repository.returnEmpty = true;
        GetEncounterDetailUseCase useCase = new GetEncounterDetailUseCase(repository);

        BizException exception =
                assertThrows(BizException.class, () -> useCase.handle(new GetEncounterDetailQuery(9999L, 2101L)));

        assertEquals(ClinicalErrorCode.ENCOUNTER_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void handle_WhenEncounterBelongsToAnotherDoctor_ThrowAccessDenied() {
        StubEncounterQueryRepository repository = new StubEncounterQueryRepository();
        repository.doctorId = 2102L;
        GetEncounterDetailUseCase useCase = new GetEncounterDetailUseCase(repository);

        BizException exception =
                assertThrows(BizException.class, () -> useCase.handle(new GetEncounterDetailQuery(8101L, 2101L)));

        assertEquals(ClinicalErrorCode.ENCOUNTER_ACCESS_DENIED.getCode(), exception.getCode());
    }

    private static final class StubEncounterQueryRepository implements EncounterQueryRepository {

        private Long lastEncounterId;
        private Long doctorId = 2101L;
        private boolean returnEmpty;

        @Override
        public List<EncounterListItem> listByDoctorId(Long doctorId, VisitEncounterStatus status) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<EncounterDetail> findDetailByEncounterId(Long encounterId) {
            this.lastEncounterId = encounterId;
            if (returnEmpty) {
                return Optional.empty();
            }
            return Optional.of(new EncounterDetail(
                    8101L,
                    6101L,
                    this.doctorId,
                    new EncounterPatientSummary(
                            2003L,
                            "李患者",
                            "FEMALE",
                            3101L,
                            "心内科",
                            LocalDate.parse("2026-04-03"),
                            "MORNING",
                            VisitEncounterStatus.SCHEDULED,
                            OffsetDateTime.parse("2026-04-03T09:00:00+08:00"),
                            null,
                            LocalDate.parse("2003-10-02"))));
        }
    }
}
