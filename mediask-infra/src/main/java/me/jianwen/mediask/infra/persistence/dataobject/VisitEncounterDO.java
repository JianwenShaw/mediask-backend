package me.jianwen.mediask.infra.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.jianwen.mediask.infra.persistence.base.BaseDO;

@Getter
@Setter
@TableName("visit_encounter")
@EqualsAndHashCode(callSuper = true)
public class VisitEncounterDO extends BaseDO {

    private Long orderId;
    private Long patientId;
    private Long doctorId;
    private Long departmentId;
    private String encounterStatus;
    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;
    private String summary;
}
