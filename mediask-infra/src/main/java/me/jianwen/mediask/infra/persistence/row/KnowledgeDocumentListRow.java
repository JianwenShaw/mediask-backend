package me.jianwen.mediask.infra.persistence.row;

import java.util.UUID;

public class KnowledgeDocumentListRow {

    private Long id;
    private UUID documentUuid;
    private String title;
    private String sourceType;
    private String documentStatus;
    private Long chunkCount;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getDocumentUuid() {
        return documentUuid;
    }

    public void setDocumentUuid(UUID documentUuid) {
        this.documentUuid = documentUuid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getDocumentStatus() {
        return documentStatus;
    }

    public void setDocumentStatus(String documentStatus) {
        this.documentStatus = documentStatus;
    }

    public Long getChunkCount() {
        return chunkCount;
    }

    public void setChunkCount(Long chunkCount) {
        this.chunkCount = chunkCount;
    }
}
