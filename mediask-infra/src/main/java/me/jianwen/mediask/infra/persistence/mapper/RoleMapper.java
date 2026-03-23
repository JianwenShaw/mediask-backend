package me.jianwen.mediask.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import me.jianwen.mediask.infra.persistence.dataobject.RoleDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RoleMapper extends BaseMapper<RoleDO> {

    @Select("""
            SELECT r.role_code
            FROM roles r
            JOIN user_roles ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId}
              AND ur.active_flag = TRUE
              AND (ur.expires_at IS NULL OR ur.expires_at > CURRENT_TIMESTAMP)
              AND r.status = 'ACTIVE'
              AND r.deleted_at IS NULL
            ORDER BY r.sort_order ASC, r.id ASC
            """)
    List<String> selectActiveRoleCodesByUserId(@Param("userId") Long userId);
}
