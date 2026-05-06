package me.jianwen.mediask.application.user.usecase;

import me.jianwen.mediask.application.audit.AuditResourceTypes;
import me.jianwen.mediask.application.audit.model.AuditContext;
import me.jianwen.mediask.application.audit.usecase.AuditTrailService;
import me.jianwen.mediask.application.user.query.GetAdminDoctorDetailQuery;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.audit.model.DataAccessPurposeCode;
import me.jianwen.mediask.domain.user.exception.UserErrorCode;
import me.jianwen.mediask.domain.user.model.AdminDoctorDetail;
import me.jianwen.mediask.domain.user.port.AdminDoctorQueryRepository;
import org.springframework.transaction.annotation.Transactional;

public class GetAdminDoctorDetailUseCase {

    private final AdminDoctorQueryRepository adminDoctorQueryRepository;
    private final AuditTrailService auditTrailService;

    public GetAdminDoctorDetailUseCase(
            AdminDoctorQueryRepository adminDoctorQueryRepository,
            AuditTrailService auditTrailService) {
        this.adminDoctorQueryRepository = adminDoctorQueryRepository;
        this.auditTrailService = auditTrailService;
    }

    @Transactional(readOnly = true)
    public AdminDoctorDetail handle(GetAdminDoctorDetailQuery query, AuditContext auditContext) {
        AdminDoctorDetail detail = adminDoctorQueryRepository.findDetailByDoctorId(query.doctorId())
                .orElseThrow(() -> new BizException(UserErrorCode.ADMIN_DOCTOR_NOT_FOUND));
        auditTrailService.recordAllowedDataAccess(
                auditContext,
                AuditResourceTypes.DOCTOR_PROFILE,
                String.valueOf(detail.doctorId()),
                detail.userId(),
                null,
                DataAccessPurposeCode.ADMIN_OPERATION);
        return detail;
    }
}
