package me.jianwen.mediask.application.ai.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import me.jianwen.mediask.application.ai.command.ImportKnowledgeDocumentCommand;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.ai.exception.AiErrorCode;
import me.jianwen.mediask.domain.ai.model.KnowledgeChunk;
import me.jianwen.mediask.domain.ai.model.KnowledgeDocument;
import me.jianwen.mediask.domain.ai.model.KnowledgeDocumentStatus;
import me.jianwen.mediask.domain.ai.model.KnowledgePrepareInvocation;
import me.jianwen.mediask.domain.ai.model.PreparedKnowledgeChunk;
import me.jianwen.mediask.domain.ai.port.KnowledgeBaseRepository;
import me.jianwen.mediask.domain.ai.port.KnowledgeChunkRepository;
import me.jianwen.mediask.domain.ai.port.KnowledgeDocumentRepository;
import me.jianwen.mediask.domain.ai.port.KnowledgeIndexPort;
import me.jianwen.mediask.domain.ai.port.KnowledgePreparePort;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionOperations;

class ImportKnowledgeDocumentUseCaseTest {

    private final ImmediateTransactionOperations transactionOperations = new ImmediateTransactionOperations();

    @Test
    void handle_WhenImportSucceeds_ShouldPersistDocumentAndChunks() {
        InMemoryKnowledgeBaseRepository knowledgeBaseRepository = new InMemoryKnowledgeBaseRepository(true);
        InMemoryKnowledgeDocumentRepository knowledgeDocumentRepository = new InMemoryKnowledgeDocumentRepository();
        CapturingKnowledgeChunkRepository knowledgeChunkRepository = new CapturingKnowledgeChunkRepository();
        StubKnowledgePreparePort knowledgePreparePort = new StubKnowledgePreparePort(List.of(
                new PreparedKnowledgeChunk(0, "第一段", "第1节", null, 0, 10, 5, "第一段", "引用1"),
                new PreparedKnowledgeChunk(1, "第二段", "第2节", null, 11, 20, 5, "第二段", "引用2")));
        CapturingKnowledgeIndexPort knowledgeIndexPort = new CapturingKnowledgeIndexPort();
        ImportKnowledgeDocumentUseCase useCase = new ImportKnowledgeDocumentUseCase(
                knowledgeBaseRepository,
                knowledgeDocumentRepository,
                knowledgeChunkRepository,
                knowledgePreparePort,
                knowledgeIndexPort,
                transactionOperations);

        ImportKnowledgeDocumentResult result =
                useCase.handle(new ImportKnowledgeDocumentCommand(10L, "指南", "MARKDOWN", null, "# 内容"));

        assertEquals(2, result.chunkCount());
        assertEquals("ACTIVE", result.documentStatus());
        assertEquals(KnowledgeDocumentStatus.ACTIVE, knowledgeDocumentRepository.getRequired(result.documentId()).status());
        assertTrue(knowledgeDocumentRepository.getRequired(result.documentId())
                .sourceUri()
                .startsWith("inline://admin-knowledge-document/"));
        assertEquals(2, knowledgeChunkRepository.savedChunks.size());
        assertTrue(knowledgeIndexPort.called);
        assertEquals(2, knowledgeIndexPort.indexedChunks.size());
        assertEquals(
                knowledgeDocumentRepository.getRequired(result.documentId()).sourceUri(),
                knowledgePreparePort.lastInvocation.sourceUri());
    }

