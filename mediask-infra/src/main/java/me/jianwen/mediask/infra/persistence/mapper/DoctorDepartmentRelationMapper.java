package me.jianwen.mediask.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import me.jianwen.mediask.infra.persistence.dataobject.DoctorDepartmentRelationDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DoctorDepartmentRelationMapper extends BaseMapper<DoctorDepartmentRelationDO> {

    @Select("""
            SELECT department_id
            FROM doctor_department_rel
            WHERE doctor_id = #{doctorId}
              AND is_primary = TRUE
              AND relation_status = 'ACTIVE'
            """)
    Long selectPrimaryDepartmentIdByDoctorId(@Param("doctorId") Long doctorId);

    @Select("""
            SELECT ddr.id AS id, ddr.doctor_id, ddr.department_id, ddr.is_primary,
                   ddr.relation_status, ddr.created_at, ddr.updated_at
            FROM doctor_department_rel ddr
            WHERE ddr.doctor_id = #{doctorId}
              AND ddr.relation_status = 'ACTIVE'
            """)
    List<DoctorDepartmentRelationDO> selectActiveByDoctorId(@Param("doctorId") Long doctorId);
}
