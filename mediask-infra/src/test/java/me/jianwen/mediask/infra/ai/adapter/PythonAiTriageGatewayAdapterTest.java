package me.jianwen.mediask.infra.ai.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import me.jianwen.mediask.domain.ai.model.AiTriageGatewayContext;
import me.jianwen.mediask.domain.ai.model.AiTriageQuery;
import me.jianwen.mediask.domain.ai.model.AiTriageQueryResponse;
import me.jianwen.mediask.domain.ai.port.AiTriageGatewayPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.AbstractClientHttpRequest;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;

class PythonAiTriageGatewayAdapterTest {

    private CapturingClientHttpRequestFactory requestFactory;
    private CapturingHttpClient httpClient;
    private PythonAiTriageGatewayAdapter adapter;

    @BeforeEach
    void setUp() {
        requestFactory = new CapturingClientHttpRequestFactory(200, """
                {
                  "request_id": "req-1",
                  "session_id": "session-1",
                  "turn_id": "turn-1",
                  "query_run_id": "query-1",
                  "triage_result": {
                    "triage_stage": "COLLECTING",
                    "triage_completion_reason": null,
                    "next_action": "CONTINUE_TRIAGE",
                    "chief_complaint_summary": "头痛",
                    "follow_up_questions": ["是否发热？"]
                  }
                }
                """);
        httpClient = new CapturingHttpClient();
        adapter = new PythonAiTriageGatewayAdapter(
                "http://python.test",
                "dev-api-key",
                30,
                RestClient.builder().requestFactory(requestFactory).build(),
                httpClient,
                new ObjectMapper().findAndRegisterModules());
    }

    @Test
    void query_ForwardsHeadersAndBody() {
        AiTriageQueryResponse response = adapter.query(
                new AiTriageGatewayContext("req-1", 2201L),
                new AiTriageQuery(null, "default", "头痛"));

        assertEquals("POST", requestFactory.capturedRequest.method.name());
        assertEquals("http://python.test/api/v1/query", requestFactory.capturedRequest.uri.toString());
        assertEquals("req-1", requestFactory.capturedRequest.headers.getFirst("X-Request-Id"));
        assertEquals("dev-api-key", requestFactory.capturedRequest.headers.getFirst("X-API-Key"));
        assertTrue(requestFactory.capturedRequest.body.contains("\"scene\":\"AI_TRIAGE\""));
        assertTrue(requestFactory.capturedRequest.body.contains("\"session_id\":null"));
        assertTrue(requestFactory.capturedRequest.body.contains("\"hospital_scope\":\"default\""));
        assertEquals("COLLECTING", response.triageResult().triageStage());
    }

    @Test
    void streamQuery_ParsesFinalEvent() {
        httpClient.responseBody = """
                event: start
                data: {"request_id":"req-1"}

                event: final
                data: {"request_id":"req-1","session_id":"session-1","turn_id":"turn-1","query_run_id":"query-1","triage_result":{"triage_stage":"READY","triage_completion_reason":"SUFFICIENT_INFO","next_action":"VIEW_TRIAGE_RESULT","risk_level":"low","chief_complaint_summary":"头痛","recommended_departments":[{"department_id":3101,"department_name":"神经内科","priority":1,"reason":"头痛优先神经内科"}],"care_advice":"建议门诊就诊","catalog_version":"deptcat-v20260501-01","citations":[]}}

                event: done
                data: {}

                """;

        java.util.LinkedList<AiTriageGatewayPort.StreamEvent> events = new java.util.LinkedList<>();
        adapter.streamQuery(
                new AiTriageGatewayContext("req-1", 2201L),
                new AiTriageQuery(null, "default", "头痛"),
                events::add);

        assertEquals("http://python.test/api/v1/query/stream", httpClient.lastRequest.uri().toString());
        assertEquals("req-1", httpClient.lastRequest.headers().firstValue("X-Request-Id").orElseThrow());
        assertTrue(readBody(httpClient.lastRequest).contains("\"session_id\":null"));
        assertEquals(3, events.size());
        assertEquals("start", events.get(0).event());
        assertEquals("READY", events.get(1).finalResponse().triageResult().triageStage());
        assertEquals("done", events.get(2).event());
    }

