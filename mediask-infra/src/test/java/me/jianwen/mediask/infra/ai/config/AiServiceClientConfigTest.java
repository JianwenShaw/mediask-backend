package me.jianwen.mediask.infra.ai.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.lang.reflect.Field;
import java.time.Duration;
import me.jianwen.mediask.infra.ai.client.PythonAiChatClient;
import me.jianwen.mediask.infra.ai.client.support.AiServiceErrorDecoder;
import me.jianwen.mediask.infra.ai.client.support.ApiKeyAuthenticationInterceptor;
import me.jianwen.mediask.infra.observability.RequestContextPropagationInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class AiServiceClientConfigTest {

    private final AiServiceClientConfig config = new AiServiceClientConfig();

    @Test
    void aiServiceClientBeans_ShouldUseConfiguredRestClient() {
        AiServiceProperties properties = new AiServiceProperties(
                java.net.URI.create("http://localhost:8000"),
                "test-key",
                Duration.ofSeconds(3),
                Duration.ofSeconds(30),
                Duration.ofMinutes(5));
        RequestContextPropagationInterceptor requestInterceptor = config.requestContextPropagationInterceptor();
        ApiKeyAuthenticationInterceptor apiKeyInterceptor = config.apiKeyAuthenticationInterceptor(properties);
        ObjectMapper aiServiceObjectMapper = config.aiServiceObjectMapper();
        AiServiceErrorDecoder errorDecoder = config.aiServiceErrorDecoder(aiServiceObjectMapper);

        RestClient normalClient = config.aiServiceRestClient(
                RestClient.builder(), properties, requestInterceptor, apiKeyInterceptor, errorDecoder, aiServiceObjectMapper);
        PythonAiChatClient pythonAiChatClient = config.pythonAiChatClient(normalClient);

        assertSame(normalClient, readRestClient(pythonAiChatClient));
    }

    @Test
    void aiServiceObjectMapper_ShouldSerializeLongAsNumber() throws Exception {
        ObjectMapper objectMapper = config.aiServiceObjectMapper();
        ObjectNode node = objectMapper.createObjectNode();
        node.putPOJO("value", 123L);

        assertEquals("{\"value\":123}", objectMapper.writeValueAsString(node));
    }

    @Test
    void aiServiceProperties_WhenStreamReadTimeoutConfigured_ShouldExposeValue() {
        AiServiceProperties properties = new AiServiceProperties(
                java.net.URI.create("http://localhost:8000"),
                "test-key",
                Duration.ofSeconds(3),
                Duration.ofSeconds(30),
                Duration.ofMinutes(5));

        assertEquals(Duration.ofMinutes(5), properties.streamReadTimeout());
    }

    @Test
    void aiServiceProperties_WhenStreamReadTimeoutInvalid_ShouldThrowException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AiServiceProperties(
                        java.net.URI.create("http://localhost:8000"),
                        "test-key",
                        Duration.ofSeconds(3),
                        Duration.ofSeconds(30),
                        Duration.ZERO));
    }

    private RestClient readRestClient(Object target) {
        try {
            Field field = target.getClass().getDeclaredField("aiServiceRestClient");
            field.setAccessible(true);
            return (RestClient) field.get(target);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
