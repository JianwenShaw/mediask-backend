package me.jianwen.mediask.infra.persistence.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.pagination.PageData;
import me.jianwen.mediask.common.pagination.PageQuery;
import me.jianwen.mediask.domain.ai.exception.AiErrorCode;
import me.jianwen.mediask.domain.ai.model.KnowledgeDocument;
import me.jianwen.mediask.domain.ai.model.KnowledgeDocumentStatus;
import me.jianwen.mediask.domain.ai.model.KnowledgeDocumentSummary;
import me.jianwen.mediask.domain.ai.model.KnowledgeSourceType;
import me.jianwen.mediask.domain.ai.port.KnowledgeDocumentRepository;
import me.jianwen.mediask.infra.persistence.dataobject.KnowledgeChunkDO;
import me.jianwen.mediask.infra.persistence.dataobject.KnowledgeDocumentDO;
import me.jianwen.mediask.infra.persistence.mapper.KnowledgeChunkMapper;
import me.jianwen.mediask.infra.persistence.mapper.KnowledgeDocumentMapper;
import me.jianwen.mediask.infra.persistence.row.KnowledgeDocumentListRow;
import org.springframework.stereotype.Component;
import org.springframework.dao.DuplicateKeyException;

@Component
public class KnowledgeDocumentRepositoryAdapter implements KnowledgeDocumentRepository {

    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final KnowledgeChunkMapper knowledgeChunkMapper;

    public KnowledgeDocumentRepositoryAdapter(
            KnowledgeDocumentMapper knowledgeDocumentMapper, KnowledgeChunkMapper knowledgeChunkMapper) {
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
        this.knowledgeChunkMapper = knowledgeChunkMapper;
    }

    @Override
    public void save(KnowledgeDocument knowledgeDocument) {
        KnowledgeDocumentDO dataObject = toDataObject(knowledgeDocument);
        try {
            knowledgeDocumentMapper.insertKnowledgeDocument(dataObject);
        } catch (DuplicateKeyException exception) {
            throw mapDuplicateKeyOnSave(exception);
        }
    }

    @Override
    public Optional<KnowledgeDocument> findById(Long documentId) {
        KnowledgeDocumentDO dataObject = knowledgeDocumentMapper.selectOne(Wrappers.lambdaQuery(KnowledgeDocumentDO.class)
                .eq(KnowledgeDocumentDO::getId, documentId)
                .isNull(KnowledgeDocumentDO::getDeletedAt));
        return Optional.ofNullable(dataObject).map(this::toDomain);
    }

    @Override
    public boolean existsEffectiveByKnowledgeBaseIdAndContentHash(Long knowledgeBaseId, String contentHash) {
        return knowledgeDocumentMapper.selectCount(Wrappers.lambdaQuery(KnowledgeDocumentDO.class)
                        .eq(KnowledgeDocumentDO::getKnowledgeBaseId, knowledgeBaseId)
                        .eq(KnowledgeDocumentDO::getContentHash, contentHash)
                        .notIn(
                                KnowledgeDocumentDO::getDocumentStatus,
                                KnowledgeDocumentStatus.FAILED.name(),
                                KnowledgeDocumentStatus.ARCHIVED.name())
                        .isNull(KnowledgeDocumentDO::getDeletedAt))
                > 0;
    }

    @Override
    public void update(KnowledgeDocument knowledgeDocument) {
        KnowledgeDocumentDO existing = knowledgeDocumentMapper.selectOne(Wrappers.lambdaQuery(KnowledgeDocumentDO.class)
                .eq(KnowledgeDocumentDO::getId, knowledgeDocument.id())
                .isNull(KnowledgeDocumentDO::getDeletedAt));
        if (existing == null) {
            throw new BizException(AiErrorCode.KNOWLEDGE_DOCUMENT_NOT_FOUND);
        }

        KnowledgeDocumentDO toUpdate = new KnowledgeDocumentDO();
        toUpdate.setId(existing.getId());
        toUpdate.setVersion(existing.getVersion());
        toUpdate.setDocumentStatus(knowledgeDocument.status().name());

        if (knowledgeDocumentMapper.updateById(toUpdate) != 1) {
            throw new BizException(AiErrorCode.KNOWLEDGE_DOCUMENT_UPDATE_CONFLICT);
        }
    }

    @Override
    public PageData<KnowledgeDocumentSummary> pageByKnowledgeBaseId(Long knowledgeBaseId, PageQuery pageQuery) {
        Page<KnowledgeDocumentListRow> page = new Page<>(pageQuery.pageNum(), pageQuery.pageSize());
        return new PageData<>(
                knowledgeDocumentMapper.selectKnowledgeDocumentPage(page, knowledgeBaseId).getRecords().stream()
                        .map(this::toSummary)
                        .toList(),
                pageQuery.pageNum(),
                pageQuery.pageSize(),
                page.getTotal(),
                page.getPages(),
                page.hasNext());
    }

