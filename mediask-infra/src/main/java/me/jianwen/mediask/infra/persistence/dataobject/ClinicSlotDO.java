package me.jianwen.mediask.infra.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.jianwen.mediask.infra.persistence.base.BaseDO;

@Getter
@Setter
@TableName("clinic_slot")
@EqualsAndHashCode(callSuper = true)
public class ClinicSlotDO extends BaseDO {

    private Long sessionId;
    private Integer slotSeq;
    private OffsetDateTime slotStartTime;
    private OffsetDateTime slotEndTime;
    private String slotStatus;
    private Integer capacity;
    private Integer remainingCount;
}
