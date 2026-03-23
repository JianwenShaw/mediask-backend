package me.jianwen.mediask.infra.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("doctor_department_rel")
public class DoctorDepartmentRelationDO {

    private Long id;
    private Long doctorId;
    private Long departmentId;

    @TableField("is_primary")
    private Boolean primary;

    private String relationStatus;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
