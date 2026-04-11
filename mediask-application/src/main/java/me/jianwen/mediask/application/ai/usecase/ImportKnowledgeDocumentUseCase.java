package me.jianwen.mediask.application.ai.usecase;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import me.jianwen.mediask.common.exception.ErrorCode;
import me.jianwen.mediask.application.ai.command.ImportKnowledgeDocumentCommand;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.ai.exception.AiErrorCode;
import me.jianwen.mediask.domain.ai.model.KnowledgeChunk;
import me.jianwen.mediask.domain.ai.model.KnowledgeDocument;
import me.jianwen.mediask.domain.ai.model.KnowledgePrepareInvocation;
import me.jianwen.mediask.domain.ai.model.KnowledgeSourceType;
import me.jianwen.mediask.domain.ai.model.PreparedKnowledgeChunk;
import me.jianwen.mediask.domain.ai.port.KnowledgeBaseRepository;
import me.jianwen.mediask.domain.ai.port.KnowledgeChunkRepository;
import me.jianwen.mediask.domain.ai.port.KnowledgeDocumentRepository;
import me.jianwen.mediask.domain.ai.port.KnowledgeDocumentStoragePort;
import me.jianwen.mediask.domain.ai.port.KnowledgeIndexPort;
import me.jianwen.mediask.domain.ai.port.KnowledgePreparePort;
import org.springframework.transaction.support.TransactionOperations;

public class ImportKnowledgeDocumentUseCase {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final KnowledgeDocumentStoragePort knowledgeDocumentStoragePort;
    private final KnowledgePreparePort knowledgePreparePort;
    private final KnowledgeIndexPort knowledgeIndexPort;
    private final TransactionOperations transactionOperations;

    public ImportKnowledgeDocumentUseCase(
            KnowledgeBaseRepository knowledgeBaseRepository,
            KnowledgeDocumentRepository knowledgeDocumentRepository,
            KnowledgeChunkRepository knowledgeChunkRepository,
            KnowledgeDocumentStoragePort knowledgeDocumentStoragePort,
            KnowledgePreparePort knowledgePreparePort,
            KnowledgeIndexPort knowledgeIndexPort,
            TransactionOperations transactionOperations) {
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
        this.knowledgeChunkRepository = knowledgeChunkRepository;
        this.knowledgeDocumentStoragePort = knowledgeDocumentStoragePort;
        this.knowledgePreparePort = knowledgePreparePort;
        this.knowledgeIndexPort = knowledgeIndexPort;
        this.transactionOperations = transactionOperations;
    }

    public ImportKnowledgeDocumentResult handle(ImportKnowledgeDocumentCommand command) {
        validate(command);
        if (!knowledgeBaseRepository.existsEnabled(command.knowledgeBaseId())) {
            throw new BizException(AiErrorCode.KNOWLEDGE_BASE_NOT_FOUND);
        }

        KnowledgeSourceType sourceType = detectSourceType(command.originalFilename(), command.contentType());
        String title = titleOf(command.originalFilename());
        String contentHash = sha256(command.fileContent());
        if (knowledgeDocumentRepository.existsEffectiveByKnowledgeBaseIdAndContentHash(
                command.knowledgeBaseId(), contentHash)) {
            throw new BizException(AiErrorCode.KNOWLEDGE_DOCUMENT_DUPLICATE);
        }
        String sourceUri = knowledgeDocumentStoragePort.store(
                command.knowledgeBaseId(), command.originalFilename(), sourceType, command.fileContent());

        KnowledgeDocument knowledgeDocument = transactionOperations.execute(status -> {
            KnowledgeDocument created = KnowledgeDocument.createUploaded(
                    command.knowledgeBaseId(), title, sourceType, sourceUri, contentHash);
            knowledgeDocumentRepository.save(created);
            return created;
        });
        if (knowledgeDocument == null) {
            throw new IllegalStateException("failed to create knowledge document");
        }

        try {
            transactionOperations.executeWithoutResult(status -> {
                KnowledgeDocument current = getRequiredDocument(knowledgeDocument.id());
                current.markParsing();
                knowledgeDocumentRepository.update(current);
            });

            List<PreparedKnowledgeChunk> preparedChunks = knowledgePreparePort.prepare(new KnowledgePrepareInvocation(
                    knowledgeDocument.id(),
                    knowledgeDocument.documentUuid(),
                    knowledgeDocument.knowledgeBaseId(),
                    knowledgeDocument.title(),
                    knowledgeDocument.sourceType(),
                    knowledgeDocument.sourceUri()));
            if (preparedChunks.isEmpty()) {
                throw new BizException(AiErrorCode.INVALID_RESPONSE);
            }

            List<KnowledgeChunk> knowledgeChunks = preparedChunks.stream()
                    .map(chunk -> KnowledgeChunk.create(
                            knowledgeDocument.knowledgeBaseId(),
                            knowledgeDocument.id(),
                            chunk.chunkIndex(),
                            chunk.sectionTitle(),
                            chunk.pageNo(),
                            chunk.charStart(),
                            chunk.charEnd(),
                            chunk.tokenCount(),
                            chunk.content(),
                            chunk.contentPreview(),
                            chunk.citationLabel()))
                    .toList();

            transactionOperations.executeWithoutResult(status -> {
                knowledgeChunkRepository.saveAll(knowledgeChunks);
                KnowledgeDocument current = getRequiredDocument(knowledgeDocument.id());
                current.markChunked();
                knowledgeDocumentRepository.update(current);
            });

            KnowledgeDocument indexingDocument = transactionOperations.execute(status -> {
                KnowledgeDocument current = getRequiredDocument(knowledgeDocument.id());
                current.markIndexing();
                knowledgeDocumentRepository.update(current);
                return current;
            });
            if (indexingDocument == null) {
                throw new IllegalStateException("failed to update indexing status");
            }

            knowledgeIndexPort.index(indexingDocument);

            KnowledgeDocument activeDocument = transactionOperations.execute(status -> {
                KnowledgeDocument current = getRequiredDocument(knowledgeDocument.id());
                current.markActive();
                knowledgeDocumentRepository.update(current);
                return current;
            });
            if (activeDocument == null) {
                throw new IllegalStateException("failed to update active status");
            }
            return new ImportKnowledgeDocumentResult(
                    activeDocument.id(),
                    activeDocument.documentUuid().toString(),
                    knowledgeChunks.size(),
                    activeDocument.status().name());
        } catch (RuntimeException exception) {
            markFailedQuietly(knowledgeDocument.id());
            throw exception;
        }
    }

