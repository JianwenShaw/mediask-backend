package me.jianwen.mediask.application.outpatient.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import me.jianwen.mediask.domain.outpatient.model.ClinicSessionListItem;
import me.jianwen.mediask.domain.outpatient.model.ClinicSessionPeriodCode;
import me.jianwen.mediask.domain.outpatient.model.ClinicSessionSlotListItem;
import me.jianwen.mediask.domain.outpatient.model.ClinicType;
import me.jianwen.mediask.domain.outpatient.port.ClinicSessionQueryRepository;
import org.junit.jupiter.api.Test;

class ListClinicSessionSlotsUseCaseTest {

    @Test
    void handle_WhenClinicSessionIdProvided_DelegateToRepository() {
        StubClinicSessionQueryRepository repository = new StubClinicSessionQueryRepository();
        ListClinicSessionSlotsUseCase useCase = new ListClinicSessionSlotsUseCase(repository);

        List<ClinicSessionSlotListItem> result = useCase.handle(4101L);

        assertEquals(4101L, repository.lastClinicSessionId);
        assertEquals(1, result.size());
        assertEquals(5101L, result.getFirst().clinicSlotId());
    }

    private static final class StubClinicSessionQueryRepository implements ClinicSessionQueryRepository {

        private Long lastClinicSessionId;

        @Override
        public List<ClinicSessionListItem> listOpenSessions(Long departmentId, LocalDate dateFrom, LocalDate dateTo) {
            return List.of(new ClinicSessionListItem(
                    4101L,
                    3101L,
                    "内科",
                    2101L,
                    "张医生",
                    LocalDate.of(2026, 4, 10),
                    ClinicSessionPeriodCode.MORNING,
                    ClinicType.GENERAL,
                    8,
                    new java.math.BigDecimal("25.00")));
        }

        @Override
        public List<ClinicSessionSlotListItem> listAvailableSlotsBySessionId(Long clinicSessionId) {
            this.lastClinicSessionId = clinicSessionId;
            return List.of(new ClinicSessionSlotListItem(
                    5101L,
                    1,
                    OffsetDateTime.parse("2026-04-21T08:30:00+08:00"),
                    OffsetDateTime.parse("2026-04-21T08:40:00+08:00")));
        }
    }
}
