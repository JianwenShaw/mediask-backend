package me.jianwen.mediask.infra.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.jianwen.mediask.infra.persistence.base.BaseDO;

@Getter
@Setter
@TableName("knowledge_base")
@EqualsAndHashCode(callSuper = true)
public class KnowledgeBaseDO extends BaseDO {

    private String kbCode;
    private String name;
    private String ownerType;
    private Long ownerDeptId;
    private String visibility;
    private String status;
    private String embeddingModel;
    private Integer embeddingDim;
    private String chunkStrategyJson;
    private String retrievalStrategyJson;
}
