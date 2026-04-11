package me.jianwen.mediask.infra.ai.adapter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import me.jianwen.mediask.common.util.ArgumentChecks;
import me.jianwen.mediask.domain.ai.model.KnowledgeSourceType;
import me.jianwen.mediask.domain.ai.port.KnowledgeDocumentStoragePort;
import me.jianwen.mediask.infra.ai.config.KnowledgeDocumentStorageProperties;

public class LocalKnowledgeDocumentStorageAdapter implements KnowledgeDocumentStoragePort {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final Path baseDir;

    public LocalKnowledgeDocumentStorageAdapter(KnowledgeDocumentStorageProperties properties) {
        this.baseDir = ArgumentChecks.requireNonNull(
                        properties.local().baseDir(), "mediask.ai.knowledge-storage.local.base-dir")
                .toAbsolutePath()
                .normalize();
    }

    @Override
    public String store(Long knowledgeBaseId, String originalFilename, KnowledgeSourceType sourceType, byte[] fileContent) {
        String objectKey = objectKeyOf(knowledgeBaseId, sourceType, originalFilename);
        Path targetPath = baseDir.resolve(objectKey);
        try {
            Files.createDirectories(targetPath.getParent());
            Files.write(targetPath, fileContent);
        } catch (IOException exception) {
            throw new IllegalStateException("failed to store knowledge document file", exception);
        }
        return targetPath.toAbsolutePath().normalize().toUri().toString();
    }

    private String objectKeyOf(Long knowledgeBaseId, KnowledgeSourceType sourceType, String originalFilename) {
        String extension = extensionOf(originalFilename);
        return "knowledge-documents/%d/%s/%s-%s.%s"
                .formatted(
                        knowledgeBaseId,
                        LocalDate.now().format(DATE_FORMATTER),
                        sourceType.name().toLowerCase(java.util.Locale.ROOT),
                        UUID.randomUUID(),
                        extension);
    }

    private String extensionOf(String originalFilename) {
        int extensionIndex = originalFilename.lastIndexOf('.');
        if (extensionIndex < 0 || extensionIndex == originalFilename.length() - 1) {
            throw new IllegalArgumentException("originalFilename must contain extension");
        }
        return originalFilename.substring(extensionIndex + 1).toLowerCase(java.util.Locale.ROOT);
    }
}
