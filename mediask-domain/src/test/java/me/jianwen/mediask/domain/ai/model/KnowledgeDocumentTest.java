package me.jianwen.mediask.domain.ai.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.ai.exception.AiErrorCode;
import org.junit.jupiter.api.Test;

class KnowledgeDocumentTest {

    @Test
    void statusFlow_WhenTransitionsAreValid_ShouldAdvance() {
        KnowledgeDocument document = KnowledgeDocument.rehydrate(
                1L,
                2L,
                UUID.randomUUID(),
                "高血压指南",
                KnowledgeSourceType.MARKDOWN,
                null,
                "hash-1",
                "zh-CN",
                1,
                "JAVA",
                KnowledgeDocumentStatus.UPLOADED,
                0);

        document.markParsing();
        document.markChunked();
        document.markIndexing();
        document.markActive();

        assertEquals(KnowledgeDocumentStatus.ACTIVE, document.status());
    }

    @Test
    void markFailed_WhenStatusIsParsing_ShouldMoveToFailed() {
        KnowledgeDocument document = KnowledgeDocument.rehydrate(
                1L,
                2L,
                UUID.randomUUID(),
                "高血压指南",
                KnowledgeSourceType.MARKDOWN,
                null,
                "hash-1",
                "zh-CN",
                1,
                "JAVA",
                KnowledgeDocumentStatus.PARSING,
                0);

        document.markFailed();

        assertEquals(KnowledgeDocumentStatus.FAILED, document.status());
    }

    @Test
    void markParsing_WhenStatusIsActive_ShouldThrowBizException() {
        KnowledgeDocument document = KnowledgeDocument.rehydrate(
                1L,
                2L,
                UUID.randomUUID(),
                "高血压指南",
                KnowledgeSourceType.MARKDOWN,
                null,
                "hash-1",
                "zh-CN",
                1,
                "JAVA",
                KnowledgeDocumentStatus.ACTIVE,
                0);

        BizException exception = assertThrows(BizException.class, document::markParsing);

        assertEquals(AiErrorCode.KNOWLEDGE_DOCUMENT_STATUS_INVALID, exception.getErrorCode());
    }

    @Test
    void createUploaded_WhenSourceUriMissing_ShouldGenerateInlineSourceUri() {
        KnowledgeDocument document =
                KnowledgeDocument.createUploaded(2L, "高血压指南", KnowledgeSourceType.MARKDOWN, null, "hash-1");

        assertTrue(document.sourceUri().startsWith("inline://admin-knowledge-document/"));
        assertTrue(document.sourceUri().endsWith(document.documentUuid().toString()));
    }

    @Test
    void createUploaded_WhenSourceUriProvided_ShouldKeepSourceUri() {
        KnowledgeDocument document = KnowledgeDocument.createUploaded(
                2L, "高血压指南", KnowledgeSourceType.MARKDOWN, "oss://mediask/kb/htn-guide-v1.md", "hash-1");

        assertEquals("oss://mediask/kb/htn-guide-v1.md", document.sourceUri());
    }
}
