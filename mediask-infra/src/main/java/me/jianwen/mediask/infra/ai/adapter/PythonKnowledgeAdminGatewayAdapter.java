package me.jianwen.mediask.infra.ai.adapter;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.LinkedHashMap;
import java.util.Map;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.exception.ErrorCode;
import me.jianwen.mediask.common.exception.SysException;
import me.jianwen.mediask.common.request.RequestConstants;
import me.jianwen.mediask.domain.ai.model.CreateKnowledgeBasePayload;
import me.jianwen.mediask.domain.ai.model.KnowledgeAdminFile;
import me.jianwen.mediask.domain.ai.model.KnowledgeAdminGatewayContext;
import me.jianwen.mediask.domain.ai.model.KnowledgeBaseListQuery;
import me.jianwen.mediask.domain.ai.model.KnowledgeDocumentListQuery;
import me.jianwen.mediask.domain.ai.model.PublishKnowledgeReleasePayload;
import me.jianwen.mediask.domain.ai.model.UpdateKnowledgeBasePayload;
import me.jianwen.mediask.domain.ai.port.KnowledgeAdminGatewayPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class PythonKnowledgeAdminGatewayAdapter implements KnowledgeAdminGatewayPort {

    private static final Logger log = LoggerFactory.getLogger(PythonKnowledgeAdminGatewayAdapter.class);
    private static final String ACTOR_ID_HEADER = "X-Actor-Id";
    private static final String HOSPITAL_SCOPE_HEADER = "X-Hospital-Scope";
    private static final int ERROR_BODY_LOG_LIMIT = 512;

    private final String baseUrl;
    private final RestClient restClient;

    @Autowired
    public PythonKnowledgeAdminGatewayAdapter(
            @Value("${mediask.ai.base-url:}") String baseUrl,
            RestClient.Builder restClientBuilder) {
        this(baseUrl, restClientBuilder
                .requestFactory(new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_1_1)
                        .build()))
                .build());
    }

    PythonKnowledgeAdminGatewayAdapter(String baseUrl, RestClient restClient) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
        this.restClient = restClient;
    }

    @Override
    public Object listKnowledgeBases(KnowledgeAdminGatewayContext context, KnowledgeBaseListQuery query) {
        return exchangeJson(
                context,
                operation("knowledgeBase.list", HttpMethod.GET, "/api/v1/admin/knowledge-bases", null),
                knowledgeBaseListQueryParams(query),
                null);
    }

    @Override
    public Object createKnowledgeBase(KnowledgeAdminGatewayContext context, CreateKnowledgeBasePayload payload) {
        return exchangeJson(
                context,
                operation("knowledgeBase.create", HttpMethod.POST, "/api/v1/admin/knowledge-bases", payload.code()),
                Map.of(),
                createKnowledgeBaseBody(payload));
    }

    @Override
    public Object updateKnowledgeBase(
            KnowledgeAdminGatewayContext context,
            String knowledgeBaseId,
            UpdateKnowledgeBasePayload payload) {
        return exchangeJson(
                context,
                operation(
                        "knowledgeBase.update",
                        HttpMethod.PATCH,
                        "/api/v1/admin/knowledge-bases/" + knowledgeBaseId,
                        knowledgeBaseId),
                Map.of(),
                updateKnowledgeBaseBody(payload));
    }

    @Override
    public void deleteKnowledgeBase(KnowledgeAdminGatewayContext context, String knowledgeBaseId) {
        exchangeJson(
                context,
                operation(
                        "knowledgeBase.delete",
                        HttpMethod.DELETE,
                        "/api/v1/admin/knowledge-bases/" + knowledgeBaseId,
                        knowledgeBaseId),
                Map.of(),
                null);
    }

    @Override
    public Object importKnowledgeDocument(
            KnowledgeAdminGatewayContext context,
            String knowledgeBaseId,
            KnowledgeAdminFile file) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("knowledge_base_id", knowledgeBaseId);
        form.add("file", filePart(file));
        return exchangeMultipart(
                context,
                operation(
                        "knowledgeDocument.import",
                        HttpMethod.POST,
                        "/api/v1/admin/knowledge-documents/import",
                        knowledgeBaseId),
                form);
    }

    @Override
    public Object listKnowledgeDocuments(KnowledgeAdminGatewayContext context, KnowledgeDocumentListQuery query) {
        return exchangeJson(
                context,
                operation(
                        "knowledgeDocument.list",
                        HttpMethod.GET,
                        "/api/v1/admin/knowledge-documents",
                        query.knowledgeBaseId()),
                knowledgeDocumentListQueryParams(query),
                null);
    }

    @Override
    public void deleteKnowledgeDocument(KnowledgeAdminGatewayContext context, String documentId) {
        exchangeJson(
                context,
                operation(
                        "knowledgeDocument.delete",
                        HttpMethod.DELETE,
                        "/api/v1/admin/knowledge-documents/" + documentId,
                        documentId),
                Map.of(),
                null);
    }

    @Override
    public Object getIngestJob(KnowledgeAdminGatewayContext context, String jobId) {
        return exchangeJson(
                context,
                operation("ingestJob.get", HttpMethod.GET, "/api/v1/admin/ingest-jobs/" + jobId, jobId),
                Map.of(),
                null);
    }

    @Override
    public Object listKnowledgeIndexVersions(KnowledgeAdminGatewayContext context, String knowledgeBaseId) {
        return exchangeJson(
                context,
                operation(
                        "knowledgeIndexVersion.list",
                        HttpMethod.GET,
                        "/api/v1/admin/knowledge-index-versions",
                        knowledgeBaseId),
                knowledgeBaseIdQueryParams(knowledgeBaseId),
                null);
    }

    @Override
    public Object listKnowledgeReleases(KnowledgeAdminGatewayContext context, String knowledgeBaseId) {
        return exchangeJson(
                context,
                operation("knowledgeRelease.list", HttpMethod.GET, "/api/v1/admin/knowledge-releases", knowledgeBaseId),
                knowledgeBaseIdQueryParams(knowledgeBaseId),
                null);
    }

    @Override
    public Object publishKnowledgeRelease(
            KnowledgeAdminGatewayContext context,
            PublishKnowledgeReleasePayload payload) {
        return exchangeJson(
                context,
                operation(
                        "knowledgeRelease.publish",
                        HttpMethod.POST,
                        "/api/v1/admin/knowledge-releases",
                        payload.knowledgeBaseId()),
                Map.of(),
                publishKnowledgeReleaseBody(payload));
    }

    private Object exchangeJson(
            KnowledgeAdminGatewayContext context,
            Operation operation,
            Map<String, String> queryParams,
            Object body) {
        long startedAtNanos = System.nanoTime();
        logPythonRequestStarted(context, operation);
        try {
            RestClient.RequestBodySpec requestSpec = restClient.method(operation.method())
                    .uri(uri(operation.path(), queryParams))
                    .headers(headers -> applyGatewayHeaders(headers, context));
            if (operation.method() == HttpMethod.DELETE) {
                requestSpec.retrieve().toBodilessEntity();
                logPythonRequestSucceeded(context, operation, startedAtNanos);
                return null;
            }
            if (body == null) {
                Object response = requestSpec.retrieve().body(Object.class);
                logPythonRequestSucceeded(context, operation, startedAtNanos);
                return response;
            }
            // log.info("Python knowledge admin request body, operation={}, body={}", operation.name(), body);
            Object response = requestSpec
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Object.class);
            logPythonRequestSucceeded(context, operation, startedAtNanos);
            return response;
        } catch (RestClientResponseException exception) {
            logPythonResponseException(context, operation, startedAtNanos, exception);
            throw mapPythonResponseException(operation, exception);
        } catch (ResourceAccessException exception) {
            logPythonRequestFailed(context, operation, startedAtNanos, exception);
            throw new SysException(ErrorCode.AI_SERVICE_UNAVAILABLE, "Python knowledge admin API unavailable", exception);
        } catch (RestClientException exception) {
            logPythonRequestFailed(context, operation, startedAtNanos, exception);
            throw new SysException(ErrorCode.AI_RESPONSE_INVALID, "Python knowledge admin API response invalid", exception);
        }
    }

    private Object exchangeMultipart(
            KnowledgeAdminGatewayContext context,
            Operation operation,
            MultiValueMap<String, Object> form) {
        long startedAtNanos = System.nanoTime();
        logPythonRequestStarted(context, operation);
        try {
            Object response = restClient.post()
                    .uri(uri(operation.path(), Map.of()))
                    .headers(headers -> applyGatewayHeaders(headers, context))
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(form)
                    .retrieve()
                    .body(Object.class);
            logPythonRequestSucceeded(context, operation, startedAtNanos);
            return response;
        } catch (RestClientResponseException exception) {
            logPythonResponseException(context, operation, startedAtNanos, exception);
            throw mapPythonResponseException(operation, exception);
        } catch (ResourceAccessException exception) {
            logPythonRequestFailed(context, operation, startedAtNanos, exception);
            throw new SysException(ErrorCode.AI_SERVICE_UNAVAILABLE, "Python knowledge admin API unavailable", exception);
        } catch (RestClientException exception) {
            logPythonRequestFailed(context, operation, startedAtNanos, exception);
            throw new SysException(ErrorCode.AI_RESPONSE_INVALID, "Python knowledge admin API response invalid", exception);
        }
    }

    private URI uri(String path, Map<String, String> queryParams) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(requireBaseUrl()).path(path);
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            builder.queryParam(entry.getKey(), entry.getValue());
        }
        return builder.build().toUri();
    }

    private RuntimeException mapPythonResponseException(Operation operation, RestClientResponseException exception) {
        int statusCode = exception.getStatusCode().value();
        String message = "Python knowledge admin API " + operation.name() + " returned HTTP " + statusCode;
        if (exception.getStatusCode().is4xxClientError()) {
            if (statusCode == 404) {
                return new BizException(ErrorCode.RESOURCE_NOT_FOUND, message, exception);
            }
            return new BizException(ErrorCode.INVALID_PARAMETER, message, exception);
        }
        return new SysException(ErrorCode.AI_SERVICE_UNAVAILABLE, message, exception);
    }

    private String requireBaseUrl() {
        if (baseUrl.isBlank()) {
            throw new SysException(ErrorCode.AI_SERVICE_UNAVAILABLE, "mediask.ai.base-url is required");
        }
        return baseUrl;
    }

    private void applyGatewayHeaders(HttpHeaders headers, KnowledgeAdminGatewayContext context) {
        headers.set(RequestConstants.REQUEST_ID_HEADER, context.requestId());
        headers.set(ACTOR_ID_HEADER, String.valueOf(context.actorId()));
        headers.set(HOSPITAL_SCOPE_HEADER, context.hospitalScope());
    }

    private Operation operation(String name, HttpMethod method, String path, String resourceId) {
        return new Operation(name, method, path, resourceId);
    }

    private Map<String, String> knowledgeBaseListQueryParams(KnowledgeBaseListQuery query) {
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        putQueryParam(params, "keyword", query.keyword());
        putQueryParam(params, "page_num", query.pageNum());
        putQueryParam(params, "page_size", query.pageSize());
        return params;
    }

    private Map<String, String> knowledgeDocumentListQueryParams(KnowledgeDocumentListQuery query) {
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        putQueryParam(params, "knowledge_base_id", query.knowledgeBaseId());
        putQueryParam(params, "page_num", query.pageNum());
        putQueryParam(params, "page_size", query.pageSize());
        return params;
    }

    private Map<String, String> knowledgeBaseIdQueryParams(String knowledgeBaseId) {
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        putQueryParam(params, "knowledge_base_id", knowledgeBaseId);
        return params;
    }

    private Map<String, Object> createKnowledgeBaseBody(CreateKnowledgeBasePayload payload) {
        LinkedHashMap<String, Object> body = new LinkedHashMap<>();
        putBodyField(body, "code", payload.code());
        putBodyField(body, "name", payload.name());
        putBodyField(body, "description", payload.description());
        putBodyField(body, "default_embedding_model", payload.defaultEmbeddingModel());
        putBodyField(body, "default_embedding_dimension", payload.defaultEmbeddingDimension());
        putBodyField(body, "retrieval_strategy", payload.retrievalStrategy());
        return body;
    }

    private Map<String, Object> updateKnowledgeBaseBody(UpdateKnowledgeBasePayload payload) {
        LinkedHashMap<String, Object> body = new LinkedHashMap<>();
        putBodyField(body, "name", payload.name());
        putBodyField(body, "description", payload.description());
        putBodyField(body, "status", payload.status());
        return body;
    }

    private Map<String, Object> publishKnowledgeReleaseBody(PublishKnowledgeReleasePayload payload) {
        LinkedHashMap<String, Object> body = new LinkedHashMap<>();
        putBodyField(body, "knowledge_base_id", payload.knowledgeBaseId());
        putBodyField(body, "target_index_version_id", payload.targetIndexVersionId());
        return body;
    }

    private void putQueryParam(Map<String, String> params, String name, Object value) {
        if (value != null) {
            params.put(name, String.valueOf(value));
        }
    }

    private void putBodyField(Map<String, Object> body, String name, Object value) {
        if (value != null) {
            body.put(name, value);
        }
    }

    private void logPythonRequestStarted(KnowledgeAdminGatewayContext context, Operation operation) {
        log.info(
                "Python knowledge admin request started, operation={}, method={}, path={}, requestId={}, actorId={}, hospitalScope={}, resourceId={}",
                operation.name(),
                operation.method(),
                operation.path(),
                context.requestId(),
                context.actorId(),
                context.hospitalScope(),
                operation.resourceId());
    }

    private void logPythonRequestSucceeded(
            KnowledgeAdminGatewayContext context,
            Operation operation,
            long startedAtNanos) {
        log.info(
                "Python knowledge admin request succeeded, operation={}, requestId={}, durationMs={}, resourceId={}",
                operation.name(),
                context.requestId(),
                durationMs(startedAtNanos),
                operation.resourceId());
    }

    private void logPythonResponseException(
            KnowledgeAdminGatewayContext context,
            Operation operation,
            long startedAtNanos,
            RestClientResponseException exception) {
        int statusCode = exception.getStatusCode().value();
        if (exception.getStatusCode().is4xxClientError()) {
            log.warn(
                    "Python knowledge admin request rejected, operation={}, status={}, requestId={}, durationMs={}, resourceId={}, responseBody={}",
                    operation.name(),
                    statusCode,
                    context.requestId(),
                    durationMs(startedAtNanos),
                    operation.resourceId(),
                    responseBodySnippet(exception));
            return;
        }
        log.error(
                "Python knowledge admin request failed, operation={}, status={}, requestId={}, durationMs={}, resourceId={}, responseBody={}",
                operation.name(),
                statusCode,
                context.requestId(),
                durationMs(startedAtNanos),
                operation.resourceId(),
                responseBodySnippet(exception),
                exception);
    }

    private void logPythonRequestFailed(
            KnowledgeAdminGatewayContext context,
            Operation operation,
            long startedAtNanos,
            RestClientException exception) {
        log.error(
                "Python knowledge admin request failed, operation={}, requestId={}, durationMs={}, resourceId={}",
                operation.name(),
                context.requestId(),
                durationMs(startedAtNanos),
                operation.resourceId(),
                exception);
    }

    private long durationMs(long startedAtNanos) {
        return (System.nanoTime() - startedAtNanos) / 1_000_000;
    }

    private String responseBodySnippet(RestClientResponseException exception) {
        String body = exception.getResponseBodyAsString();
        if (body == null || body.length() <= ERROR_BODY_LOG_LIMIT) {
            return body;
        }
        return body.substring(0, ERROR_BODY_LOG_LIMIT);
    }

    private HttpEntity<ByteArrayResource> filePart(KnowledgeAdminFile file) {
        ByteArrayResource resource = new ByteArrayResource(file.content()) {
            @Override
            public String getFilename() {
                return file.filename();
            }
        };
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(resolveContentType(file.contentType()));
        return new HttpEntity<>(resource, headers);
    }

    private MediaType resolveContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        return MediaType.parseMediaType(contentType);
    }

    private record Operation(String name, HttpMethod method, String path, String resourceId) {
    }
}
