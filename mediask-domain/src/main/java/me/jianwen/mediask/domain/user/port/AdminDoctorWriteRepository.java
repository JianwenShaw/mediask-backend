package me.jianwen.mediask.domain.user.port;

import me.jianwen.mediask.domain.user.model.AdminDoctorCreateDraft;
import me.jianwen.mediask.domain.user.model.AdminDoctorDetail;
import me.jianwen.mediask.domain.user.model.AdminDoctorUpdateDraft;

public interface AdminDoctorWriteRepository {

    AdminDoctorDetail create(AdminDoctorCreateDraft draft);

    AdminDoctorDetail update(Long doctorId, AdminDoctorUpdateDraft draft);

    void softDelete(Long doctorId);
}
