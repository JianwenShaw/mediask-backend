package me.jianwen.mediask.api;

import java.util.List;
import java.util.Optional;
import me.jianwen.mediask.api.audit.AuditApiSupport;
import me.jianwen.mediask.application.audit.usecase.AuditTrailService;
import me.jianwen.mediask.application.audit.usecase.RecordAuditEventUseCase;
import me.jianwen.mediask.application.audit.usecase.RecordDataAccessLogUseCase;
import me.jianwen.mediask.domain.clinical.model.EncounterDetail;
import me.jianwen.mediask.domain.clinical.model.EncounterListItem;
import me.jianwen.mediask.domain.clinical.model.VisitEncounterStatus;
import me.jianwen.mediask.domain.clinical.port.EncounterQueryRepository;
import me.jianwen.mediask.domain.user.port.AdminPatientQueryRepository;

public final class TestAuditSupport {

    private TestAuditSupport() {
    }

    public static AuditApiSupport auditApiSupport() {
        return new AuditApiSupport(auditTrailService());
    }

    public static AuditTrailService auditTrailService() {
        return new AuditTrailService(
                new RecordAuditEventUseCase(record -> {}),
                new RecordDataAccessLogUseCase(record -> {}));
    }

    public static EncounterQueryRepository emptyEncounterQueryRepository() {
        return new EncounterQueryRepository() {
            @Override
            public List<EncounterListItem> listByDoctorId(Long doctorId, VisitEncounterStatus status) {
                return List.of();
            }

            @Override
            public Optional<EncounterDetail> findDetailByEncounterId(Long encounterId) {
                return Optional.empty();
            }
        };
    }

    public static AdminPatientQueryRepository emptyAdminPatientQueryRepository() {
        return new AdminPatientQueryRepository() {
            @Override
            public me.jianwen.mediask.common.pagination.PageData<me.jianwen.mediask.domain.user.model.AdminPatientListItem> pageByKeyword(
                    String keyword,
                    me.jianwen.mediask.common.pagination.PageQuery pageQuery) {
                return me.jianwen.mediask.common.pagination.PageQuery.toPageData(pageQuery, 0, List.of());
            }

            @Override
            public Optional<me.jianwen.mediask.domain.user.model.AdminPatientDetail> findDetailByPatientId(Long patientId) {
                return Optional.empty();
            }
        };
    }
}
