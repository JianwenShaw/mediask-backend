package me.jianwen.mediask.application.outpatient.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import me.jianwen.mediask.application.outpatient.query.ListClinicSessionsQuery;
import me.jianwen.mediask.domain.outpatient.model.ClinicSessionListItem;
import me.jianwen.mediask.domain.outpatient.model.ClinicSessionPeriodCode;
import me.jianwen.mediask.domain.outpatient.model.ClinicType;
import me.jianwen.mediask.domain.outpatient.port.ClinicSessionQueryRepository;
import org.junit.jupiter.api.Test;

class ListClinicSessionsUseCaseTest {

    @Test
    void handle_WhenQueryProvided_DelegateToRepositoryWithoutDefaultDates() {
        StubClinicSessionQueryRepository repository = new StubClinicSessionQueryRepository();
        ListClinicSessionsUseCase useCase = new ListClinicSessionsUseCase(repository);
        ListClinicSessionsQuery query = new ListClinicSessionsQuery(3101L, null, LocalDate.of(2026, 4, 30));

        List<ClinicSessionListItem> result = useCase.handle(query);

        assertEquals(1, result.size());
        assertEquals(3101L, repository.lastDepartmentId);
        assertEquals(null, repository.lastDateFrom);
        assertEquals(LocalDate.of(2026, 4, 30), repository.lastDateTo);
    }

    private static final class StubClinicSessionQueryRepository implements ClinicSessionQueryRepository {

        private Long lastDepartmentId;
        private LocalDate lastDateFrom;
        private LocalDate lastDateTo;

        @Override
        public List<ClinicSessionListItem> listOpenSessions(Long departmentId, LocalDate dateFrom, LocalDate dateTo) {
            this.lastDepartmentId = departmentId;
            this.lastDateFrom = dateFrom;
            this.lastDateTo = dateTo;
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
                    new BigDecimal("25.00")));
        }
    }
}
