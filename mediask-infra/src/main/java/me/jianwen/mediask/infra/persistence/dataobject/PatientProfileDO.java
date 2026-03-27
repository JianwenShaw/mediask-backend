package me.jianwen.mediask.infra.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.jianwen.mediask.infra.persistence.base.BaseDO;

@Getter
@Setter
@TableName("patient_profile")
@EqualsAndHashCode(callSuper = true)
public class PatientProfileDO extends BaseDO {

    private Long userId;
    private String patientNo;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String gender;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate birthDate;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String bloodType;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String allergySummary;
}
