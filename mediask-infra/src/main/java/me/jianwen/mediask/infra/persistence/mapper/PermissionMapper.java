package me.jianwen.mediask.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import me.jianwen.mediask.infra.persistence.dataobject.PermissionDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PermissionMapper extends BaseMapper<PermissionDO> {

    @Select({
        "<script>",
        "SELECT DISTINCT p.permission_code",
        "FROM permissions p",
        "JOIN role_permissions rp ON rp.permission_id = p.id",
        "WHERE rp.role_id IN",
        "<foreach collection='roleIds' item='roleId' open='(' separator=',' close=')'>",
        "#{roleId}",
        "</foreach>",
        "AND p.status = 'ACTIVE'",
        "AND p.deleted_at IS NULL",
        "ORDER BY p.permission_code",
        "</script>"
    })
    List<String> selectActivePermissionCodesByRoleIds(@Param("roleIds") List<Long> roleIds);
}
