package me.jianwen.mediask.application.user.usecase;

import me.jianwen.mediask.application.user.query.ListAdminDoctorsQuery;
import me.jianwen.mediask.common.pagination.PageData;
import me.jianwen.mediask.domain.user.model.AdminDoctorListItem;
import me.jianwen.mediask.domain.user.port.AdminDoctorQueryRepository;
import org.springframework.transaction.annotation.Transactional;


public class ListAdminDoctorsUseCase {

    private final AdminDoctorQueryRepository adminDoctorQueryRepository;

    public ListAdminDoctorsUseCase(AdminDoctorQueryRepository adminDoctorQueryRepository) {
        this.adminDoctorQueryRepository = adminDoctorQueryRepository;
    }

    @Transactional(readOnly = true)
    public PageData<AdminDoctorListItem> handle(ListAdminDoctorsQuery query) {
        return adminDoctorQueryRepository.pageByKeyword(query.keyword(), query.pageQuery());
    }
}
