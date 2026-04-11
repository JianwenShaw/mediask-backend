package me.jianwen.mediask.infra.ai.adapter;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import me.jianwen.mediask.domain.ai.model.KnowledgeSourceType;
import me.jianwen.mediask.infra.ai.config.KnowledgeDocumentStorageMode;
import me.jianwen.mediask.infra.ai.config.KnowledgeDocumentStorageProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalKnowledgeDocumentStorageAdapterTest {

    @TempDir
    Path tempDir;

    @Test
    void store_ShouldWriteFileAndReturnFileUri() throws Exception {
        LocalKnowledgeDocumentStorageAdapter adapter = new LocalKnowledgeDocumentStorageAdapter(
                new KnowledgeDocumentStorageProperties(
                        KnowledgeDocumentStorageMode.LOCAL,
                        new KnowledgeDocumentStorageProperties.Local(tempDir),
                        new KnowledgeDocumentStorageProperties.Oss(null, null)));

        String uri = adapter.store(
                4001L, "htn-guide.pdf", KnowledgeSourceType.PDF, "PDF".getBytes(StandardCharsets.UTF_8));

        assertTrue(uri.startsWith("file:"));
        Path storedPath = Path.of(java.net.URI.create(uri));
        assertTrue(Files.exists(storedPath));
        assertTrue(storedPath.startsWith(tempDir));
    }
}
