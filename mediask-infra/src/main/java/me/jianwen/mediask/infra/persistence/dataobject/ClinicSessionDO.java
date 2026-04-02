package me.jianwen.mediask.infra.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.jianwen.mediask.infra.persistence.base.BaseDO;

@Getter
@Setter
@TableName("clinic_session")
@EqualsAndHashCode(callSuper = true)
public class ClinicSessionDO extends BaseDO {

    private Long hospitalId;
    private Long departmentId;
    private Long doctorId;
    private LocalDate sessionDate;
    private String periodCode;
    private String clinicType;
    private String sessionStatus;
    private Integer capacity;
    private Integer remainingCount;
    private BigDecimal fee;
    private String sourceType;
}
