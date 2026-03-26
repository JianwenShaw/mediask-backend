package me.jianwen.mediask.infra.persistence.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DataScopeRuleMapper {

    @Select({
        "<script>",
        "SELECT DISTINCT resource_type, scope_type, scope_dept_id",
        "FROM data_scope_rules",
        "WHERE role_id IN",
        "<foreach collection='roleIds' item='roleId' open='(' separator=',' close=')'>",
        "#{roleId}",
        "</foreach>",
        "AND status = 'ACTIVE'",
        "ORDER BY resource_type, scope_type",
        "</script>"
    })
    List<ActiveDataScopeRuleRow> selectActiveRulesByRoleIds(@Param("roleIds") List<Long> roleIds);

    @Select({
        "SELECT COUNT(1)",
        "FROM data_scope_rules",
        "WHERE scope_type = 'CUSTOM'",
        "AND status = 'ACTIVE'"
    })
    long countActiveCustomRules();
}
