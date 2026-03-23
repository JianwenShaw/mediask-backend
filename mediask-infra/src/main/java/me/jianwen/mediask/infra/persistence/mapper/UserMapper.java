package me.jianwen.mediask.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.jianwen.mediask.infra.persistence.dataobject.UserDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper extends BaseMapper<UserDO> {

    @Select("""
            SELECT *
            FROM users
            WHERE username = #{username}
              AND deleted_at IS NULL
            LIMIT 1
            """)
    UserDO selectActiveByUsername(@Param("username") String username);

    @Select("""
            SELECT *
            FROM users
            WHERE id = #{userId}
              AND account_status = 'ACTIVE'
              AND deleted_at IS NULL
            LIMIT 1
            """)
    UserDO selectActiveById(@Param("userId") Long userId);

    @Update("""
            UPDATE users
            SET last_login_at = CURRENT_TIMESTAMP,
                updated_at = CURRENT_TIMESTAMP,
                version = version + 1
            WHERE id = #{userId}
              AND deleted_at IS NULL
            """)
    int updateLastLoginAt(@Param("userId") Long userId);
}
