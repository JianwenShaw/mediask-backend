package me.jianwen.mediask.domain.user.port;

import java.util.Optional;
import me.jianwen.mediask.common.pagination.PageData;
import me.jianwen.mediask.common.pagination.PageQuery;
import me.jianwen.mediask.domain.user.model.AdminDoctorDetail;
import me.jianwen.mediask.domain.user.model.AdminDoctorListItem;

public interface AdminDoctorQueryRepository {

    PageData<AdminDoctorListItem> pageByKeyword(String keyword, PageQuery pageQuery);

    Optional<AdminDoctorDetail> findDetailByDoctorId(Long doctorId);
}
