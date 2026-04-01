package me.jianwen.mediask.application.user.usecase;

import me.jianwen.mediask.application.user.query.ListAdminPatientsQuery;
import me.jianwen.mediask.common.pagination.PageData;
import me.jianwen.mediask.domain.user.model.AdminPatientListItem;
import me.jianwen.mediask.domain.user.port.AdminPatientQueryRepository;
import org.springframework.transaction.annotation.Transactional;

public class ListAdminPatientsUseCase {

    private final AdminPatientQueryRepository adminPatientQueryRepository;

    public ListAdminPatientsUseCase(AdminPatientQueryRepository adminPatientQueryRepository) {
        this.adminPatientQueryRepository = adminPatientQueryRepository;
    }

    @Transactional(readOnly = true)
    public PageData<AdminPatientListItem> handle(ListAdminPatientsQuery query) {
        return adminPatientQueryRepository.pageByKeyword(query.keyword(), query.pageQuery());
    }
}
