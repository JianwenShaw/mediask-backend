package me.jianwen.mediask.domain.user.port;

import me.jianwen.mediask.domain.user.model.AdminDepartmentCreateDraft;
import me.jianwen.mediask.domain.user.model.AdminDepartmentDetail;
import me.jianwen.mediask.domain.user.model.AdminDepartmentUpdateDraft;

public interface AdminDepartmentWriteRepository {

    AdminDepartmentDetail create(AdminDepartmentCreateDraft draft);

    AdminDepartmentDetail update(Long id, AdminDepartmentUpdateDraft draft);

    void softDelete(Long id);
}
