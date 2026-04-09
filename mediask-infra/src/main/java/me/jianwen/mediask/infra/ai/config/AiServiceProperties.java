package me.jianwen.mediask.infra.ai.config;

import java.net.URI;
import java.time.Duration;
import me.jianwen.mediask.common.util.ArgumentChecks;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "mediask.ai.service")
public record AiServiceProperties(
        URI baseUrl,
        String apiKey,
        @DefaultValue("PT3S") Duration connectTimeout,
        @DefaultValue("PT30S") Duration readTimeout,
        @DefaultValue("PT5M") Duration streamReadTimeout) {

    public AiServiceProperties {
        baseUrl = requireValue(baseUrl, "mediask.ai.service.base-url");
        apiKey = ArgumentChecks.requireNonBlank(apiKey, "mediask.ai.service.api-key");
        connectTimeout = requirePositive(connectTimeout, "mediask.ai.service.connect-timeout");
        readTimeout = requirePositive(readTimeout, "mediask.ai.service.read-timeout");
        streamReadTimeout = requirePositive(streamReadTimeout, "mediask.ai.service.stream-read-timeout");
    }

    private static <T> T requireValue(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
        return value;
    }

    private static Duration requirePositive(Duration value, String fieldName) {
        if (value == null || value.isNegative() || value.isZero()) {
            throw new IllegalArgumentException(fieldName + " must be greater than 0");
        }
        return value;
    }
}
