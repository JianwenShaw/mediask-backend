package me.jianwen.mediask.infra.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.jianwen.mediask.infra.persistence.base.BaseDO;

@Getter
@Setter
@TableName("doctors")
@EqualsAndHashCode(callSuper = true)
public class DoctorDO extends BaseDO {

    private Long userId;
    private Long hospitalId;
    private String doctorCode;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String professionalTitle;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String introductionMasked;
    private String status;
}
