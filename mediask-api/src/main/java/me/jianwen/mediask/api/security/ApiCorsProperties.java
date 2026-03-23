package me.jianwen.mediask.api.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "mediask.cors")
public record ApiCorsProperties(
        List<String> allowedOrigins,
        List<String> allowedMethods,
        List<String> allowedHeaders,
        @DefaultValue("true") boolean allowCredentials,
        @DefaultValue("3600") long maxAge) {

    private static final List<String> DEFAULT_ALLOWED_METHODS =
            List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    private static final List<String> DEFAULT_ALLOWED_HEADERS = List.of("*");

    public ApiCorsProperties {
        allowedOrigins = immutableListOrEmpty(allowedOrigins);
        allowedMethods = immutableListOrDefault(allowedMethods, DEFAULT_ALLOWED_METHODS);
        allowedHeaders = immutableListOrDefault(allowedHeaders, DEFAULT_ALLOWED_HEADERS);
        if (maxAge < 0L) {
            throw new IllegalArgumentException("mediask.cors.max-age must be greater than or equal to 0");
        }
    }

    private static List<String> immutableListOrEmpty(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static List<String> immutableListOrDefault(List<String> values, List<String> defaultValues) {
        return values == null || values.isEmpty() ? defaultValues : List.copyOf(values);
    }
}
