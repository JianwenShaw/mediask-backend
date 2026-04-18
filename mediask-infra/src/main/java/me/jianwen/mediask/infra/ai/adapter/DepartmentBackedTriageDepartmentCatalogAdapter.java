package me.jianwen.mediask.infra.ai.adapter;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.exception.ErrorCode;
import me.jianwen.mediask.domain.ai.model.TriageDepartmentCandidate;
import me.jianwen.mediask.domain.ai.model.TriageDepartmentCatalog;
import me.jianwen.mediask.domain.ai.port.TriageDepartmentCatalogPort;
import me.jianwen.mediask.infra.persistence.dataobject.DepartmentDO;
import me.jianwen.mediask.infra.persistence.mapper.DepartmentMapper;
import org.springframework.stereotype.Component;

@Component
public class DepartmentBackedTriageDepartmentCatalogAdapter implements TriageDepartmentCatalogPort {

    private static final String DEFAULT_HOSPITAL_SCOPE = "default-hospital";

    private final DepartmentMapper departmentMapper;

    public DepartmentBackedTriageDepartmentCatalogAdapter(DepartmentMapper departmentMapper) {
        this.departmentMapper = departmentMapper;
    }

    @Override
    public TriageDepartmentCatalog getCatalog(String hospitalScope) {
        if (!DEFAULT_HOSPITAL_SCOPE.equals(hospitalScope)) {
            throw new BizException(ErrorCode.INVALID_PARAMETER);
        }

        List<DepartmentDO> departments = departmentMapper.selectList(Wrappers.lambdaQuery(DepartmentDO.class)
                .eq(DepartmentDO::getStatus, "ACTIVE")
                .eq(DepartmentDO::getDeptType, "CLINICAL")
                .isNull(DepartmentDO::getDeletedAt)
                .orderByAsc(DepartmentDO::getSortOrder, DepartmentDO::getId));

        String versionSeed = departments.stream()
                .map(department -> department.getId() + ":" + normalize(department.getUpdatedAt()))
                .collect(Collectors.joining("|"));
        String version = "deptcat-" + Integer.toUnsignedString(versionSeed.hashCode());
        return new TriageDepartmentCatalog(
                DEFAULT_HOSPITAL_SCOPE,
                version,
                departments.stream()
                        .map(this::toCandidate)
                        .toList());
    }

    private TriageDepartmentCandidate toCandidate(DepartmentDO department) {
        return new TriageDepartmentCandidate(
                department.getId(),
                department.getName(),
                department.getName(),
                List.of(department.getName()),
                department.getSortOrder());
    }

    private String normalize(OffsetDateTime updatedAt) {
        return updatedAt == null ? "null" : updatedAt.toInstant().toString().toLowerCase(Locale.ROOT);
    }
}
