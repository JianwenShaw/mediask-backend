package me.jianwen.mediask.infra.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.Optional;
import me.jianwen.mediask.common.pagination.PageData;
import me.jianwen.mediask.common.pagination.PageQuery;
import me.jianwen.mediask.domain.user.model.AdminDepartmentDetail;
import me.jianwen.mediask.domain.user.model.AdminDepartmentListItem;
import me.jianwen.mediask.domain.user.port.AdminDepartmentQueryRepository;
import me.jianwen.mediask.infra.persistence.dataobject.DepartmentDO;
import me.jianwen.mediask.infra.persistence.mapper.DepartmentMapper;
import org.springframework.stereotype.Component;

@Component
public class AdminDepartmentQueryRepositoryAdapter implements AdminDepartmentQueryRepository {

    private final DepartmentMapper departmentMapper;

    public AdminDepartmentQueryRepositoryAdapter(DepartmentMapper departmentMapper) {
        this.departmentMapper = departmentMapper;
    }

    @Override
    public PageData<AdminDepartmentListItem> pageByKeyword(String keyword, PageQuery pageQuery) {
        Page<DepartmentDO> page = new Page<>(pageQuery.pageNum(), pageQuery.pageSize(), true);
        LambdaQueryWrapper<DepartmentDO> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(DepartmentDO::getName, keyword)
                    .or()
                    .like(DepartmentDO::getDeptCode, keyword));
        }
        wrapper.orderByAsc(DepartmentDO::getSortOrder).orderByAsc(DepartmentDO::getId);
        var result = departmentMapper.selectPage(page, wrapper);
        return new PageData<>(
                result.getRecords().stream().map(this::toListItem).toList(),
                result.getCurrent(),
                result.getSize(),
                result.getTotal(),
                result.getPages(),
                result.getCurrent() < result.getPages());
    }

    @Override
    public Optional<AdminDepartmentDetail> findDetailById(Long id) {
        DepartmentDO dept = departmentMapper.selectById(id);
        if (dept == null) {
            return Optional.empty();
        }
        return Optional.of(toDetail(dept));
    }

    private AdminDepartmentListItem toListItem(DepartmentDO dept) {
        return new AdminDepartmentListItem(
                dept.getId(),
                dept.getHospitalId(),
                dept.getDeptCode(),
                dept.getName(),
                dept.getDeptType(),
                dept.getSortOrder(),
                dept.getStatus());
    }

    private AdminDepartmentDetail toDetail(DepartmentDO dept) {
        return new AdminDepartmentDetail(
                dept.getId(),
                dept.getHospitalId(),
                dept.getDeptCode(),
                dept.getName(),
                dept.getDeptType(),
                dept.getSortOrder(),
                dept.getStatus());
    }
}
