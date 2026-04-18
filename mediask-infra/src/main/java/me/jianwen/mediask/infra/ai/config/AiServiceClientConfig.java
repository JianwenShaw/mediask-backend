package me.jianwen.mediask.infra.ai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import me.jianwen.mediask.domain.ai.port.AiChatPort;
import me.jianwen.mediask.domain.ai.port.AiContentEncryptorPort;
import me.jianwen.mediask.domain.ai.port.KnowledgeIndexPort;
import me.jianwen.mediask.domain.ai.port.KnowledgePreparePort;
import me.jianwen.mediask.domain.ai.port.KnowledgeDocumentStoragePort;
import me.jianwen.mediask.infra.ai.adapter.PythonAiChatPortAdapter;
import me.jianwen.mediask.infra.ai.adapter.LocalKnowledgeDocumentStorageAdapter;
import me.jianwen.mediask.infra.ai.adapter.OssKnowledgeDocumentStorageAdapter;
import me.jianwen.mediask.infra.ai.adapter.PythonKnowledgePortAdapter;
import me.jianwen.mediask.infra.ai.adapter.AesGcmAiContentEncryptor;
import me.jianwen.mediask.infra.ai.client.PythonAiChatClient;
import me.jianwen.mediask.infra.ai.client.PythonKnowledgeClient;
import me.jianwen.mediask.infra.ai.client.mapper.PythonAiChatMapper;
import me.jianwen.mediask.infra.ai.client.support.AiServiceErrorDecoder;
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
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({AiServiceProperties.class, KnowledgeDocumentStorageProperties.class, AiEncryptionProperties.class})
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
    public ObjectMapper aiServiceObjectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    @Bean
    public AiServiceErrorDecoder aiServiceErrorDecoder(ObjectMapper aiServiceObjectMapper) {
        return new AiServiceErrorDecoder(aiServiceObjectMapper.copy());
    }

    @Bean(name = "aiServiceRestClient")
    public RestClient aiServiceRestClient(
            RestClient.Builder builder,
            AiServiceProperties properties,
            RequestContextPropagationInterceptor requestContextPropagationInterceptor,
            ApiKeyAuthenticationInterceptor apiKeyAuthenticationInterceptor,
            AiServiceErrorDecoder aiServiceErrorDecoder,
            ObjectMapper aiServiceObjectMapper) {
        return builder.baseUrl(properties.baseUrl().toString())
                .requestFactory(aiServiceRequestFactory(properties.connectTimeout(), properties.readTimeout()))
                .requestInterceptor(requestContextPropagationInterceptor)
                .requestInterceptor(apiKeyAuthenticationInterceptor)
                .messageConverters(converters -> converters.stream()
                        .filter(MappingJackson2HttpMessageConverter.class::isInstance)
                        .map(MappingJackson2HttpMessageConverter.class::cast)
                        .forEach(converter -> converter.setObjectMapper(aiServiceObjectMapper.copy())))
                .defaultStatusHandler(aiServiceResponseErrorHandler(aiServiceErrorDecoder))
                .build();
    }

    @Bean
    public PythonAiChatMapper pythonAiChatMapper() {
        return new PythonAiChatMapper();
    }

    @Bean
    public AiContentEncryptorPort aiContentEncryptorPort(AiEncryptionProperties aiEncryptionProperties) {
        return new AesGcmAiContentEncryptor(Base64.getDecoder().decode(aiEncryptionProperties.key()));
    }

    @Bean
    public PythonAiChatClient pythonAiChatClient(
            @Qualifier("aiServiceRestClient") RestClient aiServiceRestClient) {
        return new PythonAiChatClient(aiServiceRestClient);
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
    public KnowledgeDocumentStoragePort knowledgeDocumentStoragePort(
            KnowledgeDocumentStorageProperties knowledgeDocumentStorageProperties) {
        return switch (knowledgeDocumentStorageProperties.mode()) {
            case LOCAL -> new LocalKnowledgeDocumentStorageAdapter(knowledgeDocumentStorageProperties);
            case OSS -> new OssKnowledgeDocumentStorageAdapter(knowledgeDocumentStorageProperties.oss());
        };
    }

    @Bean
    public AiChatPort aiChatPort(PythonAiChatClient pythonAiChatClient, PythonAiChatMapper pythonAiChatMapper) {
        return new PythonAiChatPortAdapter(pythonAiChatClient, pythonAiChatMapper);
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
