package me.jianwen.mediask.domain.user.port;

import java.util.Optional;
import me.jianwen.mediask.common.pagination.PageData;
import me.jianwen.mediask.common.pagination.PageQuery;
import me.jianwen.mediask.domain.user.model.AdminDepartmentDetail;
import me.jianwen.mediask.domain.user.model.AdminDepartmentListItem;

public interface AdminDepartmentQueryRepository {

    PageData<AdminDepartmentListItem> pageByKeyword(String keyword, PageQuery pageQuery);

    Optional<AdminDepartmentDetail> findDetailById(Long id);
}
