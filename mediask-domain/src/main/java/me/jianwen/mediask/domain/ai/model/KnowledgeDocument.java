package me.jianwen.mediask.domain.ai.model;

import java.util.Objects;
import java.util.UUID;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.id.SnowflakeIdGenerator;
import me.jianwen.mediask.domain.ai.exception.AiErrorCode;

public final class KnowledgeDocument {

    private static final String INLINE_SOURCE_URI_PREFIX = "inline://admin-knowledge-document/";

    private final Long id;
    private final Long knowledgeBaseId;
    private final UUID documentUuid;
    private final String title;
    private final KnowledgeSourceType sourceType;
    private final String sourceUri;
    private final String contentHash;
    private final String languageCode;
    private final Integer versionNo;
    private final String ingestedByService;
    private final Integer version;
    private KnowledgeDocumentStatus status;

    private KnowledgeDocument(
            Long id,
            Long knowledgeBaseId,
            UUID documentUuid,
            String title,
            KnowledgeSourceType sourceType,
            String sourceUri,
            String contentHash,
            String languageCode,
            Integer versionNo,
            String ingestedByService,
            KnowledgeDocumentStatus status,
            Integer version) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.knowledgeBaseId = Objects.requireNonNull(knowledgeBaseId, "knowledgeBaseId must not be null");
        this.documentUuid = Objects.requireNonNull(documentUuid, "documentUuid must not be null");
        this.title = requireNonBlank(title, "title");
        this.sourceType = Objects.requireNonNull(sourceType, "sourceType must not be null");
        this.sourceUri = normalize(sourceUri);
        this.contentHash = requireNonBlank(contentHash, "contentHash");
        this.languageCode = requireNonBlank(languageCode, "languageCode");
        this.versionNo = Objects.requireNonNull(versionNo, "versionNo must not be null");
        this.ingestedByService = requireNonBlank(ingestedByService, "ingestedByService");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.version = Objects.requireNonNull(version, "version must not be null");
    }

    public static KnowledgeDocument createUploaded(
            Long knowledgeBaseId, String title, KnowledgeSourceType sourceType, String sourceUri, String contentHash) {
        UUID documentUuid = UUID.randomUUID();
        return new KnowledgeDocument(
                SnowflakeIdGenerator.nextId(),
                knowledgeBaseId,
                documentUuid,
                title,
                sourceType,
                resolveSourceUri(sourceUri, documentUuid),
                contentHash,
                "zh-CN",
                1,
                "JAVA",
                KnowledgeDocumentStatus.UPLOADED,
                0);
    }

    public static KnowledgeDocument rehydrate(
            Long id,
            Long knowledgeBaseId,
            UUID documentUuid,
            String title,
            KnowledgeSourceType sourceType,
            String sourceUri,
            String contentHash,
            String languageCode,
            Integer versionNo,
            String ingestedByService,
            KnowledgeDocumentStatus status,
            Integer version) {
        return new KnowledgeDocument(
                id,
                knowledgeBaseId,
                documentUuid,
                title,
                sourceType,
                sourceUri,
                contentHash,
                languageCode,
                versionNo,
                ingestedByService,
                status,
                version);
    }

    public void markParsing() {
        transitionTo(KnowledgeDocumentStatus.PARSING, KnowledgeDocumentStatus.UPLOADED);
    }

    public void markChunked() {
        transitionTo(KnowledgeDocumentStatus.CHUNKED, KnowledgeDocumentStatus.PARSING);
    }

    public void markIndexing() {
        transitionTo(KnowledgeDocumentStatus.INDEXING, KnowledgeDocumentStatus.CHUNKED);
    }

    public void markActive() {
        transitionTo(KnowledgeDocumentStatus.ACTIVE, KnowledgeDocumentStatus.INDEXING);
    }

    public void markFailed() {
        if (status == KnowledgeDocumentStatus.ACTIVE || status == KnowledgeDocumentStatus.ARCHIVED) {
            throw new BizException(AiErrorCode.KNOWLEDGE_DOCUMENT_STATUS_INVALID);
        }
        status = KnowledgeDocumentStatus.FAILED;
    }

    private void transitionTo(KnowledgeDocumentStatus targetStatus, KnowledgeDocumentStatus expectedCurrentStatus) {
        if (status != expectedCurrentStatus) {
            throw new BizException(AiErrorCode.KNOWLEDGE_DOCUMENT_STATUS_INVALID);
        }
        status = targetStatus;
    }

    public Long id() {
        return id;
    }

    public Long knowledgeBaseId() {
        return knowledgeBaseId;
    }

    public UUID documentUuid() {
        return documentUuid;
    }

    public String title() {
        return title;
    }

    public KnowledgeSourceType sourceType() {
        return sourceType;
    }

    public String sourceUri() {
        return sourceUri;
    }

    public String contentHash() {
        return contentHash;
    }

    public String languageCode() {
        return languageCode;
    }

    public Integer versionNo() {
        return versionNo;
    }

    public String ingestedByService() {
        return ingestedByService;
    }

    public KnowledgeDocumentStatus status() {
        return status;
    }

    public Integer version() {
        return version;
    }

    private static String resolveSourceUri(String sourceUri, UUID documentUuid) {
        String normalizedSourceUri = normalize(sourceUri);
        if (normalizedSourceUri != null) {
            return normalizedSourceUri;
        }
        return INLINE_SOURCE_URI_PREFIX + documentUuid;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
