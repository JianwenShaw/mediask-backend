package me.jianwen.mediask.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import me.jianwen.mediask.infra.persistence.dataobject.DoctorDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DoctorMapper extends BaseMapper<DoctorDO> {

    @Select("""
            <script>
            SELECT
                d.id AS doctor_id,
                u.id AS user_id,
                u.version AS user_version,
                d.version AS doctor_version,
                d.hospital_id,
                d.doctor_code,
                u.username,
                u.display_name,
                u.phone,
                d.professional_title,
                d.introduction_masked,
                u.mobile_masked,
                u.account_status
            FROM doctors d
            JOIN users u ON u.id = d.user_id
            WHERE d.deleted_at IS NULL
              AND u.deleted_at IS NULL
              <if test="keyword != null">
                AND (
                    u.username ILIKE CONCAT('%', #{keyword}, '%')
                    OR u.display_name ILIKE CONCAT('%', #{keyword}, '%')
                    OR d.doctor_code ILIKE CONCAT('%', #{keyword}, '%')
                )
              </if>
            ORDER BY d.created_at DESC, d.id DESC
            </script>
            """)
    IPage<AdminDoctorRow> selectAdminDoctorsByKeywordPage(
            IPage<AdminDoctorRow> page, @Param("keyword") String keyword);

    @Select("""
            SELECT
                d.id AS doctor_id,
                u.id AS user_id,
                u.version AS user_version,
                d.version AS doctor_version,
                d.hospital_id,
                d.doctor_code,
                u.username,
                u.display_name,
                u.phone,
                d.professional_title,
                d.introduction_masked,
                u.mobile_masked,
                u.account_status
            FROM doctors d
            JOIN users u ON u.id = d.user_id
            WHERE d.id = #{doctorId}
              AND d.deleted_at IS NULL
              AND u.deleted_at IS NULL
            """)
    AdminDoctorRow selectAdminDoctorByDoctorId(@Param("doctorId") Long doctorId);
}
