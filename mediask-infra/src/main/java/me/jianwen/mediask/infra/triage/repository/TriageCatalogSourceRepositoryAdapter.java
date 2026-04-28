package me.jianwen.mediask.infra.triage.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.List;
import me.jianwen.mediask.domain.triage.model.DepartmentCandidate;
import me.jianwen.mediask.domain.triage.port.DepartmentCatalogSourcePort;
import me.jianwen.mediask.infra.persistence.dataobject.DepartmentDO;
import me.jianwen.mediask.infra.persistence.mapper.DepartmentMapper;
import org.springframework.stereotype.Component;

@Component
public class TriageCatalogSourceRepositoryAdapter implements DepartmentCatalogSourcePort {

    private final DepartmentMapper departmentMapper;

    public TriageCatalogSourceRepositoryAdapter(DepartmentMapper departmentMapper) {
        this.departmentMapper = departmentMapper;
    }

    @Override
    public List<DepartmentCandidate> loadCandidates(String hospitalScope) {
        return departmentMapper.selectList(
                        Wrappers.lambdaQuery(DepartmentDO.class)
                                .eq(DepartmentDO::getStatus, "ACTIVE")
                                .eq(DepartmentDO::getDeptType, "CLINICAL")
                                .isNull(DepartmentDO::getDeletedAt)
                                .orderByAsc(DepartmentDO::getSortOrder)
                                .orderByAsc(DepartmentDO::getId))
                .stream()
                .map(this::toCandidate)
                .toList();
    }

    private DepartmentCandidate toCandidate(DepartmentDO department) {
        int sortOrder = department.getSortOrder() == null ? 0 : department.getSortOrder();
        return new DepartmentCandidate(
                department.getId(),
                department.getName(),
                department.getName() + "相关问题优先考虑",
                List.of(),
                sortOrder);
    }
}
