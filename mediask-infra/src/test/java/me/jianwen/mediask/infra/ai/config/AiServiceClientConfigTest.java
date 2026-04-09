package me.jianwen.mediask.infra.ai.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Field;
import java.time.Duration;
import me.jianwen.mediask.infra.ai.client.PythonAiChatClient;
import me.jianwen.mediask.infra.ai.client.PythonAiChatStreamClient;
import me.jianwen.mediask.infra.ai.client.support.AiServiceErrorDecoder;
import me.jianwen.mediask.infra.ai.client.support.AiServiceSseEventReader;
import me.jianwen.mediask.infra.ai.client.support.ApiKeyAuthenticationInterceptor;
import me.jianwen.mediask.infra.observability.RequestContextPropagationInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.web.client.RestClient;

class AiServiceClientConfigTest {

    private final AiServiceClientConfig config = new AiServiceClientConfig();

    @Test
    void aiServiceClientBeans_ShouldUseDedicatedStreamRestClient() {
        AiServiceProperties properties = new AiServiceProperties(
                java.net.URI.create("http://localhost:8000"),
                "test-key",
                Duration.ofSeconds(3),
                Duration.ofSeconds(30),
                Duration.ofMinutes(5));
        RequestContextPropagationInterceptor requestInterceptor = config.requestContextPropagationInterceptor();
        ApiKeyAuthenticationInterceptor apiKeyInterceptor = config.apiKeyAuthenticationInterceptor(properties);
        AiServiceErrorDecoder errorDecoder = config.aiServiceErrorDecoder(objectMapperProvider());

        RestClient normalClient = config.aiServiceRestClient(
                RestClient.builder(), properties, requestInterceptor, apiKeyInterceptor, errorDecoder);
        RestClient streamClient = config.aiServiceStreamRestClient(
                RestClient.builder(), properties, requestInterceptor, apiKeyInterceptor, errorDecoder);
        PythonAiChatClient pythonAiChatClient = config.pythonAiChatClient(normalClient);
        PythonAiChatStreamClient pythonAiChatStreamClient = config.pythonAiChatStreamClient(
                streamClient, errorDecoder, config.aiServiceSseEventReader(), objectMapperProvider());

        assertSame(normalClient, readRestClient(pythonAiChatClient));
        assertSame(streamClient, readRestClient(pythonAiChatStreamClient));
        assertNotSame(normalClient, streamClient);
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

    private org.springframework.beans.factory.ObjectProvider<ObjectMapper> objectMapperProvider() {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("objectMapper", new ObjectMapper());
        return beanFactory.getBeanProvider(ObjectMapper.class);
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
