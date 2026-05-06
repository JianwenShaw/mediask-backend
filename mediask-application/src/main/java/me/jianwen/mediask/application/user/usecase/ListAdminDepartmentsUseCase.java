package me.jianwen.mediask.application.user.usecase;

import me.jianwen.mediask.application.user.query.ListAdminDepartmentsQuery;
import me.jianwen.mediask.common.pagination.PageData;
import me.jianwen.mediask.domain.user.model.AdminDepartmentListItem;
import me.jianwen.mediask.domain.user.port.AdminDepartmentQueryRepository;
import org.springframework.transaction.annotation.Transactional;

public class ListAdminDepartmentsUseCase {

    private final AdminDepartmentQueryRepository adminDepartmentQueryRepository;

    public ListAdminDepartmentsUseCase(AdminDepartmentQueryRepository adminDepartmentQueryRepository) {
        this.adminDepartmentQueryRepository = adminDepartmentQueryRepository;
    }

    @Transactional(readOnly = true)
    public PageData<AdminDepartmentListItem> handle(ListAdminDepartmentsQuery query) {
        return adminDepartmentQueryRepository.pageByKeyword(query.keyword(), query.pageQuery());
    }
}