    @Test
    void handle_WhenKnowledgeBaseMissing_ShouldThrowBizException() {
        ImportKnowledgeDocumentUseCase useCase = new ImportKnowledgeDocumentUseCase(
                new InMemoryKnowledgeBaseRepository(false),
                new InMemoryKnowledgeDocumentRepository(),
                knowledgeChunks -> {},
                invocation -> List.of(),
                (knowledgeDocument, knowledgeChunks) -> {},
                transactionOperations);

        BizException exception = assertThrows(
                BizException.class,
                () -> useCase.handle(new ImportKnowledgeDocumentCommand(10L, "指南", "MARKDOWN", null, "# 内容")));

        assertEquals(AiErrorCode.KNOWLEDGE_BASE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void handle_WhenDocumentDuplicate_ShouldThrowBizException() {
        InMemoryKnowledgeDocumentRepository knowledgeDocumentRepository = new InMemoryKnowledgeDocumentRepository();
        knowledgeDocumentRepository.existingDuplicate = true;
        ImportKnowledgeDocumentUseCase useCase = new ImportKnowledgeDocumentUseCase(
                new InMemoryKnowledgeBaseRepository(true),
                knowledgeDocumentRepository,
                knowledgeChunks -> {},
                invocation -> List.of(),
                (knowledgeDocument, knowledgeChunks) -> {},
                transactionOperations);

        BizException exception = assertThrows(
                BizException.class,
                () -> useCase.handle(new ImportKnowledgeDocumentCommand(10L, "指南", "MARKDOWN", null, "# 内容")));

        assertEquals(AiErrorCode.KNOWLEDGE_DOCUMENT_DUPLICATE, exception.getErrorCode());
    }

    @Test
    void handle_WhenIndexFails_ShouldMarkDocumentFailed() {
        InMemoryKnowledgeDocumentRepository knowledgeDocumentRepository = new InMemoryKnowledgeDocumentRepository();
        ImportKnowledgeDocumentUseCase useCase = new ImportKnowledgeDocumentUseCase(
                new InMemoryKnowledgeBaseRepository(true),
                knowledgeDocumentRepository,
                new CapturingKnowledgeChunkRepository(),
                new StubKnowledgePreparePort(List.of(new PreparedKnowledgeChunk(0, "第一段", null, null, 0, 10, 5, null, null))),
                (knowledgeDocument, knowledgeChunks) -> {
                    throw new BizException(AiErrorCode.SERVICE_UNAVAILABLE);
                },
                transactionOperations);

        BizException exception = assertThrows(
                BizException.class,
                () -> useCase.handle(new ImportKnowledgeDocumentCommand(10L, "指南", "MARKDOWN", null, "# 内容")));

        assertEquals(AiErrorCode.SERVICE_UNAVAILABLE, exception.getErrorCode());
        assertEquals(KnowledgeDocumentStatus.FAILED, knowledgeDocumentRepository.lastSavedDocument.status());
    }

    @Test
    void handle_WhenSourceUriProvided_ShouldPersistAndForwardSourceUri() {
        InMemoryKnowledgeDocumentRepository knowledgeDocumentRepository = new InMemoryKnowledgeDocumentRepository();
        StubKnowledgePreparePort knowledgePreparePort =
                new StubKnowledgePreparePort(List.of(new PreparedKnowledgeChunk(0, "第一段", null, null, 0, 10, 5, null, null)));
        ImportKnowledgeDocumentUseCase useCase = new ImportKnowledgeDocumentUseCase(
                new InMemoryKnowledgeBaseRepository(true),
                knowledgeDocumentRepository,
                new CapturingKnowledgeChunkRepository(),
                knowledgePreparePort,
                new CapturingKnowledgeIndexPort(),
                transactionOperations);

        ImportKnowledgeDocumentResult result = useCase.handle(new ImportKnowledgeDocumentCommand(
                10L, "指南", "MARKDOWN", "oss://mediask/kb/htn-guide-v1.md", "# 内容"));

        assertEquals(
                "oss://mediask/kb/htn-guide-v1.md",
                knowledgeDocumentRepository.getRequired(result.documentId()).sourceUri());
        assertEquals("oss://mediask/kb/htn-guide-v1.md", knowledgePreparePort.lastInvocation.sourceUri());
    }

    @Test
    void handle_WhenPdfSourceProvided_ShouldForwardSourceTypeAndSourceUri() {
        InMemoryKnowledgeDocumentRepository knowledgeDocumentRepository = new InMemoryKnowledgeDocumentRepository();
        StubKnowledgePreparePort knowledgePreparePort =
                new StubKnowledgePreparePort(List.of(new PreparedKnowledgeChunk(0, "第一段", null, null, 0, 10, 5, null, null)));
        ImportKnowledgeDocumentUseCase useCase = new ImportKnowledgeDocumentUseCase(
                new InMemoryKnowledgeBaseRepository(true),
                knowledgeDocumentRepository,
                new CapturingKnowledgeChunkRepository(),
                knowledgePreparePort,
                new CapturingKnowledgeIndexPort(),
                transactionOperations);

        ImportKnowledgeDocumentResult result = useCase.handle(new ImportKnowledgeDocumentCommand(
                10L, "指南", "PDF", "oss://mediask/kb/htn-guide-v1.pdf", null));

        assertEquals("PDF", knowledgeDocumentRepository.getRequired(result.documentId()).sourceType().name());
        assertEquals("PDF", knowledgePreparePort.lastInvocation.sourceType().name());
        assertEquals("oss://mediask/kb/htn-guide-v1.pdf", knowledgePreparePort.lastInvocation.sourceUri());
        assertEquals(null, knowledgePreparePort.lastInvocation.inlineContent());
    }

    private static final class ImmediateTransactionOperations implements TransactionOperations {

        @Override
        public <T> T execute(org.springframework.transaction.support.TransactionCallback<T> action) {
            return action.doInTransaction(new SimpleTransactionStatus());
        }
    }

    private static final class InMemoryKnowledgeBaseRepository implements KnowledgeBaseRepository {

        private final boolean existsEnabled;

        private InMemoryKnowledgeBaseRepository(boolean existsEnabled) {
            this.existsEnabled = existsEnabled;
        }

        @Override
        public boolean existsEnabled(Long knowledgeBaseId) {
            return existsEnabled;
        }
    }

    private static final class InMemoryKnowledgeDocumentRepository implements KnowledgeDocumentRepository {

        private final Map<Long, KnowledgeDocument> documents = new LinkedHashMap<>();
        private boolean existingDuplicate;
        private KnowledgeDocument lastSavedDocument;

        @Override
        public void save(KnowledgeDocument knowledgeDocument) {
            documents.put(knowledgeDocument.id(), knowledgeDocument);
            lastSavedDocument = knowledgeDocument;
        }

        @Override
        public Optional<KnowledgeDocument> findById(Long documentId) {
            return Optional.ofNullable(documents.get(documentId));
        }

        @Override
        public boolean existsEffectiveByKnowledgeBaseIdAndContentHash(Long knowledgeBaseId, String contentHash) {
            return existingDuplicate;
        }

        @Override
        public void update(KnowledgeDocument knowledgeDocument) {
            documents.put(knowledgeDocument.id(), knowledgeDocument);
            lastSavedDocument = knowledgeDocument;
        }

        private KnowledgeDocument getRequired(Long documentId) {
            return documents.get(documentId);
        }
    }

    private static final class CapturingKnowledgeChunkRepository implements KnowledgeChunkRepository {

        private final List<KnowledgeChunk> savedChunks = new ArrayList<>();

        @Override
        public void saveAll(List<KnowledgeChunk> knowledgeChunks) {
            savedChunks.addAll(knowledgeChunks);
        }
    }

    private static final class StubKnowledgePreparePort implements KnowledgePreparePort {

        private final List<PreparedKnowledgeChunk> chunks;
        private KnowledgePrepareInvocation lastInvocation;

        private StubKnowledgePreparePort(List<PreparedKnowledgeChunk> chunks) {
            this.chunks = chunks;
        }

        @Override
        public List<PreparedKnowledgeChunk> prepare(KnowledgePrepareInvocation invocation) {
            lastInvocation = invocation;
            return chunks;
        }
    }

    private static final class CapturingKnowledgeIndexPort implements KnowledgeIndexPort {

        private boolean called;
        private List<KnowledgeChunk> indexedChunks = List.of();

        @Override
        public void index(KnowledgeDocument knowledgeDocument, List<KnowledgeChunk> knowledgeChunks) {
            called = true;
            indexedChunks = knowledgeChunks;
        }
    }
}
