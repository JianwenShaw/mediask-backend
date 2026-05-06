package me.jianwen.mediask.application.user.usecase;

import me.jianwen.mediask.application.user.query.GetAdminDepartmentDetailQuery;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.user.exception.UserErrorCode;
import me.jianwen.mediask.domain.user.model.AdminDepartmentDetail;
import me.jianwen.mediask.domain.user.port.AdminDepartmentQueryRepository;
import org.springframework.transaction.annotation.Transactional;

public class GetAdminDepartmentDetailUseCase {

    private final AdminDepartmentQueryRepository adminDepartmentQueryRepository;

    public GetAdminDepartmentDetailUseCase(AdminDepartmentQueryRepository adminDepartmentQueryRepository) {
        this.adminDepartmentQueryRepository = adminDepartmentQueryRepository;
    }

    @Transactional(readOnly = true)
    public AdminDepartmentDetail handle(GetAdminDepartmentDetailQuery query) {
        return adminDepartmentQueryRepository.findDetailById(query.id())
                .orElseThrow(() -> new BizException(UserErrorCode.ADMIN_DEPARTMENT_NOT_FOUND));
    }
}
