package me.jianwen.mediask.infra.ai.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.exception.SysException;
import me.jianwen.mediask.domain.ai.model.CreateKnowledgeBasePayload;
import me.jianwen.mediask.domain.ai.model.KnowledgeAdminFile;
import me.jianwen.mediask.domain.ai.model.KnowledgeAdminGatewayContext;
import me.jianwen.mediask.domain.ai.model.KnowledgeBaseListQuery;
import me.jianwen.mediask.domain.ai.model.PublishKnowledgeReleasePayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.AbstractClientHttpRequest;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;

class PythonKnowledgeAdminGatewayAdapterTest {

    private CapturingClientHttpRequestFactory requestFactory;
    private PythonKnowledgeAdminGatewayAdapter adapter;

    @BeforeEach
    void setUp() {
        requestFactory = new CapturingClientHttpRequestFactory(200);
        adapter = new PythonKnowledgeAdminGatewayAdapter(
                "http://python.test",
                RestClient.builder().requestFactory(requestFactory).build());
    }

    @Test
    void listKnowledgeBases_ForwardsPathQueryAndGatewayHeaders() {
        Object response = adapter.listKnowledgeBases(context(), new KnowledgeBaseListQuery("triage", 2, 5));

        assertTrue(response instanceof Map<?, ?>);
        assertEquals("GET", requestFactory.capturedRequest.method.name());
        assertEquals(
                "http://python.test/api/v1/admin/knowledge-bases?keyword=triage&page_num=2&page_size=5",
                requestFactory.capturedRequest.uri.toString());
        assertEquals("req-1", requestFactory.capturedRequest.headers.getFirst("X-Request-Id"));
        assertEquals("2001", requestFactory.capturedRequest.headers.getFirst("X-Actor-Id"));
        assertEquals("default", requestFactory.capturedRequest.headers.getFirst("X-Hospital-Scope"));
    }

    @Test
    void importKnowledgeDocument_ForwardsMultipartBody() {
        adapter.importKnowledgeDocument(
                context(),
                "kb-1",
                new KnowledgeAdminFile(
                        "manual.pdf",
                        "application/pdf",
                        "pdf-content".getBytes(StandardCharsets.UTF_8)));

        assertEquals("POST", requestFactory.capturedRequest.method.name());
        assertEquals(
                "http://python.test/api/v1/admin/knowledge-documents/import",
                requestFactory.capturedRequest.uri.toString());
        assertTrue(requestFactory.capturedRequest.body.contains("name=\"knowledge_base_id\""));
        assertTrue(requestFactory.capturedRequest.body.contains("kb-1"));
        assertTrue(requestFactory.capturedRequest.body.contains("filename=\"manual.pdf\""));
        assertTrue(requestFactory.capturedRequest.body.contains("pdf-content"));
    }

    @Test
    void publishKnowledgeRelease_ForwardsJsonBody() {
        adapter.publishKnowledgeRelease(
                context(),
                new PublishKnowledgeReleasePayload("kb-1", "idx-1"));

        assertEquals("POST", requestFactory.capturedRequest.method.name());
        assertEquals(
                "http://python.test/api/v1/admin/knowledge-releases",
                requestFactory.capturedRequest.uri.toString());
        assertEquals("application/json", requestFactory.capturedRequest.headers.getContentType().toString());
        assertTrue(requestFactory.capturedRequest.body.contains("\"knowledge_base_id\":\"kb-1\""));
        assertTrue(requestFactory.capturedRequest.body.contains("\"target_index_version_id\":\"idx-1\""));
    }

    @Test
    void createKnowledgeBase_ForwardsSnakeCaseJsonBody() {
        adapter.createKnowledgeBase(
                context(),
                new CreateKnowledgeBasePayload(
                        "triage-general",
                        "导诊通用知识库",
                        null,
                        "text-embedding-v4",
                        1024,
                        "HYBRID_RRF"));

        assertEquals("POST", requestFactory.capturedRequest.method.name());
        assertEquals(
                "http://python.test/api/v1/admin/knowledge-bases",
                requestFactory.capturedRequest.uri.toString());
        assertTrue(requestFactory.capturedRequest.body.contains("\"default_embedding_model\":\"text-embedding-v4\""));
        assertTrue(requestFactory.capturedRequest.body.contains("\"default_embedding_dimension\":1024"));
        assertTrue(requestFactory.capturedRequest.body.contains("\"retrieval_strategy\":\"HYBRID_RRF\""));
    }

