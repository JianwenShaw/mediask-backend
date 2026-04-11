package me.jianwen.mediask.application.ai.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import me.jianwen.mediask.application.ai.command.CreateKnowledgeBaseCommand;
import me.jianwen.mediask.application.ai.command.DeleteKnowledgeBaseCommand;
import me.jianwen.mediask.application.ai.command.DeleteKnowledgeDocumentCommand;
import me.jianwen.mediask.application.ai.command.UpdateKnowledgeBaseCommand;
import me.jianwen.mediask.application.ai.query.ListKnowledgeBasesQuery;
import me.jianwen.mediask.application.ai.query.ListKnowledgeDocumentsQuery;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.pagination.PageData;
import me.jianwen.mediask.common.pagination.PageQuery;
import me.jianwen.mediask.domain.ai.exception.AiErrorCode;
import me.jianwen.mediask.domain.ai.model.KnowledgeBase;
import me.jianwen.mediask.domain.ai.model.KnowledgeBaseOwnerType;
import me.jianwen.mediask.domain.ai.model.KnowledgeBaseStatus;
import me.jianwen.mediask.domain.ai.model.KnowledgeBaseSummary;
import me.jianwen.mediask.domain.ai.model.KnowledgeBaseVisibility;
import me.jianwen.mediask.domain.ai.model.KnowledgeDocument;
import me.jianwen.mediask.domain.ai.model.KnowledgeDocumentStatus;
import me.jianwen.mediask.domain.ai.model.KnowledgeDocumentSummary;
import me.jianwen.mediask.domain.ai.model.KnowledgeSourceType;
import me.jianwen.mediask.domain.ai.port.KnowledgeBaseRepository;
import me.jianwen.mediask.domain.ai.port.KnowledgeDocumentRepository;
import org.junit.jupiter.api.Test;

class KnowledgeAdminUseCaseTest {

    @Test
    void createKnowledgeBase_WhenDepartmentOwnerMissingDeptId_ShouldThrow() {
        CreateKnowledgeBaseUseCase useCase = new CreateKnowledgeBaseUseCase(new InMemoryKnowledgeBaseRepository());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.handle(new CreateKnowledgeBaseCommand("科室知识库", "KB_DEPT_1", "DEPARTMENT", null, "DEPT")));

