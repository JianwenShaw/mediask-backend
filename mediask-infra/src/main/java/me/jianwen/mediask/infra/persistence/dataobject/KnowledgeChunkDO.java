package me.jianwen.mediask.infra.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.jianwen.mediask.infra.persistence.base.BaseDO;

@Getter
@Setter
@TableName("knowledge_chunk")
@EqualsAndHashCode(callSuper = true)
public class KnowledgeChunkDO extends BaseDO {

    private Long knowledgeBaseId;
    private Long documentId;
    private Integer chunkIndex;
    private String sectionTitle;
    private Integer pageNo;
    private Integer charStart;
    private Integer charEnd;
    private Integer tokenCount;
    private String content;
    private String contentPreview;
    private String citationLabel;
    private String chunkMetadata;
    private String chunkStatus;
}
