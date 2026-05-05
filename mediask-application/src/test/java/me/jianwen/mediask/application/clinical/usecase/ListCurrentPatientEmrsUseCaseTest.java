package me.jianwen.mediask.application.clinical.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import me.jianwen.mediask.application.TestAuditSupport;
import me.jianwen.mediask.application.clinical.query.ListCurrentPatientEmrsQuery;
import me.jianwen.mediask.domain.clinical.model.EmrRecord;
import me.jianwen.mediask.domain.clinical.model.EmrRecordAccess;
import me.jianwen.mediask.domain.clinical.model.EmrRecordListItem;
import me.jianwen.mediask.domain.clinical.model.EmrRecordStatus;
import me.jianwen.mediask.domain.clinical.port.EmrRecordQueryRepository;
import org.junit.jupiter.api.Test;

class ListCurrentPatientEmrsUseCaseTest {

    @Test
    void handle_WhenCalled_ListOwnEmrs() {
        StubEmrRecordQueryRepository repository = new StubEmrRecordQueryRepository();
        ListCurrentPatientEmrsUseCase useCase =
                new ListCurrentPatientEmrsUseCase(repository, TestAuditSupport.auditTrailService());

        List<EmrRecordListItem> result =
                useCase.handle(new ListCurrentPatientEmrsQuery(1001L), TestAuditSupport.auditContext());

        assertEquals(1001L, repository.lastPatientUserId);
        assertEquals(null, repository.lastExcludeEncounterId);
        assertEquals(1, result.size());
        assertEquals(7101L, result.getFirst().recordId());
        assertEquals("李医生", result.getFirst().doctorName());
    }

    private static final class StubEmrRecordQueryRepository implements EmrRecordQueryRepository {

        private Long lastPatientUserId;
        private Long lastExcludeEncounterId;

        @Override
        public Optional<EmrRecord> findByEncounterId(Long encounterId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<EmrRecordListItem> listByPatientUserId(Long patientUserId, Long excludeEncounterId) {
            this.lastPatientUserId = patientUserId;
            this.lastExcludeEncounterId = excludeEncounterId;
            return List.of(new EmrRecordListItem(
                    7101L,
                    "EMR20260504001",
                    8104L,
                    EmrRecordStatus.DRAFT,
                    3102L,
                    "全科门诊",
                    3203L,
                    "李医生",
                    LocalDate.parse("2026-05-04"),
                    "发热缓解后复诊评估",
                    OffsetDateTime.parse("2026-05-04T15:10:00+08:00")));
        }

        @Override
        public Optional<Long> findRecordIdByEncounterId(Long encounterId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<EmrRecordAccess> findAccessByRecordId(Long recordId) {
            throw new UnsupportedOperationException();
        }
    }
}
