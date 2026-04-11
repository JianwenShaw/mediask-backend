package me.jianwen.mediask.infra.persistence.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.pagination.PageData;
import me.jianwen.mediask.common.pagination.PageQuery;
import me.jianwen.mediask.domain.ai.exception.AiErrorCode;
import me.jianwen.mediask.domain.ai.model.KnowledgeBase;
import me.jianwen.mediask.domain.ai.model.KnowledgeBaseOwnerType;
import me.jianwen.mediask.domain.ai.model.KnowledgeBaseStatus;
import me.jianwen.mediask.domain.ai.model.KnowledgeBaseSummary;
import me.jianwen.mediask.domain.ai.model.KnowledgeBaseVisibility;
import me.jianwen.mediask.domain.ai.port.KnowledgeBaseRepository;
import me.jianwen.mediask.infra.persistence.dataobject.KnowledgeChunkDO;
import me.jianwen.mediask.infra.persistence.dataobject.KnowledgeDocumentDO;
import me.jianwen.mediask.infra.persistence.dataobject.KnowledgeBaseDO;
import me.jianwen.mediask.infra.persistence.mapper.KnowledgeChunkMapper;
import me.jianwen.mediask.infra.persistence.mapper.KnowledgeDocumentMapper;
import me.jianwen.mediask.infra.persistence.mapper.KnowledgeBaseMapper;
import me.jianwen.mediask.infra.persistence.row.KnowledgeBaseListRow;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeBaseRepositoryAdapter implements KnowledgeBaseRepository {

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final KnowledgeChunkMapper knowledgeChunkMapper;

    public KnowledgeBaseRepositoryAdapter(
            KnowledgeBaseMapper knowledgeBaseMapper,
            KnowledgeDocumentMapper knowledgeDocumentMapper,
            KnowledgeChunkMapper knowledgeChunkMapper) {
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
        this.knowledgeChunkMapper = knowledgeChunkMapper;
    }

    @Override
    public boolean existsEnabled(Long knowledgeBaseId) {
        return knowledgeBaseMapper.selectCount(Wrappers.lambdaQuery(KnowledgeBaseDO.class)
                        .eq(KnowledgeBaseDO::getId, knowledgeBaseId)
                        .eq(KnowledgeBaseDO::getStatus, "ENABLED")
                        .isNull(KnowledgeBaseDO::getDeletedAt))
                > 0;
    }

    @Override
    public PageData<KnowledgeBaseSummary> pageByKeyword(String keyword, PageQuery pageQuery) {
        Page<KnowledgeBaseListRow> page = new Page<>(pageQuery.pageNum(), pageQuery.pageSize());
        List<KnowledgeBaseSummary> items =
                knowledgeBaseMapper.selectKnowledgeBasePage(page, keyword).getRecords().stream()
                        .map(this::toSummary)
                        .toList();
        return new PageData<>(items, pageQuery.pageNum(), pageQuery.pageSize(), page.getTotal(), page.getPages(), page.hasNext());
    }

    @Override
    public void save(KnowledgeBase knowledgeBase) {
        try {
            knowledgeBaseMapper.insert(toDataObject(knowledgeBase));
        } catch (DuplicateKeyException exception) {
            throw mapDuplicateKey(exception);
        }
    }

    @Override
    public Optional<KnowledgeBase> findById(Long knowledgeBaseId) {
        KnowledgeBaseDO dataObject = knowledgeBaseMapper.selectOne(Wrappers.lambdaQuery(KnowledgeBaseDO.class)
                .eq(KnowledgeBaseDO::getId, knowledgeBaseId)
                .isNull(KnowledgeBaseDO::getDeletedAt));
        return Optional.ofNullable(dataObject).map(this::toDomain);
    }

    @Override
    public Optional<KnowledgeBaseSummary> findSummaryById(Long knowledgeBaseId) {
        KnowledgeBaseDO dataObject = knowledgeBaseMapper.selectOne(Wrappers.lambdaQuery(KnowledgeBaseDO.class)
                .eq(KnowledgeBaseDO::getId, knowledgeBaseId)
                .isNull(KnowledgeBaseDO::getDeletedAt));
        if (dataObject == null) {
            return Optional.empty();
        }
        long docCount = knowledgeDocumentMapper.selectCount(Wrappers.lambdaQuery(KnowledgeDocumentDO.class)
                .eq(KnowledgeDocumentDO::getKnowledgeBaseId, knowledgeBaseId)
                .isNull(KnowledgeDocumentDO::getDeletedAt));
        return Optional.of(new KnowledgeBaseSummary(
                dataObject.getId(),
                dataObject.getKbCode(),
                dataObject.getName(),
                KnowledgeBaseOwnerType.valueOf(dataObject.getOwnerType()),
                dataObject.getOwnerDeptId(),
                KnowledgeBaseVisibility.valueOf(dataObject.getVisibility()),
                KnowledgeBaseStatus.valueOf(dataObject.getStatus()),
                docCount));
    }

    @Override
    public void update(KnowledgeBase knowledgeBase) {
        KnowledgeBaseDO existing = knowledgeBaseMapper.selectOne(Wrappers.lambdaQuery(KnowledgeBaseDO.class)
                .eq(KnowledgeBaseDO::getId, knowledgeBase.id())
                .isNull(KnowledgeBaseDO::getDeletedAt));
        if (existing == null) {
            throw new BizException(AiErrorCode.KNOWLEDGE_BASE_NOT_FOUND);
        }

        KnowledgeBaseDO toUpdate = new KnowledgeBaseDO();
        toUpdate.setId(existing.getId());
        toUpdate.setVersion(existing.getVersion());
        toUpdate.setName(knowledgeBase.name());
        toUpdate.setOwnerType(knowledgeBase.ownerType().name());
        toUpdate.setOwnerDeptId(knowledgeBase.ownerDeptId());
        toUpdate.setVisibility(knowledgeBase.visibility().name());
        toUpdate.setStatus(knowledgeBase.status().name());

        if (knowledgeBaseMapper.updateById(toUpdate) != 1) {
            throw new BizException(AiErrorCode.KNOWLEDGE_BASE_UPDATE_CONFLICT);
        }
    }

    @Override
    public void deleteById(Long knowledgeBaseId) {
        KnowledgeBaseDO existing = knowledgeBaseMapper.selectOne(Wrappers.lambdaQuery(KnowledgeBaseDO.class)
                .eq(KnowledgeBaseDO::getId, knowledgeBaseId)
                .isNull(KnowledgeBaseDO::getDeletedAt));
        if (existing == null) {
            throw new BizException(AiErrorCode.KNOWLEDGE_BASE_NOT_FOUND);
        }

        OffsetDateTime deletedAt = OffsetDateTime.now();
        LambdaUpdateWrapper<KnowledgeBaseDO> deleteKnowledgeBase = Wrappers.<KnowledgeBaseDO>lambdaUpdate()
                .eq(KnowledgeBaseDO::getId, existing.getId())
                .eq(KnowledgeBaseDO::getVersion, existing.getVersion())
                .isNull(KnowledgeBaseDO::getDeletedAt)
                .set(KnowledgeBaseDO::getDeletedAt, deletedAt)
                .set(KnowledgeBaseDO::getUpdatedAt, deletedAt)
                .set(KnowledgeBaseDO::getVersion, existing.getVersion() + 1);
        int updatedKnowledgeBases = knowledgeBaseMapper.update(null, deleteKnowledgeBase);
        if (updatedKnowledgeBases != 1) {
            throw new BizException(AiErrorCode.KNOWLEDGE_BASE_DELETE_CONFLICT);
        }

        LambdaUpdateWrapper<KnowledgeDocumentDO> deleteDocuments = Wrappers.<KnowledgeDocumentDO>lambdaUpdate()
                .eq(KnowledgeDocumentDO::getKnowledgeBaseId, knowledgeBaseId)
                .isNull(KnowledgeDocumentDO::getDeletedAt)
                .set(KnowledgeDocumentDO::getDeletedAt, deletedAt)
                .set(KnowledgeDocumentDO::getUpdatedAt, deletedAt);
        knowledgeDocumentMapper.update(null, deleteDocuments);

        LambdaUpdateWrapper<KnowledgeChunkDO> deleteChunks = Wrappers.<KnowledgeChunkDO>lambdaUpdate()
                .eq(KnowledgeChunkDO::getKnowledgeBaseId, knowledgeBaseId)
                .isNull(KnowledgeChunkDO::getDeletedAt)
                .set(KnowledgeChunkDO::getDeletedAt, deletedAt)
                .set(KnowledgeChunkDO::getUpdatedAt, deletedAt);
        knowledgeChunkMapper.update(null, deleteChunks);
    }

    private KnowledgeBaseDO toDataObject(KnowledgeBase knowledgeBase) {
        KnowledgeBaseDO dataObject = new KnowledgeBaseDO();
        dataObject.setId(knowledgeBase.id());
        dataObject.setKbCode(knowledgeBase.kbCode());
        dataObject.setName(knowledgeBase.name());
        dataObject.setOwnerType(knowledgeBase.ownerType().name());
        dataObject.setOwnerDeptId(knowledgeBase.ownerDeptId());
        dataObject.setVisibility(knowledgeBase.visibility().name());
        dataObject.setStatus(knowledgeBase.status().name());
        dataObject.setVersion(knowledgeBase.version());
        return dataObject;
    }

    private KnowledgeBase toDomain(KnowledgeBaseDO dataObject) {
        return KnowledgeBase.rehydrate(
                dataObject.getId(),
                dataObject.getKbCode(),
                dataObject.getName(),
                KnowledgeBaseOwnerType.valueOf(dataObject.getOwnerType()),
                dataObject.getOwnerDeptId(),
                KnowledgeBaseVisibility.valueOf(dataObject.getVisibility()),
                KnowledgeBaseStatus.valueOf(dataObject.getStatus()),
                dataObject.getVersion());
    }

    private KnowledgeBaseSummary toSummary(KnowledgeBaseListRow row) {
        return new KnowledgeBaseSummary(
                row.getId(),
                row.getKbCode(),
                row.getName(),
                KnowledgeBaseOwnerType.valueOf(row.getOwnerType()),
                row.getOwnerDeptId(),
                KnowledgeBaseVisibility.valueOf(row.getVisibility()),
                KnowledgeBaseStatus.valueOf(row.getStatus()),
                row.getDocCount() == null ? 0L : row.getDocCount());
    }

    private BizException mapDuplicateKey(DuplicateKeyException exception) {
        String message = exception.getMessage();
        if (message != null && message.contains("uk_knowledge_base_code")) {
            return new BizException(AiErrorCode.KNOWLEDGE_BASE_CODE_CONFLICT);
        }
        throw exception;
    }
}
