package me.jianwen.mediask.application.clinical.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import me.jianwen.mediask.application.clinical.query.ListEncountersQuery;
import me.jianwen.mediask.domain.clinical.model.EncounterListItem;
import me.jianwen.mediask.domain.clinical.model.VisitEncounterStatus;
import me.jianwen.mediask.domain.clinical.port.EncounterQueryRepository;
import org.junit.jupiter.api.Test;

class ListEncountersUseCaseTest {

    @Test
    void handle_WhenQueryProvided_DelegateToRepository() {
        StubEncounterQueryRepository repository = new StubEncounterQueryRepository();
        ListEncountersUseCase useCase = new ListEncountersUseCase(repository);

        List<EncounterListItem> result = useCase.handle(new ListEncountersQuery(2101L, VisitEncounterStatus.SCHEDULED));

        assertEquals(1, result.size());
        assertEquals(2101L, repository.lastDoctorId);
        assertEquals(VisitEncounterStatus.SCHEDULED, repository.lastStatus);
    }

    private static final class StubEncounterQueryRepository implements EncounterQueryRepository {

        private Long lastDoctorId;
        private VisitEncounterStatus lastStatus;

        @Override
        public List<EncounterListItem> listByDoctorId(Long doctorId, VisitEncounterStatus status) {
            this.lastDoctorId = doctorId;
            this.lastStatus = status;
            return List.of(new EncounterListItem(
                    8101L,
                    6101L,
                    2003L,
                    "李患者",
                    3101L,
                    "心内科",
                    LocalDate.parse("2026-04-03"),
                    "MORNING",
                    VisitEncounterStatus.SCHEDULED,
                    OffsetDateTime.parse("2026-04-03T09:00:00+08:00"),
                    null));
        }
    }
}