    @Override
    public void deleteById(Long documentId) {
        KnowledgeDocumentDO existing = knowledgeDocumentMapper.selectOne(Wrappers.lambdaQuery(KnowledgeDocumentDO.class)
                .eq(KnowledgeDocumentDO::getId, documentId)
                .isNull(KnowledgeDocumentDO::getDeletedAt));
        if (existing == null) {
            throw new BizException(AiErrorCode.KNOWLEDGE_DOCUMENT_NOT_FOUND);
        }

        OffsetDateTime deletedAt = OffsetDateTime.now();
        LambdaUpdateWrapper<KnowledgeDocumentDO> deleteDocument = Wrappers.lambdaUpdate(KnowledgeDocumentDO.class)
                .eq(KnowledgeDocumentDO::getId, existing.getId())
                .eq(KnowledgeDocumentDO::getVersion, existing.getVersion())
                .isNull(KnowledgeDocumentDO::getDeletedAt)
                .set(KnowledgeDocumentDO::getDeletedAt, deletedAt)
                .set(KnowledgeDocumentDO::getUpdatedAt, deletedAt)
                .set(KnowledgeDocumentDO::getVersion, existing.getVersion() + 1);
        int updatedDocuments = knowledgeDocumentMapper.update(null, deleteDocument);
        if (updatedDocuments != 1) {
            throw new BizException(AiErrorCode.KNOWLEDGE_DOCUMENT_DELETE_CONFLICT);
        }

        LambdaUpdateWrapper<KnowledgeChunkDO> deleteChunks = Wrappers.lambdaUpdate(KnowledgeChunkDO.class)
                .eq(KnowledgeChunkDO::getDocumentId, documentId)
                .isNull(KnowledgeChunkDO::getDeletedAt)
                .set(KnowledgeChunkDO::getDeletedAt, deletedAt)
                .set(KnowledgeChunkDO::getUpdatedAt, deletedAt);
        knowledgeChunkMapper.update(null, deleteChunks);
    }

    private KnowledgeDocumentDO toDataObject(KnowledgeDocument knowledgeDocument) {
        KnowledgeDocumentDO dataObject = new KnowledgeDocumentDO();
        dataObject.setId(knowledgeDocument.id());
        dataObject.setKnowledgeBaseId(knowledgeDocument.knowledgeBaseId());
        dataObject.setDocumentUuid(knowledgeDocument.documentUuid().toString());
        dataObject.setTitle(knowledgeDocument.title());
        dataObject.setSourceType(knowledgeDocument.sourceType().name());
        dataObject.setSourceUri(knowledgeDocument.sourceUri());
        dataObject.setContentHash(knowledgeDocument.contentHash());
        dataObject.setLanguageCode(knowledgeDocument.languageCode());
        dataObject.setVersionNo(knowledgeDocument.versionNo());
        dataObject.setDocumentStatus(knowledgeDocument.status().name());
        dataObject.setIngestedByService(knowledgeDocument.ingestedByService());
        dataObject.setVersion(knowledgeDocument.version());
        return dataObject;
    }

    private KnowledgeDocument toDomain(KnowledgeDocumentDO dataObject) {
        return KnowledgeDocument.rehydrate(
                dataObject.getId(),
                dataObject.getKnowledgeBaseId(),
                UUID.fromString(dataObject.getDocumentUuid()),
                dataObject.getTitle(),
                KnowledgeSourceType.valueOf(dataObject.getSourceType()),
                dataObject.getSourceUri(),
                dataObject.getContentHash(),
                dataObject.getLanguageCode(),
                dataObject.getVersionNo(),
                dataObject.getIngestedByService(),
                KnowledgeDocumentStatus.valueOf(dataObject.getDocumentStatus()),
                dataObject.getVersion());
    }

    private BizException mapDuplicateKeyOnSave(DuplicateKeyException exception) {
        String message = exception.getMessage();
        if (message != null && message.contains("uk_knowledge_document_base_hash_active")) {
            return new BizException(AiErrorCode.KNOWLEDGE_DOCUMENT_DUPLICATE);
        }
        throw exception;
    }

    private KnowledgeDocumentSummary toSummary(KnowledgeDocumentListRow row) {
        return new KnowledgeDocumentSummary(
                row.getId(),
                row.getDocumentUuid(),
                row.getTitle(),
                KnowledgeSourceType.valueOf(row.getSourceType()),
                KnowledgeDocumentStatus.valueOf(row.getDocumentStatus()),
                row.getChunkCount() == null ? 0L : row.getChunkCount());
    }
}
