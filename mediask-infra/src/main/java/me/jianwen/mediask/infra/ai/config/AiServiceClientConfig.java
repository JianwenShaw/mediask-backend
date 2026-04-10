package me.jianwen.mediask.infra.ai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.jianwen.mediask.domain.ai.port.AiChatPort;
import me.jianwen.mediask.domain.ai.port.AiChatStreamPort;
import me.jianwen.mediask.domain.ai.port.KnowledgeIndexPort;
import me.jianwen.mediask.domain.ai.port.KnowledgePreparePort;
import me.jianwen.mediask.infra.ai.adapter.PythonAiChatPortAdapter;
import me.jianwen.mediask.infra.ai.adapter.PythonAiChatStreamPortAdapter;
import me.jianwen.mediask.infra.ai.adapter.PythonKnowledgePortAdapter;
import me.jianwen.mediask.infra.ai.client.PythonAiChatClient;
import me.jianwen.mediask.infra.ai.client.PythonAiChatStreamClient;
import me.jianwen.mediask.infra.ai.client.PythonKnowledgeClient;
import me.jianwen.mediask.infra.ai.client.mapper.PythonAiChatMapper;
import me.jianwen.mediask.infra.ai.client.support.AiServiceErrorDecoder;
import me.jianwen.mediask.infra.ai.client.support.AiServiceSseEventReader;
import me.jianwen.mediask.infra.ai.client.support.ApiKeyAuthenticationInterceptor;
import me.jianwen.mediask.infra.observability.RequestContextPropagationInterceptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(AiServiceProperties.class)
@ConditionalOnProperty(prefix = "mediask.ai.service", name = {"base-url", "api-key"})
public class AiServiceClientConfig {

    @Bean
    public RequestContextPropagationInterceptor requestContextPropagationInterceptor() {
        return new RequestContextPropagationInterceptor();
    }

    @Bean
    public ApiKeyAuthenticationInterceptor apiKeyAuthenticationInterceptor(AiServiceProperties properties) {
        return new ApiKeyAuthenticationInterceptor(properties);
    }

    @Bean
    public AiServiceErrorDecoder aiServiceErrorDecoder(ObjectProvider<ObjectMapper> objectMapperProvider) {
        ObjectMapper objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new).copy().findAndRegisterModules();
        return new AiServiceErrorDecoder(objectMapper);
    }

    @Bean
    public AiServiceSseEventReader aiServiceSseEventReader() {
        return new AiServiceSseEventReader();
    }

    @Bean(name = "aiServiceRestClient")
    public RestClient aiServiceRestClient(
            RestClient.Builder builder,
            AiServiceProperties properties,
            RequestContextPropagationInterceptor requestContextPropagationInterceptor,
            ApiKeyAuthenticationInterceptor apiKeyAuthenticationInterceptor,
            AiServiceErrorDecoder aiServiceErrorDecoder) {
        return builder.baseUrl(properties.baseUrl().toString())
                .requestFactory(aiServiceRequestFactory(properties.connectTimeout(), properties.readTimeout()))
                .requestInterceptor(requestContextPropagationInterceptor)
                .requestInterceptor(apiKeyAuthenticationInterceptor)
                .defaultStatusHandler(aiServiceResponseErrorHandler(aiServiceErrorDecoder))
                .build();
    }

    @Bean(name = "aiServiceStreamRestClient")
    public RestClient aiServiceStreamRestClient(
            RestClient.Builder builder,
            AiServiceProperties properties,
            RequestContextPropagationInterceptor requestContextPropagationInterceptor,
            ApiKeyAuthenticationInterceptor apiKeyAuthenticationInterceptor,
            AiServiceErrorDecoder aiServiceErrorDecoder) {
        return builder.baseUrl(properties.baseUrl().toString())
                .requestFactory(aiServiceRequestFactory(properties.connectTimeout(), properties.streamReadTimeout()))
                .requestInterceptor(requestContextPropagationInterceptor)
                .requestInterceptor(apiKeyAuthenticationInterceptor)
                .defaultStatusHandler(aiServiceResponseErrorHandler(aiServiceErrorDecoder))
                .build();
    }

    @Bean
    public PythonAiChatMapper pythonAiChatMapper() {
        return new PythonAiChatMapper();
    }

    @Bean
    public PythonAiChatClient pythonAiChatClient(
            @Qualifier("aiServiceRestClient") RestClient aiServiceRestClient) {
        return new PythonAiChatClient(aiServiceRestClient);
    }

    @Bean
    public PythonAiChatStreamClient pythonAiChatStreamClient(
            @Qualifier("aiServiceStreamRestClient") RestClient aiServiceRestClient,
            AiServiceErrorDecoder aiServiceErrorDecoder,
            AiServiceSseEventReader aiServiceSseEventReader,
            ObjectProvider<ObjectMapper> objectMapperProvider) {
        ObjectMapper objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new).copy().findAndRegisterModules();
        return new PythonAiChatStreamClient(
                aiServiceRestClient, aiServiceErrorDecoder, aiServiceSseEventReader, objectMapper);
    }

    @Bean
    public PythonKnowledgeClient pythonKnowledgeClient(
            @Qualifier("aiServiceRestClient") RestClient aiServiceRestClient) {
        return new PythonKnowledgeClient(aiServiceRestClient);
    }

    @Bean
    public PythonKnowledgePortAdapter pythonKnowledgePortAdapter(PythonKnowledgeClient pythonKnowledgeClient) {
        return new PythonKnowledgePortAdapter(pythonKnowledgeClient);
    }

    @Bean
    public AiChatPort aiChatPort(PythonAiChatClient pythonAiChatClient, PythonAiChatMapper pythonAiChatMapper) {
        return new PythonAiChatPortAdapter(pythonAiChatClient, pythonAiChatMapper);
    }

    @Bean
    public AiChatStreamPort aiChatStreamPort(
            PythonAiChatStreamClient pythonAiChatStreamClient, PythonAiChatMapper pythonAiChatMapper) {
        return new PythonAiChatStreamPortAdapter(pythonAiChatStreamClient, pythonAiChatMapper);
    }

    @Bean
    public KnowledgePreparePort knowledgePreparePort(PythonKnowledgePortAdapter pythonKnowledgePortAdapter) {
        return pythonKnowledgePortAdapter;
    }

    @Bean
    public KnowledgeIndexPort knowledgeIndexPort(PythonKnowledgePortAdapter pythonKnowledgePortAdapter) {
        return pythonKnowledgePortAdapter;
    }

    private ClientHttpRequestFactory aiServiceRequestFactory(
            java.time.Duration connectTimeout, java.time.Duration readTimeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Math.toIntExact(connectTimeout.toMillis()));
        requestFactory.setReadTimeout(Math.toIntExact(readTimeout.toMillis()));
        return requestFactory;
    }

    private ResponseErrorHandler aiServiceResponseErrorHandler(AiServiceErrorDecoder aiServiceErrorDecoder) {
        return new ResponseErrorHandler() {
            @Override
            public boolean hasError(org.springframework.http.client.ClientHttpResponse response)
                    throws java.io.IOException {
                return response.getStatusCode().isError();
            }

            @Override
            public void handleError(
                    java.net.URI url,
                    org.springframework.http.HttpMethod method,
                    org.springframework.http.client.ClientHttpResponse response)
                    throws java.io.IOException {
                throw aiServiceErrorDecoder.decode(
                        response.getStatusCode(), response.getHeaders(), response.getBody().readAllBytes());
            }
        };
    }
}
