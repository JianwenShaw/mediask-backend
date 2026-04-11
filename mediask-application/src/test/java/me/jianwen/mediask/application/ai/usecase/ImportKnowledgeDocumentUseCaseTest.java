package me.jianwen.mediask.application.ai.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import me.jianwen.mediask.application.ai.command.ImportKnowledgeDocumentCommand;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.pagination.PageData;
import me.jianwen.mediask.common.pagination.PageQuery;
import me.jianwen.mediask.domain.ai.exception.AiErrorCode;
import me.jianwen.mediask.domain.ai.model.KnowledgeBase;
import me.jianwen.mediask.domain.ai.model.KnowledgeBaseSummary;
import me.jianwen.mediask.domain.ai.model.KnowledgeChunk;
import me.jianwen.mediask.domain.ai.model.KnowledgeDocument;
import me.jianwen.mediask.domain.ai.model.KnowledgeDocumentStatus;
import me.jianwen.mediask.domain.ai.model.KnowledgeDocumentSummary;
import me.jianwen.mediask.domain.ai.model.KnowledgePrepareInvocation;
import me.jianwen.mediask.domain.ai.model.PreparedKnowledgeChunk;
import me.jianwen.mediask.domain.ai.port.KnowledgeBaseRepository;
import me.jianwen.mediask.domain.ai.port.KnowledgeChunkRepository;
import me.jianwen.mediask.domain.ai.port.KnowledgeDocumentRepository;
import me.jianwen.mediask.domain.ai.port.KnowledgeDocumentStoragePort;
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
        CapturingKnowledgeDocumentStoragePort knowledgeDocumentStoragePort = new CapturingKnowledgeDocumentStoragePort();
        StubKnowledgePreparePort knowledgePreparePort = new StubKnowledgePreparePort(List.of(
                new PreparedKnowledgeChunk(0, "第一段", "第1节", null, 0, 10, 5, "第一段", "引用1"),
                new PreparedKnowledgeChunk(1, "第二段", "第2节", null, 11, 20, 5, "第二段", "引用2")));
        CapturingKnowledgeIndexPort knowledgeIndexPort = new CapturingKnowledgeIndexPort();
        ImportKnowledgeDocumentUseCase useCase = new ImportKnowledgeDocumentUseCase(
                knowledgeBaseRepository,
                knowledgeDocumentRepository,
                knowledgeChunkRepository,
                knowledgeDocumentStoragePort,
                knowledgePreparePort,
                knowledgeIndexPort,
                transactionOperations);

        ImportKnowledgeDocumentResult result =
                useCase.handle(new ImportKnowledgeDocumentCommand(
                        10L, "htn-guide.md", "text/markdown", "# 内容".getBytes(StandardCharsets.UTF_8)));

        assertEquals(2, result.chunkCount());
        assertEquals("ACTIVE", result.documentStatus());
        assertEquals(KnowledgeDocumentStatus.ACTIVE, knowledgeDocumentRepository.getRequired(result.documentId()).status());
        assertEquals("oss://mediask/knowledge-documents/htn-guide.md", knowledgeDocumentRepository.getRequired(result.documentId())
                .sourceUri());
        assertEquals(2, knowledgeChunkRepository.savedChunks.size());
        assertTrue(knowledgeIndexPort.called);
        assertEquals(result.documentId(), knowledgeIndexPort.indexedDocument.id());
        assertEquals(
                knowledgeDocumentRepository.getRequired(result.documentId()).sourceUri(),
                knowledgePreparePort.lastInvocation.sourceUri());
        assertEquals("htn-guide.md", knowledgeDocumentStoragePort.originalFilename);
    }

    @Test
    void handle_WhenKnowledgeBaseMissing_ShouldThrowBizException() {
        ImportKnowledgeDocumentUseCase useCase = new ImportKnowledgeDocumentUseCase(
                new InMemoryKnowledgeBaseRepository(false),
                new InMemoryKnowledgeDocumentRepository(),
                knowledgeChunks -> {},
                (knowledgeBaseId, originalFilename, sourceType, fileContent) -> "oss://mediask/knowledge-documents/" + originalFilename,
                invocation -> List.of(),
                knowledgeDocument -> {},
                transactionOperations);

        BizException exception = assertThrows(
                BizException.class,
                () -> useCase.handle(new ImportKnowledgeDocumentCommand(
                        10L, "guide.md", "text/markdown", "# 内容".getBytes(StandardCharsets.UTF_8))));

        assertEquals(AiErrorCode.KNOWLEDGE_BASE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void handle_WhenDocumentDuplicate_ShouldThrowBizException() {
        InMemoryKnowledgeDocumentRepository knowledgeDocumentRepository = new InMemoryKnowledgeDocumentRepository();
        knowledgeDocumentRepository.existingDuplicate = true;
        CapturingKnowledgeDocumentStoragePort knowledgeDocumentStoragePort = new CapturingKnowledgeDocumentStoragePort();
        ImportKnowledgeDocumentUseCase useCase = new ImportKnowledgeDocumentUseCase(
                new InMemoryKnowledgeBaseRepository(true),
                knowledgeDocumentRepository,
                knowledgeChunks -> {},
                knowledgeDocumentStoragePort,
                invocation -> List.of(),
                knowledgeDocument -> {},
                transactionOperations);

        BizException exception = assertThrows(
                BizException.class,
                () -> useCase.handle(new ImportKnowledgeDocumentCommand(
                        10L, "guide.md", "text/markdown", "# 内容".getBytes(StandardCharsets.UTF_8))));

        assertEquals(AiErrorCode.KNOWLEDGE_DOCUMENT_DUPLICATE, exception.getErrorCode());
        assertEquals(0, knowledgeDocumentStoragePort.storeCallCount);
    }

    @Test
    void handle_WhenIndexFails_ShouldMarkDocumentFailed() {
        InMemoryKnowledgeDocumentRepository knowledgeDocumentRepository = new InMemoryKnowledgeDocumentRepository();
        ImportKnowledgeDocumentUseCase useCase = new ImportKnowledgeDocumentUseCase(
                new InMemoryKnowledgeBaseRepository(true),
                knowledgeDocumentRepository,
                new CapturingKnowledgeChunkRepository(),
                (knowledgeBaseId, originalFilename, sourceType, fileContent) -> "oss://mediask/knowledge-documents/" + originalFilename,
                new StubKnowledgePreparePort(List.of(new PreparedKnowledgeChunk(0, "第一段", null, null, 0, 10, 5, null, null))),
                knowledgeDocument -> {
                    throw new BizException(AiErrorCode.SERVICE_UNAVAILABLE);
                },
                transactionOperations);

        BizException exception = assertThrows(
                BizException.class,
                () -> useCase.handle(new ImportKnowledgeDocumentCommand(
                        10L, "guide.md", "text/markdown", "# 内容".getBytes(StandardCharsets.UTF_8))));

        assertEquals(AiErrorCode.SERVICE_UNAVAILABLE, exception.getErrorCode());
        assertEquals(KnowledgeDocumentStatus.FAILED, knowledgeDocumentRepository.lastSavedDocument.status());
    }

    @Test
    void handle_WhenMarkdownFileUploaded_ShouldPersistAndForwardGeneratedSourceUri() {
        InMemoryKnowledgeDocumentRepository knowledgeDocumentRepository = new InMemoryKnowledgeDocumentRepository();
        CapturingKnowledgeDocumentStoragePort knowledgeDocumentStoragePort = new CapturingKnowledgeDocumentStoragePort();
        StubKnowledgePreparePort knowledgePreparePort =
                new StubKnowledgePreparePort(List.of(new PreparedKnowledgeChunk(0, "第一段", null, null, 0, 10, 5, null, null)));
        ImportKnowledgeDocumentUseCase useCase = new ImportKnowledgeDocumentUseCase(
                new InMemoryKnowledgeBaseRepository(true),
                knowledgeDocumentRepository,
                new CapturingKnowledgeChunkRepository(),
                knowledgeDocumentStoragePort,
                knowledgePreparePort,
                new CapturingKnowledgeIndexPort(),
                transactionOperations);

        ImportKnowledgeDocumentResult result = useCase.handle(new ImportKnowledgeDocumentCommand(
                10L, "htn-guide-v1.md", "text/markdown", "# 内容".getBytes(StandardCharsets.UTF_8)));

        assertEquals(
                "oss://mediask/knowledge-documents/htn-guide-v1.md",
                knowledgeDocumentRepository.getRequired(result.documentId()).sourceUri());
        assertEquals("oss://mediask/knowledge-documents/htn-guide-v1.md", knowledgePreparePort.lastInvocation.sourceUri());
        assertEquals("htn-guide-v1.md", knowledgeDocumentStoragePort.originalFilename);
    }

    @Test
    void handle_WhenPdfFileUploaded_ShouldForwardSourceTypeAndSourceUri() {
        InMemoryKnowledgeDocumentRepository knowledgeDocumentRepository = new InMemoryKnowledgeDocumentRepository();
        CapturingKnowledgeDocumentStoragePort knowledgeDocumentStoragePort = new CapturingKnowledgeDocumentStoragePort();
        StubKnowledgePreparePort knowledgePreparePort =
                new StubKnowledgePreparePort(List.of(new PreparedKnowledgeChunk(0, "第一段", null, null, 0, 10, 5, null, null)));
        ImportKnowledgeDocumentUseCase useCase = new ImportKnowledgeDocumentUseCase(
                new InMemoryKnowledgeBaseRepository(true),
                knowledgeDocumentRepository,
                new CapturingKnowledgeChunkRepository(),
                knowledgeDocumentStoragePort,
                knowledgePreparePort,
                new CapturingKnowledgeIndexPort(),
                transactionOperations);

        ImportKnowledgeDocumentResult result = useCase.handle(new ImportKnowledgeDocumentCommand(
                10L, "htn-guide-v1.pdf", "application/pdf", "PDF 内容".getBytes(StandardCharsets.UTF_8)));

        assertEquals("PDF", knowledgeDocumentRepository.getRequired(result.documentId()).sourceType().name());
        assertEquals("PDF", knowledgePreparePort.lastInvocation.sourceType().name());
        assertEquals("oss://mediask/knowledge-documents/htn-guide-v1.pdf", knowledgePreparePort.lastInvocation.sourceUri());
    }

    @Test
    void handle_WhenDocxFileUploaded_ShouldDetectDocxSourceType() {
        InMemoryKnowledgeDocumentRepository knowledgeDocumentRepository = new InMemoryKnowledgeDocumentRepository();
        StubKnowledgePreparePort knowledgePreparePort =
                new StubKnowledgePreparePort(List.of(new PreparedKnowledgeChunk(0, "第一段", null, null, 0, 10, 5, null, null)));
        ImportKnowledgeDocumentUseCase useCase = new ImportKnowledgeDocumentUseCase(
                new InMemoryKnowledgeBaseRepository(true),
                knowledgeDocumentRepository,
                new CapturingKnowledgeChunkRepository(),
                new CapturingKnowledgeDocumentStoragePort(),
                knowledgePreparePort,
                new CapturingKnowledgeIndexPort(),
                transactionOperations);

        ImportKnowledgeDocumentResult result = useCase.handle(new ImportKnowledgeDocumentCommand(
                10L,
                "htn-guide-v1.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "DOCX 内容".getBytes(StandardCharsets.UTF_8)));

        assertEquals("DOCX", knowledgeDocumentRepository.getRequired(result.documentId()).sourceType().name());
        assertEquals("DOCX", knowledgePreparePort.lastInvocation.sourceType().name());
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

        @Override
        public PageData<me.jianwen.mediask.domain.ai.model.KnowledgeBaseSummary> pageByKeyword(
                String keyword, PageQuery pageQuery) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void save(KnowledgeBase knowledgeBase) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<KnowledgeBase> findById(Long knowledgeBaseId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<KnowledgeBaseSummary> findSummaryById(Long knowledgeBaseId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void update(KnowledgeBase knowledgeBase) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteById(Long knowledgeBaseId) {
            throw new UnsupportedOperationException();
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

        @Override
        public PageData<KnowledgeDocumentSummary> pageByKnowledgeBaseId(Long knowledgeBaseId, PageQuery pageQuery) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteById(Long documentId) {
            throw new UnsupportedOperationException();
        }

        private KnowledgeDocument getRequired(Long documentId) {
            return documents.get(documentId);
        }
    }

    private static final class CapturingKnowledgeDocumentStoragePort implements KnowledgeDocumentStoragePort {

        private String originalFilename;
        private int storeCallCount;

        @Override
        public String store(
                Long knowledgeBaseId,
                String originalFilename,
                me.jianwen.mediask.domain.ai.model.KnowledgeSourceType sourceType,
                byte[] fileContent) {
            storeCallCount++;
            this.originalFilename = originalFilename;
            return "oss://mediask/knowledge-documents/" + originalFilename;
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
        private KnowledgeDocument indexedDocument;

        @Override
        public void index(KnowledgeDocument knowledgeDocument) {
            called = true;
            indexedDocument = knowledgeDocument;
        }
    }
}
