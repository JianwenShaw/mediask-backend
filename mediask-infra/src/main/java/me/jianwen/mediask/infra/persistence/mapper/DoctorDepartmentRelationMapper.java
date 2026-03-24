package me.jianwen.mediask.infra.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DoctorDepartmentRelationMapper {

    @Select("""
            SELECT department_id
            FROM doctor_department_rel
            WHERE doctor_id = #{doctorId}
              AND is_primary = TRUE
              AND relation_status = 'ACTIVE'
            """)
    Long selectPrimaryDepartmentIdByDoctorId(@Param("doctorId") Long doctorId);
}
