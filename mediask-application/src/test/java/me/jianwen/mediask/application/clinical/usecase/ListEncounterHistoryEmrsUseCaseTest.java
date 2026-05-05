package me.jianwen.mediask.application.clinical.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import me.jianwen.mediask.application.TestAuditSupport;
import me.jianwen.mediask.application.clinical.query.ListEncounterHistoryEmrsQuery;
import me.jianwen.mediask.domain.clinical.model.EmrRecord;
import me.jianwen.mediask.domain.clinical.model.EmrRecordAccess;
import me.jianwen.mediask.domain.clinical.model.EmrRecordListItem;
import me.jianwen.mediask.domain.clinical.model.EmrRecordStatus;
import me.jianwen.mediask.domain.clinical.model.EncounterDetail;
import me.jianwen.mediask.domain.clinical.model.EncounterListItem;
import me.jianwen.mediask.domain.clinical.model.EncounterPatientSummary;
import me.jianwen.mediask.domain.clinical.model.VisitEncounterStatus;
import me.jianwen.mediask.domain.clinical.port.EmrRecordQueryRepository;
import me.jianwen.mediask.domain.clinical.port.EncounterQueryRepository;
import org.junit.jupiter.api.Test;

class ListEncounterHistoryEmrsUseCaseTest {

    @Test
    void handle_WhenCalled_ListPatientHistoryExcludingCurrentEncounter() {
        StubEncounterQueryRepository encounterQueryRepository = new StubEncounterQueryRepository();
        StubEmrRecordQueryRepository emrRecordQueryRepository = new StubEmrRecordQueryRepository();
        ListEncounterHistoryEmrsUseCase useCase = new ListEncounterHistoryEmrsUseCase(
                encounterQueryRepository, emrRecordQueryRepository, TestAuditSupport.auditTrailService());

        List<EmrRecordListItem> result =
                useCase.handle(new ListEncounterHistoryEmrsQuery(8104L), TestAuditSupport.auditContext());

        assertEquals(8104L, encounterQueryRepository.lastEncounterId);
        assertEquals(2003L, emrRecordQueryRepository.lastPatientUserId);
        assertEquals(8104L, emrRecordQueryRepository.lastExcludeEncounterId);
        assertEquals(1, result.size());
        assertEquals(8102L, result.getFirst().encounterId());
    }

    private static final class StubEncounterQueryRepository implements EncounterQueryRepository {

        private Long lastEncounterId;

        @Override
        public List<EncounterListItem> listByDoctorId(Long doctorId, VisitEncounterStatus status) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<EncounterDetail> findDetailByEncounterId(Long encounterId) {
            this.lastEncounterId = encounterId;
            return Optional.of(new EncounterDetail(
                    encounterId,
                    6104L,
                    3203L,
                    new EncounterPatientSummary(
                            2003L,
                            "张患者",
                            "FEMALE",
                            3102L,
                            "全科门诊",
                            LocalDate.parse("2026-05-04"),
                            "AFTERNOON",
                            VisitEncounterStatus.IN_PROGRESS,
                            OffsetDateTime.parse("2026-05-04T15:00:00+08:00"),
                            null,
                            LocalDate.parse("1995-03-18"))));
        }
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
                    7100L,
                    "EMR20260430001",
                    8102L,
                    EmrRecordStatus.DRAFT,
                    3103L,
                    "呼吸内科",
                    3201L,
                    "王医生",
                    LocalDate.parse("2026-04-30"),
                    "发热伴咽痛 2 天",
                    OffsetDateTime.parse("2026-04-30T09:15:00+08:00")));
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