    private static String readBody(HttpRequest request) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(java.nio.ByteBuffer item) {
                byte[] bytes = new byte[item.remaining()];
                item.get(bytes);
                outputStream.write(bytes, 0, bytes.length);
            }

            @Override
            public void onError(Throwable throwable) {
                throw new AssertionError(throwable);
            }

            @Override
            public void onComplete() {
            }
        });
        return outputStream.toString(StandardCharsets.UTF_8);
    }

    private static final class CapturingClientHttpRequestFactory implements ClientHttpRequestFactory {

        private final int responseStatus;
        private final String responseBody;
        private CapturedRequest capturedRequest;

        private CapturingClientHttpRequestFactory(int responseStatus, String responseBody) {
            this.responseStatus = responseStatus;
            this.responseBody = responseBody;
        }

        @Override
        public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
            return new CapturingClientHttpRequest(uri, httpMethod, this);
        }
    }

    private static final class CapturingClientHttpRequest extends AbstractClientHttpRequest {

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
            return new SimpleClientHttpResponse(requestFactory.responseStatus, requestFactory.responseBody);
        }
    }

    private static final class SimpleClientHttpResponse implements ClientHttpResponse {

        private final int status;
        private final String body;

        private SimpleClientHttpResponse(int status, String body) {
            this.status = status;
            this.body = body;
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
        public ByteArrayInputStream getBody() {
            return new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public void close() {
        }
    }

    private static final class CapturedRequest {

        private final HttpMethod method;
        private final URI uri;
        private final HttpHeaders headers;
        private final String body;

        private CapturedRequest(HttpMethod method, URI uri, HttpHeaders headers, String body) {
            this.method = method;
            this.uri = uri;
            this.headers = headers;
            this.body = body;
        }
    }

    private static final class CapturingHttpClient extends HttpClient {

        private HttpRequest lastRequest;
        private String responseBody = "";

        @Override
        public Optional<CookieHandler> cookieHandler() {
            return Optional.empty();
        }

        @Override
        public Optional<Duration> connectTimeout() {
            return Optional.empty();
        }

        @Override
        public Redirect followRedirects() {
            return Redirect.NEVER;
        }

        @Override
        public Optional<ProxySelector> proxy() {
            return Optional.empty();
        }

        @Override
        public javax.net.ssl.SSLContext sslContext() {
            try {
                TrustManager[] trustManagers = new TrustManager[] {new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {}

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {}

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }};
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, trustManagers, new SecureRandom());
                return sslContext;
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        }

        @Override
        public SSLParameters sslParameters() {
            return new SSLParameters();
        }

        @Override
        public Optional<Executor> executor() {
            return Optional.empty();
        }

        @Override
        public Version version() {
            return Version.HTTP_1_1;
        }

        @Override
        public Optional<java.net.Authenticator> authenticator() {
            return Optional.empty();
        }

        @Override
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
            this.lastRequest = request;
            return (HttpResponse<T>) new SimpleHttpResponse(request, responseBody);
        }

        @Override
        public <T> java.util.concurrent.CompletableFuture<HttpResponse<T>> sendAsync(
                HttpRequest request,
                HttpResponse.BodyHandler<T> responseBodyHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> java.util.concurrent.CompletableFuture<HttpResponse<T>> sendAsync(
                HttpRequest request,
                HttpResponse.BodyHandler<T> responseBodyHandler,
                HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class SimpleHttpResponse implements HttpResponse<InputStream> {

        private final HttpRequest request;
        private final String body;

        private SimpleHttpResponse(HttpRequest request, String body) {
            this.request = request;
            this.body = body;
        }

        @Override
        public int statusCode() {
            return 200;
        }

        @Override
        public HttpRequest request() {
            return request;
        }

        @Override
        public Optional<HttpResponse<InputStream>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public java.net.http.HttpHeaders headers() {
            return java.net.http.HttpHeaders.of(Map.of(), (name, value) -> true);
        }

        @Override
        public InputStream body() {
            return new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return request.uri();
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }
    }
}
