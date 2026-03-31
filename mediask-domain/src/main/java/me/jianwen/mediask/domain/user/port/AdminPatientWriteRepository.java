package me.jianwen.mediask.domain.user.port;

import me.jianwen.mediask.domain.user.model.AdminPatientCreateDraft;
import me.jianwen.mediask.domain.user.model.AdminPatientDetail;
import me.jianwen.mediask.domain.user.model.AdminPatientUpdateDraft;

public interface AdminPatientWriteRepository {

    AdminPatientDetail create(AdminPatientCreateDraft draft);

    AdminPatientDetail update(Long patientId, AdminPatientUpdateDraft draft);

    void softDelete(Long patientId);
}