    @Test
    void listKnowledgeBases_WhenPythonReturnsBadRequest_ThrowsBizException() {
        requestFactory = new CapturingClientHttpRequestFactory(400);
        adapter = new PythonKnowledgeAdminGatewayAdapter(
                "http://python.test",
                RestClient.builder().requestFactory(requestFactory).build());

        BizException exception = assertThrows(BizException.class, () ->
                adapter.listKnowledgeBases(context(), new KnowledgeBaseListQuery(null, null, null)));

        assertEquals(1002, exception.getCode());
    }

    @Test
    void listKnowledgeBases_WhenPythonReturnsNotFound_ThrowsBizException() {
        requestFactory = new CapturingClientHttpRequestFactory(404);
        adapter = new PythonKnowledgeAdminGatewayAdapter(
                "http://python.test",
                RestClient.builder().requestFactory(requestFactory).build());

        BizException exception = assertThrows(BizException.class, () ->
                adapter.listKnowledgeBases(context(), new KnowledgeBaseListQuery(null, null, null)));

        assertEquals(1004, exception.getCode());
    }

    @Test
    void listKnowledgeBases_WhenPythonReturnsServerError_ThrowsSysException() {
        requestFactory = new CapturingClientHttpRequestFactory(500);
        adapter = new PythonKnowledgeAdminGatewayAdapter(
                "http://python.test",
                RestClient.builder().requestFactory(requestFactory).build());

        SysException exception = assertThrows(SysException.class, () ->
                adapter.listKnowledgeBases(context(), new KnowledgeBaseListQuery(null, null, null)));

        assertEquals(6001, exception.getCode());
    }

    @Test
    void listKnowledgeBases_WhenBaseUrlMissing_ThrowsSysException() {
        PythonKnowledgeAdminGatewayAdapter missingBaseUrlAdapter =
                new PythonKnowledgeAdminGatewayAdapter("", RestClient.create());

        SysException exception = assertThrows(SysException.class, () ->
                missingBaseUrlAdapter.listKnowledgeBases(context(), new KnowledgeBaseListQuery(null, null, null)));

        assertEquals(6001, exception.getCode());
        assertEquals("mediask.ai.base-url is required", exception.getMessage());
    }

    private KnowledgeAdminGatewayContext context() {
        return new KnowledgeAdminGatewayContext("req-1", 2001L, "default");
    }

    private static class CapturingClientHttpRequestFactory implements ClientHttpRequestFactory {

        private final int responseStatus;
        private CapturedRequest capturedRequest;

        private CapturingClientHttpRequestFactory(int responseStatus) {
            this.responseStatus = responseStatus;
        }

        @Override
        public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
            return new CapturingClientHttpRequest(uri, httpMethod, this);
        }
    }

    private static class CapturingClientHttpRequest extends AbstractClientHttpRequest {

        private final URI uri;
        private final HttpMethod method;
        private final CapturingClientHttpRequestFactory requestFactory;
        private final ByteArrayOutputStream body = new ByteArrayOutputStream();

        private CapturingClientHttpRequest(
                URI uri,
                HttpMethod method,
                CapturingClientHttpRequestFactory requestFactory) {
            this.uri = uri;
            this.method = method;
            this.requestFactory = requestFactory;
        }

        @Override
        public HttpMethod getMethod() {
            return method;
        }

        @Override
        public URI getURI() {
            return uri;
        }

        @Override
        protected OutputStream getBodyInternal(HttpHeaders headers) {
            return body;
        }

        @Override
        protected ClientHttpResponse executeInternal(HttpHeaders headers) {
            requestFactory.capturedRequest = new CapturedRequest(
                    method,
                    uri,
                    headers,
                    body.toString(StandardCharsets.UTF_8));
            return new SimpleClientHttpResponse(requestFactory.responseStatus);
        }
    }

    private static class SimpleClientHttpResponse implements ClientHttpResponse {

        private final int status;

        private SimpleClientHttpResponse(int status) {
            this.status = status;
        }

        @Override
        public HttpStatusCode getStatusCode() {
            return HttpStatusCode.valueOf(status);
        }

        @Override
        public String getStatusText() {
            return String.valueOf(status);
        }

        @Override
        public HttpHeaders getHeaders() {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            return headers;
        }

        @Override
        public ByteArrayInputStream getBody() throws IOException {
            String body = status >= 400 ? "{\"error\":\"failed\"}" : "{\"ok\":true}";
            return new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public void close() {
        }
    }

    private record CapturedRequest(
            HttpMethod method,
            URI uri,
            HttpHeaders headers,
            String body) {
    }
}
