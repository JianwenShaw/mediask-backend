package me.jianwen.mediask.domain.user.port;

import java.util.List;
import java.util.Optional;
import me.jianwen.mediask.common.pagination.PageData;
import me.jianwen.mediask.common.pagination.PageQuery;
import me.jianwen.mediask.domain.user.model.AdminPatientDetail;
import me.jianwen.mediask.domain.user.model.AdminPatientListItem;

public interface AdminPatientQueryRepository {

    PageData<AdminPatientListItem> pageByKeyword(String keyword, PageQuery pageQuery);

    Optional<AdminPatientDetail> findDetailByPatientId(Long patientId);
}