        assertEquals("ownerDeptId must be greater than 0", exception.getMessage());
    }

    @Test
    void createKnowledgeBase_WhenValid_ShouldPersistEnabledKnowledgeBase() {
        InMemoryKnowledgeBaseRepository repository = new InMemoryKnowledgeBaseRepository();
        CreateKnowledgeBaseUseCase useCase = new CreateKnowledgeBaseUseCase(repository);

        KnowledgeBaseSummary knowledgeBase = useCase.handle(
                new CreateKnowledgeBaseCommand("系统库", "KB_SYSTEM_1", "SYSTEM", null, "PUBLIC"));

        assertNotNull(knowledgeBase.id());
        assertEquals("KB_SYSTEM_1", knowledgeBase.kbCode());
        assertEquals("ENABLED", knowledgeBase.status().name());
        assertEquals(knowledgeBase.id(), repository.lastSaved.id());
    }

    @Test
    void updateKnowledgeBase_WhenExisting_ShouldUpdateMutableFields() {
        InMemoryKnowledgeBaseRepository repository = new InMemoryKnowledgeBaseRepository();
        KnowledgeBase existing = KnowledgeBase.create("KB_A", "旧名称", KnowledgeBaseOwnerType.SYSTEM, null, KnowledgeBaseVisibility.PUBLIC);
        repository.lastSaved = existing;
        repository.items.put(existing.id(), existing);
        UpdateKnowledgeBaseUseCase useCase = new UpdateKnowledgeBaseUseCase(repository);

        KnowledgeBaseSummary updated = useCase.handle(
                new UpdateKnowledgeBaseCommand(existing.id(), "新名称", "DEPARTMENT", 3103L, "DEPT", "DISABLED"));

        assertEquals("新名称", updated.name());
        assertEquals("DEPARTMENT", updated.ownerType().name());
        assertEquals(3103L, updated.ownerDeptId());
        assertEquals("DEPT", updated.visibility().name());
        assertEquals("DISABLED", updated.status().name());
    }

    @Test
    void updateKnowledgeBase_WhenOnlyStatusProvided_ShouldKeepOtherFields() {
        InMemoryKnowledgeBaseRepository repository = new InMemoryKnowledgeBaseRepository();
        KnowledgeBase existing = KnowledgeBase.create("KB_A", "旧名称", KnowledgeBaseOwnerType.SYSTEM, null, KnowledgeBaseVisibility.PUBLIC);
        repository.items.put(existing.id(), existing);
        UpdateKnowledgeBaseUseCase useCase = new UpdateKnowledgeBaseUseCase(repository);

        KnowledgeBaseSummary updated =
                useCase.handle(new UpdateKnowledgeBaseCommand(existing.id(), null, null, null, null, "DISABLED"));

        assertEquals("旧名称", updated.name());
        assertEquals(KnowledgeBaseOwnerType.SYSTEM, updated.ownerType());
        assertEquals(KnowledgeBaseVisibility.PUBLIC, updated.visibility());
        assertEquals(KnowledgeBaseStatus.DISABLED, updated.status());
    }

    @Test
    void deleteKnowledgeBase_WhenMissing_ShouldThrowBizException() {
        DeleteKnowledgeBaseUseCase useCase = new DeleteKnowledgeBaseUseCase(new InMemoryKnowledgeBaseRepository());

        BizException exception = assertThrows(
                BizException.class, () -> useCase.handle(new DeleteKnowledgeBaseCommand(999L)));

        assertEquals(AiErrorCode.KNOWLEDGE_BASE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void listKnowledgeBases_ShouldDelegatePagingQuery() {
        InMemoryKnowledgeBaseRepository repository = new InMemoryKnowledgeBaseRepository();
        repository.pageResult = new PageData<>(
                List.of(new KnowledgeBaseSummary(
                        1L,
                        "KB_SYSTEM_1",
                        "系统库",
                        KnowledgeBaseOwnerType.SYSTEM,
                        null,
                        KnowledgeBaseVisibility.PUBLIC,
                        me.jianwen.mediask.domain.ai.model.KnowledgeBaseStatus.ENABLED,
                        2L)),
                1,
                20,
                1,
                1,
                false);

        PageData<KnowledgeBaseSummary> page = new ListKnowledgeBasesUseCase(repository)
                .handle(ListKnowledgeBasesQuery.page("system", null, null));

        assertEquals(1, page.items().size());
        assertEquals("system", repository.lastKeyword);
    }

    @Test
    void listKnowledgeDocuments_ShouldDelegatePagingQuery() {
        InMemoryKnowledgeDocumentRepository repository = new InMemoryKnowledgeDocumentRepository();
        repository.pageResult = new PageData<>(
                List.of(new KnowledgeDocumentSummary(
                        11L, "uuid-1", "高血压指南", KnowledgeSourceType.PDF, KnowledgeDocumentStatus.ACTIVE, 6L)),
                1,
                20,
                1,
                1,
                false);

        PageData<KnowledgeDocumentSummary> page = new ListKnowledgeDocumentsUseCase(repository)
                .handle(ListKnowledgeDocumentsQuery.page(4001L, null, null));

        assertEquals(1, page.items().size());
        assertEquals(4001L, repository.lastKnowledgeBaseId);
    }

    @Test
    void deleteKnowledgeDocument_WhenExisting_ShouldDelete() {
        InMemoryKnowledgeDocumentRepository repository = new InMemoryKnowledgeDocumentRepository();
        KnowledgeDocument document = KnowledgeDocument.createUploaded(
                4001L, "指南", KnowledgeSourceType.MARKDOWN, "file:///tmp/guide.md", "hash-1");
        repository.documents.put(document.id(), document);

        new DeleteKnowledgeDocumentUseCase(repository).handle(new DeleteKnowledgeDocumentCommand(document.id()));

        assertTrue(repository.deletedDocumentIds.contains(document.id()));
    }

    private static final class InMemoryKnowledgeBaseRepository implements KnowledgeBaseRepository {

        private final Map<Long, KnowledgeBase> items = new LinkedHashMap<>();
        private KnowledgeBase lastSaved;
        private String lastKeyword;
        private PageData<KnowledgeBaseSummary> pageResult = new PageData<>(List.of(), 1, 20, 0, 0, false);

        @Override
        public boolean existsEnabled(Long knowledgeBaseId) {
            KnowledgeBase knowledgeBase = items.get(knowledgeBaseId);
            return knowledgeBase != null && knowledgeBase.status().name().equals("ENABLED");
        }

        @Override
        public PageData<KnowledgeBaseSummary> pageByKeyword(String keyword, PageQuery pageQuery) {
            lastKeyword = keyword;
            return pageResult;
        }

        @Override
        public void save(KnowledgeBase knowledgeBase) {
            items.put(knowledgeBase.id(), knowledgeBase);
            lastSaved = knowledgeBase;
        }

        @Override
        public Optional<KnowledgeBase> findById(Long knowledgeBaseId) {
            return Optional.ofNullable(items.get(knowledgeBaseId));
        }

        @Override
        public Optional<KnowledgeBaseSummary> findSummaryById(Long knowledgeBaseId) {
            KnowledgeBase knowledgeBase = items.get(knowledgeBaseId);
            if (knowledgeBase == null) {
                return Optional.empty();
            }
            return Optional.of(new KnowledgeBaseSummary(
                    knowledgeBase.id(),
                    knowledgeBase.kbCode(),
                    knowledgeBase.name(),
                    knowledgeBase.ownerType(),
                    knowledgeBase.ownerDeptId(),
                    knowledgeBase.visibility(),
                    knowledgeBase.status(),
                    0L));
        }

        @Override
        public void update(KnowledgeBase knowledgeBase) {
            items.put(knowledgeBase.id(), knowledgeBase);
        }

        @Override
        public void deleteById(Long knowledgeBaseId) {
            items.remove(knowledgeBaseId);
        }
    }

    private static final class InMemoryKnowledgeDocumentRepository implements KnowledgeDocumentRepository {

        private final Map<Long, KnowledgeDocument> documents = new LinkedHashMap<>();
        private final List<Long> deletedDocumentIds = new java.util.ArrayList<>();
        private Long lastKnowledgeBaseId;
        private PageData<KnowledgeDocumentSummary> pageResult = new PageData<>(List.of(), 1, 20, 0, 0, false);

        @Override
        public void save(KnowledgeDocument knowledgeDocument) {
            documents.put(knowledgeDocument.id(), knowledgeDocument);
        }

        @Override
        public Optional<KnowledgeDocument> findById(Long documentId) {
            return Optional.ofNullable(documents.get(documentId));
        }

        @Override
        public boolean existsEffectiveByKnowledgeBaseIdAndContentHash(Long knowledgeBaseId, String contentHash) {
            return false;
        }

        @Override
        public void update(KnowledgeDocument knowledgeDocument) {
            documents.put(knowledgeDocument.id(), knowledgeDocument);
        }

        @Override
        public PageData<KnowledgeDocumentSummary> pageByKnowledgeBaseId(Long knowledgeBaseId, PageQuery pageQuery) {
            lastKnowledgeBaseId = knowledgeBaseId;
            return pageResult;
        }

        @Override
        public void deleteById(Long documentId) {
            deletedDocumentIds.add(documentId);
            documents.remove(documentId);
        }
    }
}
