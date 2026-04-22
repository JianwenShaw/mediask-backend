package me.jianwen.mediask.infra.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("prescription_item")
public class PrescriptionItemDO {

    @TableId(type = IdType.INPUT)
    private Long id;
    private Long prescriptionId;
    private Integer sortOrder;
    private String drugName;
    private String drugSpecification;
    private String dosageText;
    private String frequencyText;
    private String durationText;
    private BigDecimal quantity;
    private String unit;
    private String route;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
}
