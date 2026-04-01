package me.jianwen.mediask.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import me.jianwen.mediask.infra.persistence.dataobject.PatientProfileDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PatientProfileMapper extends BaseMapper<PatientProfileDO> {

    @Select("""
            <script>
            SELECT
                p.id AS patient_id,
                u.id AS user_id,
                u.version AS user_version,
                p.version AS patient_profile_version,
                p.patient_no,
                u.username,
                u.display_name,
                u.mobile_masked,
                p.gender,
                p.birth_date,
                p.blood_type,
                p.allergy_summary,
                u.account_status
            FROM patient_profile p
            JOIN users u ON u.id = p.user_id
            WHERE p.deleted_at IS NULL
              AND u.deleted_at IS NULL
              <if test="keyword != null">
                AND (
                    u.username ILIKE CONCAT('%', #{keyword}, '%')
                    OR u.display_name ILIKE CONCAT('%', #{keyword}, '%')
                    OR p.patient_no ILIKE CONCAT('%', #{keyword}, '%')
                )
              </if>
            ORDER BY p.created_at DESC, p.id DESC
            </script>
            """)
    IPage<AdminPatientRow> selectAdminPatientsByKeywordPage(IPage<AdminPatientRow> page, @Param("keyword") String keyword);


    @Select("""
            SELECT
                p.id AS patient_id,
                u.id AS user_id,
                u.version AS user_version,
                p.version AS patient_profile_version,
                p.created_at,
                p.patient_no,
                u.username,
                u.display_name,
                u.mobile_masked,
                p.gender,
                p.birth_date,
                p.blood_type,
                p.allergy_summary,
                u.account_status
            FROM patient_profile p
            JOIN users u ON u.id = p.user_id
            WHERE p.id = #{patientId}
              AND p.deleted_at IS NULL
              AND u.deleted_at IS NULL
            """)
    AdminPatientRow selectAdminPatientByPatientId(@Param("patientId") Long patientId);
}
