package me.jianwen.mediask.infra.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.jianwen.mediask.infra.persistence.base.BaseDO;

@Getter
@Setter
@TableName("knowledge_document")
@EqualsAndHashCode(callSuper = true)
public class KnowledgeDocumentDO extends BaseDO {

    private Long knowledgeBaseId;
    private String documentUuid;
    private String title;
    private String sourceType;
    private String sourceUri;
    private String contentHash;
    private String languageCode;
    private Integer versionNo;
    private String documentStatus;
    private String ingestedByService;
    private OffsetDateTime publishedAt;
    private String docMetadata;
}
