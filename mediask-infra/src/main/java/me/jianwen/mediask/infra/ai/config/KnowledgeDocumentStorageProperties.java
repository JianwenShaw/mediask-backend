package me.jianwen.mediask.infra.ai.config;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "mediask.ai.knowledge-storage")
public record KnowledgeDocumentStorageProperties(
        @DefaultValue("LOCAL") KnowledgeDocumentStorageMode mode,
        Local local,
        Oss oss) {

    public KnowledgeDocumentStorageProperties {
        mode = mode == null ? KnowledgeDocumentStorageMode.LOCAL : mode;
        local = local == null ? new Local(null) : local;
        oss = oss == null ? new Oss(null, null) : oss;
    }

    public record Local(Path baseDir) {}

    public record Oss(String bucket, String keyPrefix) {}
}
