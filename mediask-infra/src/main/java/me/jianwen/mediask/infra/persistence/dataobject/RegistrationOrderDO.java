package me.jianwen.mediask.infra.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.jianwen.mediask.infra.persistence.base.BaseDO;

@Getter
@Setter
@TableName("registration_order")
@EqualsAndHashCode(callSuper = true)
public class RegistrationOrderDO extends BaseDO {

    private String orderNo;
    private Long patientId;
    private Long doctorId;
    private Long departmentId;
    private Long sessionId;
    private Long slotId;
    private String orderStatus;
    private BigDecimal fee;
    private OffsetDateTime paidAt;
    private OffsetDateTime cancelledAt;
    private String cancellationReason;
}
