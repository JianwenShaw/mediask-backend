package me.jianwen.mediask.application.clinical.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import me.jianwen.mediask.application.TestAuditSupport;
import me.jianwen.mediask.application.audit.usecase.RecordDataAccessLogUseCase;
import me.jianwen.mediask.application.clinical.query.GetEmrDetailQuery;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.audit.model.DataAccessPurposeCode;
import me.jianwen.mediask.domain.clinical.exception.ClinicalErrorCode;
import me.jianwen.mediask.domain.clinical.model.EmrDiagnosis;
import me.jianwen.mediask.domain.clinical.model.EmrRecord;
import me.jianwen.mediask.domain.clinical.model.EmrRecordAccess;
import me.jianwen.mediask.domain.clinical.model.EmrRecordListItem;
import me.jianwen.mediask.domain.clinical.model.EmrRecordStatus;
import me.jianwen.mediask.domain.clinical.port.EmrRecordQueryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

class GetEmrDetailUseCaseTest {

    @Test
    void handle_WhenRecordExists_ReturnRecord() {
        StubEmrRecordQueryRepository repository = new StubEmrRecordQueryRepository();
        GetEmrDetailUseCase useCase = new GetEmrDetailUseCase(repository, TestAuditSupport.auditTrailService());

        EmrRecord result = useCase.handle(
                new GetEmrDetailQuery(8101L), TestAuditSupport.auditContext(), DataAccessPurposeCode.TREATMENT);

        assertEquals(8101L, repository.lastEncounterId);
        assertEquals(7101L, result.recordId());
        assertEquals("Detailed medical examination findings...", result.content());
    }

    @Test
    void handle_WhenRecordMissing_ThrowNotFound() {
        StubEmrRecordQueryRepository repository = new StubEmrRecordQueryRepository();
        repository.returnEmpty = true;
        GetEmrDetailUseCase useCase = new GetEmrDetailUseCase(repository, TestAuditSupport.auditTrailService());

        BizException exception = assertThrows(
                BizException.class,
                () -> useCase.handle(
                        new GetEmrDetailQuery(9999L),
                        TestAuditSupport.auditContext(),
                        DataAccessPurposeCode.TREATMENT));

        assertEquals(ClinicalErrorCode.EMR_RECORD_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void recordDataAccessLogUseCase_ShouldStartNewTransaction() throws NoSuchMethodException {
        Method handleMethod =
                RecordDataAccessLogUseCase.class.getMethod("handle", me.jianwen.mediask.application.audit.command.RecordDataAccessLogCommand.class);

        Transactional transactional = handleMethod.getAnnotation(Transactional.class);

        assertTrue(transactional != null);
        assertEquals(Propagation.REQUIRES_NEW, transactional.propagation());
    }

    private static final class StubEmrRecordQueryRepository implements EmrRecordQueryRepository {

        private Long lastEncounterId;
        private boolean returnEmpty;

        @Override
        public Optional<EmrRecord> findByEncounterId(Long encounterId) {
            this.lastEncounterId = encounterId;
            if (returnEmpty) {
                return Optional.empty();
            }
            return Optional.of(new EmrRecord(
                    7101L,
                    "EMR123456",
                    8101L,
                    1001L,
                    2101L,
                    3101L,
                    EmrRecordStatus.DRAFT,
                    "Headache and congestion",
                    "Detailed medical examination findings...",
                    List.of(EmrDiagnosis.createPrimary("J01.90", "Acute sinusitis", 0)),
                    0,
                    Instant.parse("2026-04-19T10:00:00Z"),
                    Instant.parse("2026-04-19T10:00:00Z")));
        }

        @Override
        public List<EmrRecordListItem> listByPatientUserId(Long patientUserId, Long excludeEncounterId) {
            throw new UnsupportedOperationException();
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
