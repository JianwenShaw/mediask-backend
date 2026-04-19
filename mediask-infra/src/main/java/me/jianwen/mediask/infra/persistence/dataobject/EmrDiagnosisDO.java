package me.jianwen.mediask.infra.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("emr_diagnosis")
public class EmrDiagnosisDO {

    @TableId(type = IdType.INPUT)
    private Long id;
    private Long recordId;
    private String diagnosisType;
    private String diagnosisCode;
    private String diagnosisName;
    private Boolean isPrimary;
    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
}
