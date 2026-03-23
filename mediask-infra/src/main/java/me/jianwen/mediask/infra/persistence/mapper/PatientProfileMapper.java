package me.jianwen.mediask.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.jianwen.mediask.infra.persistence.dataobject.PatientProfileDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PatientProfileMapper extends BaseMapper<PatientProfileDO> {

    @Select("""
            SELECT *
            FROM patient_profile
            WHERE user_id = #{userId}
              AND deleted_at IS NULL
            LIMIT 1
            """)
    PatientProfileDO selectActiveByUserId(@Param("userId") Long userId);
}
