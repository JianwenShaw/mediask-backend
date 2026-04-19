package me.jianwen.mediask.infra.ai.config;

import java.util.Base64;
import me.jianwen.mediask.domain.ai.port.AiContentEncryptorPort;
import me.jianwen.mediask.infra.ai.adapter.AesGcmAiContentEncryptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AiEncryptionProperties.class)
public class AiContentEncryptionConfig {

    @Bean
    public AiContentEncryptorPort aiContentEncryptorPort(AiEncryptionProperties aiEncryptionProperties) {
        return new AesGcmAiContentEncryptor(Base64.getDecoder().decode(aiEncryptionProperties.key()));
    }
}
