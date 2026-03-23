package me.jianwen.mediask.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.jianwen.mediask.infra.persistence.dataobject.DoctorDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DoctorMapper extends BaseMapper<DoctorDO> {

    @Select("""
            SELECT *
            FROM doctors
            WHERE user_id = #{userId}
              AND status = 'ACTIVE'
              AND deleted_at IS NULL
            LIMIT 1
            """)
    DoctorDO selectActiveByUserId(@Param("userId") Long userId);
}
