package me.jianwen.mediask.infra.ai.config;

import me.jianwen.mediask.common.util.ArgumentChecks;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mediask.encryption")
public record AiEncryptionProperties(String key) {

    public AiEncryptionProperties {
        key = ArgumentChecks.requireNonBlank(key, "mediask.encryption.key");
    }
}
