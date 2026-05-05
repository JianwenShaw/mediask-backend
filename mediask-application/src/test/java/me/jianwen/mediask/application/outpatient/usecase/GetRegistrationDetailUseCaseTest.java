package me.jianwen.mediask.application.outpatient.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import me.jianwen.mediask.application.outpatient.query.GetRegistrationDetailQuery;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.outpatient.exception.OutpatientErrorCode;
import me.jianwen.mediask.domain.outpatient.model.ClinicSessionPeriodCode;
import me.jianwen.mediask.domain.outpatient.model.RegistrationDetail;
import me.jianwen.mediask.domain.outpatient.model.RegistrationListItem;
import me.jianwen.mediask.domain.outpatient.model.RegistrationStatus;
import me.jianwen.mediask.domain.outpatient.port.RegistrationOrderQueryRepository;
import org.junit.jupiter.api.Test;

class GetRegistrationDetailUseCaseTest {

    @Test
    void handle_WhenQueryProvided_ReturnRegistrationDetail() {
        StubRegistrationOrderQueryRepository repository = new StubRegistrationOrderQueryRepository();
        GetRegistrationDetailUseCase useCase = new GetRegistrationDetailUseCase(repository);

        RegistrationDetail result = useCase.handle(new GetRegistrationDetailQuery(6101L, 2003L));

        assertEquals(6101L, repository.lastRegistrationId);
        assertEquals(2003L, repository.lastPatientUserId);
        assertEquals("REG6101", result.orderNo());
        assertEquals("张医生", result.doctorName());
    }

    @Test
    void handle_WhenRegistrationMissing_ThrowNotFound() {
        StubRegistrationOrderQueryRepository repository = new StubRegistrationOrderQueryRepository();
        repository.returnEmpty = true;
        GetRegistrationDetailUseCase useCase = new GetRegistrationDetailUseCase(repository);

        BizException exception =
                assertThrows(BizException.class, () -> useCase.handle(new GetRegistrationDetailQuery(9999L, 2003L)));

        assertEquals(OutpatientErrorCode.REGISTRATION_NOT_FOUND.getCode(), exception.getCode());
    }

    private static final class StubRegistrationOrderQueryRepository implements RegistrationOrderQueryRepository {

        private Long lastPatientUserId;
        private Long lastRegistrationId;
        private boolean returnEmpty;

        @Override
        public List<RegistrationListItem> listByPatientUserId(Long patientUserId, RegistrationStatus status) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<RegistrationDetail> findDetailByPatientUserIdAndRegistrationId(Long patientUserId, Long registrationId) {
            this.lastPatientUserId = patientUserId;
            this.lastRegistrationId = registrationId;
            if (returnEmpty) {
                return Optional.empty();
            }
            return Optional.of(new RegistrationDetail(
                    6101L,
                    2003L,
                    "REG6101",
                    RegistrationStatus.CONFIRMED,
                    OffsetDateTime.parse("2026-04-02T10:00:00+08:00"),
                    "session-1",
                    4101L,
                    5101L,
                    3101L,
                    "神经内科",
                    2101L,
                    "张医生",
                    LocalDate.parse("2026-04-03"),
                    ClinicSessionPeriodCode.MORNING,
                    new BigDecimal("18.00"),
                    null,
                    null));
        }

        @Override
        public Optional<String> findSourceAiSessionIdByRegistrationId(Long registrationId) {
            throw new UnsupportedOperationException();
        }
    }
}