    private void markFailedQuietly(Long documentId) {
        transactionOperations.executeWithoutResult(status -> knowledgeDocumentRepository.findById(documentId).ifPresent(document -> {
            if (document.status() == me.jianwen.mediask.domain.ai.model.KnowledgeDocumentStatus.ACTIVE
                    || document.status() == me.jianwen.mediask.domain.ai.model.KnowledgeDocumentStatus.FAILED
                    || document.status() == me.jianwen.mediask.domain.ai.model.KnowledgeDocumentStatus.ARCHIVED) {
                return;
            }
            document.markFailed();
            knowledgeDocumentRepository.update(document);
        }));
    }

    private KnowledgeDocument getRequiredDocument(Long documentId) {
        return knowledgeDocumentRepository
                .findById(documentId)
                .orElseThrow(() -> new BizException(AiErrorCode.KNOWLEDGE_DOCUMENT_NOT_FOUND));
    }

    private void validate(ImportKnowledgeDocumentCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command must not be null");
        }
        if (command.knowledgeBaseId() == null) {
            throw new IllegalArgumentException("knowledgeBaseId is required");
        }
        if (command.originalFilename() == null || command.originalFilename().isBlank()) {
            throw new IllegalArgumentException("originalFilename is required");
        }
        if (command.fileContent() == null || command.fileContent().length == 0) {
            throw new IllegalArgumentException("fileContent is required");
        }
    }

    private KnowledgeSourceType detectSourceType(String originalFilename, String contentType) {
        String normalizedFilename = originalFilename.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalizedFilename.endsWith(".md") || normalizedFilename.endsWith(".markdown")) {
            return KnowledgeSourceType.MARKDOWN;
        }
        if (normalizedFilename.endsWith(".docx")) {
            return KnowledgeSourceType.DOCX;
        }
        if (normalizedFilename.endsWith(".pdf")) {
            return KnowledgeSourceType.PDF;
        }
        if ("text/markdown".equalsIgnoreCase(contentType)) {
            return KnowledgeSourceType.MARKDOWN;
        }
        if ("application/vnd.openxmlformats-officedocument.wordprocessingml.document".equalsIgnoreCase(contentType)) {
            return KnowledgeSourceType.DOCX;
        }
        if ("application/pdf".equalsIgnoreCase(contentType)) {
            return KnowledgeSourceType.PDF;
        }
        throw new BizException(ErrorCode.INVALID_PARAMETER, "unsupported knowledge document file type");
    }

    private String titleOf(String originalFilename) {
        String trimmed = originalFilename.trim();
        int lastSlash = Math.max(trimmed.lastIndexOf('/'), trimmed.lastIndexOf('\\'));
        String basename = lastSlash >= 0 ? trimmed.substring(lastSlash + 1) : trimmed;
        int extensionIndex = basename.lastIndexOf('.');
        if (extensionIndex <= 0) {
            return basename;
        }
        return basename.substring(0, extensionIndex);
    }

    private String sha256(byte[] rawContent) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawContent);
            StringBuilder builder = new StringBuilder(hashBytes.length * 2);
            for (byte hashByte : hashBytes) {
                builder.append(Character.forDigit((hashByte >> 4) & 0xF, 16));
                builder.append(Character.forDigit(hashByte & 0xF, 16));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 not available", exception);
        }
    }
}
