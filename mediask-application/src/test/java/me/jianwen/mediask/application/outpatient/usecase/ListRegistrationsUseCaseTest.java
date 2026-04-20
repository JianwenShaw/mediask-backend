package me.jianwen.mediask.application.outpatient.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import me.jianwen.mediask.application.outpatient.query.ListRegistrationsQuery;
import me.jianwen.mediask.domain.outpatient.model.RegistrationDetail;
import me.jianwen.mediask.domain.outpatient.model.RegistrationListItem;
import me.jianwen.mediask.domain.outpatient.model.RegistrationStatus;
import me.jianwen.mediask.domain.outpatient.port.RegistrationOrderQueryRepository;
import org.junit.jupiter.api.Test;

class ListRegistrationsUseCaseTest {

    @Test
    void handle_WhenQueryProvided_DelegateToRepository() {
        StubRegistrationOrderQueryRepository repository = new StubRegistrationOrderQueryRepository();
        ListRegistrationsUseCase useCase = new ListRegistrationsUseCase(repository);

        List<RegistrationListItem> result =
                useCase.handle(new ListRegistrationsQuery(2003L, RegistrationStatus.CONFIRMED));

        assertEquals(1, result.size());
        assertEquals(2003L, repository.lastPatientUserId);
        assertEquals(RegistrationStatus.CONFIRMED, repository.lastStatus);
    }

    private static final class StubRegistrationOrderQueryRepository implements RegistrationOrderQueryRepository {

        private Long lastPatientUserId;
        private RegistrationStatus lastStatus;

        @Override
        public List<RegistrationListItem> listByPatientUserId(Long patientUserId, RegistrationStatus status) {
            this.lastPatientUserId = patientUserId;
            this.lastStatus = status;
            return List.of(new RegistrationListItem(
                    6101L,
                    "REG6101",
                    RegistrationStatus.CONFIRMED,
                    OffsetDateTime.parse("2026-04-02T10:00:00+08:00"),
                    7101L));
        }

        @Override
        public Optional<RegistrationDetail> findDetailByPatientUserIdAndRegistrationId(Long patientUserId, Long registrationId) {
            throw new UnsupportedOperationException();
        }
    }
}
