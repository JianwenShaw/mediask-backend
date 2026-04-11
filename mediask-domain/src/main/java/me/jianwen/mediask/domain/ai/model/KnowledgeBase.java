package me.jianwen.mediask.domain.ai.model;

import java.util.Objects;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.id.SnowflakeIdGenerator;
import me.jianwen.mediask.common.util.ArgumentChecks;
import me.jianwen.mediask.domain.ai.exception.AiErrorCode;

public final class KnowledgeBase {

    private final Long id;
    private final String kbCode;
    private final Integer version;
    private String name;
    private KnowledgeBaseOwnerType ownerType;
    private Long ownerDeptId;
    private KnowledgeBaseVisibility visibility;
    private KnowledgeBaseStatus status;

    private KnowledgeBase(
            Long id,
            String kbCode,
            String name,
            KnowledgeBaseOwnerType ownerType,
            Long ownerDeptId,
            KnowledgeBaseVisibility visibility,
            KnowledgeBaseStatus status,
            Integer version) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.kbCode = ArgumentChecks.requireNonBlank(kbCode, "kbCode");
        this.version = Objects.requireNonNull(version, "version must not be null");
        this.name = ArgumentChecks.requireNonBlank(name, "name");
        this.ownerType = Objects.requireNonNull(ownerType, "ownerType must not be null");
        this.ownerDeptId = normalizeOwnerDeptId(ownerType, ownerDeptId);
        this.visibility = Objects.requireNonNull(visibility, "visibility must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    public static KnowledgeBase create(
            String kbCode, String name, KnowledgeBaseOwnerType ownerType, Long ownerDeptId, KnowledgeBaseVisibility visibility) {
        return new KnowledgeBase(
                SnowflakeIdGenerator.nextId(),
                kbCode,
                name,
                ownerType,
                ownerDeptId,
                visibility,
                KnowledgeBaseStatus.ENABLED,
                0);
    }

    public static KnowledgeBase rehydrate(
            Long id,
            String kbCode,
            String name,
            KnowledgeBaseOwnerType ownerType,
            Long ownerDeptId,
            KnowledgeBaseVisibility visibility,
            KnowledgeBaseStatus status,
            Integer version) {
        return new KnowledgeBase(id, kbCode, name, ownerType, ownerDeptId, visibility, status, version);
    }

    public void update(
            String name,
            KnowledgeBaseOwnerType ownerType,
            Long ownerDeptId,
            KnowledgeBaseVisibility visibility,
            KnowledgeBaseStatus status) {
        this.name = ArgumentChecks.requireNonBlank(name, "name");
        this.ownerType = Objects.requireNonNull(ownerType, "ownerType must not be null");
        this.ownerDeptId = normalizeOwnerDeptId(ownerType, ownerDeptId);
        this.visibility = Objects.requireNonNull(visibility, "visibility must not be null");
        this.status = validateStatus(status);
    }

    public void patch(
            String name,
            KnowledgeBaseOwnerType ownerType,
            Long ownerDeptId,
            KnowledgeBaseVisibility visibility,
            KnowledgeBaseStatus status) {
        update(
                name != null ? name : this.name,
                ownerType != null ? ownerType : this.ownerType,
                ownerType == null ? this.ownerDeptId : ownerDeptId,
                visibility != null ? visibility : this.visibility,
                status != null ? status : this.status);
    }

    private static Long normalizeOwnerDeptId(KnowledgeBaseOwnerType ownerType, Long ownerDeptId) {
        if (ownerType == KnowledgeBaseOwnerType.DEPARTMENT) {
            return ArgumentChecks.requirePositive(ownerDeptId, "ownerDeptId");
        }
        return null;
    }

    private static KnowledgeBaseStatus validateStatus(KnowledgeBaseStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        if (status == KnowledgeBaseStatus.ARCHIVED) {
            throw new BizException(AiErrorCode.KNOWLEDGE_BASE_STATUS_INVALID);
        }
        return status;
    }

    public Long id() {
        return id;
    }

    public String kbCode() {
        return kbCode;
    }

    public String name() {
        return name;
    }

    public KnowledgeBaseOwnerType ownerType() {
        return ownerType;
    }

    public Long ownerDeptId() {
        return ownerDeptId;
    }

    public KnowledgeBaseVisibility visibility() {
        return visibility;
    }

    public KnowledgeBaseStatus status() {
        return status;
    }

    public Integer version() {
        return version;
    }
}
