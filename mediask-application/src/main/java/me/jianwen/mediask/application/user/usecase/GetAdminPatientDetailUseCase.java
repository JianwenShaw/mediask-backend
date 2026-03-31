package me.jianwen.mediask.application.user.usecase;

import me.jianwen.mediask.application.user.query.GetAdminPatientDetailQuery;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.user.exception.UserErrorCode;
import me.jianwen.mediask.domain.user.model.AdminPatientDetail;
import me.jianwen.mediask.domain.user.port.AdminPatientQueryRepository;
import org.springframework.transaction.annotation.Transactional;

public class GetAdminPatientDetailUseCase {

    private final AdminPatientQueryRepository adminPatientQueryRepository;

    public GetAdminPatientDetailUseCase(AdminPatientQueryRepository adminPatientQueryRepository) {
        this.adminPatientQueryRepository = adminPatientQueryRepository;
    }

    @Transactional(readOnly = true)
    public AdminPatientDetail handle(GetAdminPatientDetailQuery query) {
        return adminPatientQueryRepository.findDetailByPatientId(query.patientId())
                .orElseThrow(() -> new BizException(UserErrorCode.ADMIN_PATIENT_NOT_FOUND));
    }
}
