package me.jianwen.mediask.infra.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.jianwen.mediask.infra.persistence.base.BaseDO;

@Getter
@Setter
@TableName("prescription_order")
@EqualsAndHashCode(callSuper = true)
public class PrescriptionOrderDO extends BaseDO {

    private String prescriptionNo;
    private Long recordId;
    private Long encounterId;
    private Long patientId;
    private Long doctorId;
    private String prescriptionStatus;
}
