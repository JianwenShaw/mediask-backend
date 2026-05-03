package me.jianwen.mediask.application.user.usecase;

import me.jianwen.mediask.application.audit.AuditResourceTypes;
import me.jianwen.mediask.application.audit.model.AuditContext;
import me.jianwen.mediask.application.user.query.GetAdminPatientDetailQuery;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.audit.model.DataAccessPurposeCode;
import me.jianwen.mediask.domain.user.exception.UserErrorCode;
import me.jianwen.mediask.domain.user.model.AdminPatientDetail;
import me.jianwen.mediask.domain.user.port.AdminPatientQueryRepository;
import org.springframework.transaction.annotation.Transactional;

public class GetAdminPatientDetailUseCase {

    private final AdminPatientQueryRepository adminPatientQueryRepository;
    private final me.jianwen.mediask.application.audit.usecase.AuditTrailService auditTrailService;

    public GetAdminPatientDetailUseCase(
            AdminPatientQueryRepository adminPatientQueryRepository,
            me.jianwen.mediask.application.audit.usecase.AuditTrailService auditTrailService) {
        this.adminPatientQueryRepository = adminPatientQueryRepository;
        this.auditTrailService = auditTrailService;
    }

    @Transactional(readOnly = true)
    public AdminPatientDetail handle(GetAdminPatientDetailQuery query, AuditContext auditContext) {
        AdminPatientDetail detail = adminPatientQueryRepository.findDetailByPatientId(query.patientId())
                .orElseThrow(() -> new BizException(UserErrorCode.ADMIN_PATIENT_NOT_FOUND));
        auditTrailService.recordAllowedDataAccess(
                auditContext,
                AuditResourceTypes.PATIENT_PROFILE,
                String.valueOf(detail.patientId()),
                detail.userId(),
                null,
                DataAccessPurposeCode.ADMIN_OPERATION);
        return detail;
    }
}
