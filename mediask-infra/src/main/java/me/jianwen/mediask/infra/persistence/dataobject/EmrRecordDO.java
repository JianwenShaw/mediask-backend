package me.jianwen.mediask.infra.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.jianwen.mediask.infra.persistence.base.BaseDO;

@Getter
@Setter
@TableName("emr_record")
@EqualsAndHashCode(callSuper = true)
public class EmrRecordDO extends BaseDO {

    private String recordNo;
    private Long encounterId;
    private Long patientId;
    private Long doctorId;
    private Long departmentId;
    private String recordStatus;
    private String chiefComplaintSummary;
    private String contentEncrypted;
    private String contentMasked;
    private String contentHash;
}
