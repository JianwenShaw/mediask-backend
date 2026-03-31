package me.jianwen.mediask.domain.user.port;

import java.util.List;
import java.util.Optional;
import me.jianwen.mediask.domain.user.model.AdminPatientDetail;
import me.jianwen.mediask.domain.user.model.AdminPatientListItem;

public interface AdminPatientQueryRepository {

    List<AdminPatientListItem> listByKeyword(String keyword);

    Optional<AdminPatientDetail> findDetailByPatientId(Long patientId);
}
