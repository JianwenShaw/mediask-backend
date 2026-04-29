package me.jianwen.mediask.api.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import me.jianwen.mediask.api.advice.ResultResponseBodyAdvice;
import me.jianwen.mediask.api.exception.GlobalExceptionHandler;
import me.jianwen.mediask.api.security.AuthenticatedUserPrincipal;
import me.jianwen.mediask.api.security.ScenarioAuthorizationAspect;
import me.jianwen.mediask.application.authz.AuthorizationDecisionService;
import me.jianwen.mediask.application.knowledge.usecase.KnowledgeAdminGatewayUseCase;
import me.jianwen.mediask.domain.ai.model.CreateKnowledgeBasePayload;
import me.jianwen.mediask.domain.ai.model.KnowledgeAdminFile;
import me.jianwen.mediask.domain.ai.model.KnowledgeAdminGatewayContext;
import me.jianwen.mediask.domain.ai.model.KnowledgeBaseListQuery;
import me.jianwen.mediask.domain.ai.model.KnowledgeDocumentListQuery;
import me.jianwen.mediask.domain.ai.model.PublishKnowledgeReleasePayload;
import me.jianwen.mediask.domain.ai.model.UpdateKnowledgeBasePayload;
import me.jianwen.mediask.domain.ai.port.KnowledgeAdminGatewayPort;
import me.jianwen.mediask.domain.user.model.AuthenticatedUser;
import me.jianwen.mediask.domain.user.model.RoleCode;
import me.jianwen.mediask.domain.user.model.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class KnowledgeAdminControllerTest {

    private static final Set<String> ALL_PERMISSIONS = Set.of(
            "admin:knowledge-base:list",
            "admin:knowledge-base:create",
            "admin:knowledge-base:update",
            "admin:knowledge-base:delete",
            "admin:knowledge-document:import",
            "admin:knowledge-document:list",
            "admin:knowledge-document:delete",
            "admin:knowledge-ingest-job:view",
            "admin:knowledge-index-version:list",
            "admin:knowledge-release:list",
            "admin:knowledge-release:publish");

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private StubKnowledgeAdminGatewayPort gatewayPort;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        gatewayPort = new StubKnowledgeAdminGatewayPort();
        KnowledgeAdminController target = new KnowledgeAdminController(new KnowledgeAdminGatewayUseCase(gatewayPort));
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(target);
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAspect(new ScenarioAuthorizationAspect(new AuthorizationDecisionService(List.of(), List.of())));
        KnowledgeAdminController controller = proxyFactory.getProxy();

        Filter testAuthenticationFilter = (request, response, chain) -> {
            try {
                String permissionsHeader = ((HttpServletRequest) request).getHeader("X-Test-Permissions");
                Set<String> permissions = "all".equals(permissionsHeader) ? ALL_PERMISSIONS : Set.of();
                AuthenticatedUser user = new AuthenticatedUser(
                        2001L,
                        "admin",
                        "系统管理员",
                        UserType.ADMIN,
                        Set.of(RoleCode.ADMIN),
                        permissions,
                        Set.of(),
                        null,
                        null,
                        null);
                AuthenticatedUserPrincipal principal = AuthenticatedUserPrincipal.from(user);
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(principal, "n/a", principal.authorities()));
                chain.doFilter(request, response);
            } finally {
                SecurityContextHolder.clearContext();
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(), new ResultResponseBodyAdvice())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .addFilters(testAuthenticationFilter)
                .build();
    }

    @Test
    void listKnowledgeBases_WhenAuthorized_ForwardsCamelCaseQueryAndReturnsCamelCasePayload() throws Exception {
        mockMvc.perform(get("/api/v1/admin/knowledge-bases")
                        .header("X-Test-Permissions", "all")
                        .header("X-Request-Id", "req-knowledge-list")
                        .param("keyword", "triage")
                        .param("pageNum", "2")
                        .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.items[0].code").value("triage-general"))
                .andExpect(jsonPath("$.data.items[0].documentCount").value(12))
                .andExpect(jsonPath("$.data.pageNum").value(2))
                .andExpect(jsonPath("$.data.pageSize").value(5))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.hasNext").value(false));

        org.junit.jupiter.api.Assertions.assertEquals(
                new KnowledgeBaseListQuery("triage", 2, 5),
                gatewayPort.lastKnowledgeBaseListQuery);
        org.junit.jupiter.api.Assertions.assertEquals("req-knowledge-list", gatewayPort.lastContext.requestId());
        org.junit.jupiter.api.Assertions.assertEquals(2001L, gatewayPort.lastContext.actorId());
        org.junit.jupiter.api.Assertions.assertEquals("default", gatewayPort.lastContext.hospitalScope());
    }

    @Test
    void createKnowledgeBase_WhenMissingPermission_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/admin/knowledge-bases")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1003));
    }

    @Test
    void createKnowledgeBase_WhenAuthorized_ForwardsCamelCaseBodyAsSnakeCaseAndReturnsCamelCasePayload()
            throws Exception {
        mockMvc.perform(post("/api/v1/admin/knowledge-bases")
                        .header("X-Test-Permissions", "all")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "triage-general",
                                  "name": "导诊通用知识库",
                                  "defaultEmbeddingModel": "text-embedding-v4",
                                  "defaultEmbeddingDimension": 1024,
                                  "retrievalStrategy": "HYBRID_RRF"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.defaultEmbeddingModel").value("text-embedding-v4"))
                .andExpect(jsonPath("$.data.defaultEmbeddingDimension").value(1024))
                .andExpect(jsonPath("$.data.retrievalStrategy").value("HYBRID_RRF"));

        org.junit.jupiter.api.Assertions.assertEquals("text-embedding-v4", gatewayPort.lastCreatePayload.defaultEmbeddingModel());
        org.junit.jupiter.api.Assertions.assertEquals(1024, gatewayPort.lastCreatePayload.defaultEmbeddingDimension());
        org.junit.jupiter.api.Assertions.assertEquals("HYBRID_RRF", gatewayPort.lastCreatePayload.retrievalStrategy());
    }

    @Test
    void updateKnowledgeBase_WhenAuthorized_ForwardsBody() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/knowledge-bases/kb-1")
                        .header("X-Test-Permissions", "all")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "导诊通用知识库",
                                  "status": "ENABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("kb-1"));

        org.junit.jupiter.api.Assertions.assertEquals("kb-1", gatewayPort.lastResourceId);
        org.junit.jupiter.api.Assertions.assertEquals("ENABLED", gatewayPort.lastUpdatePayload.status());
    }

    @Test
    void importKnowledgeDocument_WhenAuthorized_ForwardsMultipartFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "manual.pdf",
                "application/pdf",
                "pdf-content".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/admin/knowledge-documents/import")
                        .file(file)
                        .header("X-Test-Permissions", "all")
                        .param("knowledgeBaseId", "kb-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.documentId").value("doc-1"))
                .andExpect(jsonPath("$.data.jobId").value("job-1"));

        org.junit.jupiter.api.Assertions.assertEquals("kb-1", gatewayPort.lastResourceId);
        org.junit.jupiter.api.Assertions.assertEquals("manual.pdf", gatewayPort.lastFile.filename());
        org.junit.jupiter.api.Assertions.assertArrayEquals(
                "pdf-content".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                gatewayPort.lastFile.content());
    }

    @Test
    void listKnowledgeDocuments_WhenAuthorized_ForwardsCamelCaseQueryAsSnakeCase() throws Exception {
        mockMvc.perform(get("/api/v1/admin/knowledge-documents")
                        .header("X-Test-Permissions", "all")
                        .param("knowledgeBaseId", "kb-1")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.items[0].latestJobStatus").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data.pageNum").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(10));

        org.junit.jupiter.api.Assertions.assertEquals(
                new KnowledgeDocumentListQuery("kb-1", 1, 10),
                gatewayPort.lastKnowledgeDocumentListQuery);
    }

    @Test
    void listKnowledgeIndexVersions_WhenAuthorized_ForwardsCamelCaseQueryAsSnakeCase() throws Exception {
        mockMvc.perform(get("/api/v1/admin/knowledge-index-versions")
                        .header("X-Test-Permissions", "all")
                        .param("knowledgeBaseId", "kb-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.items[0].versionCode").value("v1"));

        org.junit.jupiter.api.Assertions.assertEquals("kb-1", gatewayPort.lastResourceId);
    }

    @Test
    void listKnowledgeReleases_WhenAuthorized_ForwardsCamelCaseQueryAsSnakeCase() throws Exception {
        mockMvc.perform(get("/api/v1/admin/knowledge-releases")
                        .header("X-Test-Permissions", "all")
                        .param("knowledgeBaseId", "kb-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.items[0].targetIndexVersionId").value("idx-1"));

        org.junit.jupiter.api.Assertions.assertEquals("kb-1", gatewayPort.lastResourceId);
    }

    @Test
    void publishKnowledgeRelease_WhenAuthorized_ForwardsBody() throws Exception {
        mockMvc.perform(post("/api/v1/admin/knowledge-releases")
                        .header("X-Test-Permissions", "all")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "knowledgeBaseId": "kb-1",
                                  "targetIndexVersionId": "idx-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.releaseId").value("rel-1"))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        org.junit.jupiter.api.Assertions.assertEquals("kb-1", gatewayPort.lastPublishPayload.knowledgeBaseId());
        org.junit.jupiter.api.Assertions.assertEquals("idx-1", gatewayPort.lastPublishPayload.targetIndexVersionId());
    }

    @Test
    void deleteKnowledgeDocument_WhenAuthorized_ReturnsSuccessWrapper() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/knowledge-documents/doc-1")
                        .header("X-Test-Permissions", "all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        org.junit.jupiter.api.Assertions.assertEquals("doc-1", gatewayPort.lastResourceId);
    }

    private static class StubKnowledgeAdminGatewayPort implements KnowledgeAdminGatewayPort {

        private KnowledgeAdminGatewayContext lastContext;
        private KnowledgeBaseListQuery lastKnowledgeBaseListQuery;
        private CreateKnowledgeBasePayload lastCreatePayload;
        private UpdateKnowledgeBasePayload lastUpdatePayload;
        private KnowledgeDocumentListQuery lastKnowledgeDocumentListQuery;
        private PublishKnowledgeReleasePayload lastPublishPayload;
        private String lastResourceId;
        private KnowledgeAdminFile lastFile;

        @Override
        public Object listKnowledgeBases(KnowledgeAdminGatewayContext context, KnowledgeBaseListQuery query) {
            lastContext = context;
            lastKnowledgeBaseListQuery = query;
            return Map.of(
                    "items", List.of(Map.of(
                            "id", "kb-1",
                            "code", "triage-general",
                            "document_count", 12)),
                    "page_num", 2,
                    "page_size", 5,
                    "total", 1,
                    "total_pages", 1,
                    "has_next", false);
        }

        @Override
        public Object createKnowledgeBase(KnowledgeAdminGatewayContext context, CreateKnowledgeBasePayload payload) {
            lastContext = context;
            lastCreatePayload = payload;
            return Map.of(
                    "default_embedding_model", payload.defaultEmbeddingModel(),
                    "default_embedding_dimension", payload.defaultEmbeddingDimension(),
                    "retrieval_strategy", payload.retrievalStrategy());
        }

        @Override
        public Object updateKnowledgeBase(
                KnowledgeAdminGatewayContext context,
                String knowledgeBaseId,
                UpdateKnowledgeBasePayload payload) {
            lastContext = context;
            lastResourceId = knowledgeBaseId;
            lastUpdatePayload = payload;
            return Map.of("id", knowledgeBaseId, "status", payload.status());
        }

        @Override
        public void deleteKnowledgeBase(KnowledgeAdminGatewayContext context, String knowledgeBaseId) {
            lastContext = context;
            lastResourceId = knowledgeBaseId;
        }

        @Override
        public Object importKnowledgeDocument(
                KnowledgeAdminGatewayContext context,
                String knowledgeBaseId,
                KnowledgeAdminFile file) {
            lastContext = context;
            lastResourceId = knowledgeBaseId;
            lastFile = file;
            return Map.of("document_id", "doc-1", "job_id", "job-1");
        }

        @Override
        public Object listKnowledgeDocuments(KnowledgeAdminGatewayContext context, KnowledgeDocumentListQuery query) {
            lastContext = context;
            lastKnowledgeDocumentListQuery = query;
            return Map.of(
                    "items", List.of(Map.of(
                            "id", "doc-1",
                            "latest_job_status", "SUCCEEDED")),
                    "page_num", 1,
                    "page_size", 10);
        }

        @Override
        public void deleteKnowledgeDocument(KnowledgeAdminGatewayContext context, String documentId) {
            lastContext = context;
            lastResourceId = documentId;
        }

        @Override
        public Object getIngestJob(KnowledgeAdminGatewayContext context, String jobId) {
            lastContext = context;
            lastResourceId = jobId;
            return Map.of("id", jobId);
        }

        @Override
        public Object listKnowledgeIndexVersions(KnowledgeAdminGatewayContext context, String knowledgeBaseId) {
            lastContext = context;
            lastResourceId = knowledgeBaseId;
            return Map.of("items", List.of(Map.of("version_code", "v1")));
        }

        @Override
        public Object listKnowledgeReleases(KnowledgeAdminGatewayContext context, String knowledgeBaseId) {
            lastContext = context;
            lastResourceId = knowledgeBaseId;
            return Map.of("items", List.of(Map.of("target_index_version_id", "idx-1")));
        }

        @Override
        public Object publishKnowledgeRelease(
                KnowledgeAdminGatewayContext context,
                PublishKnowledgeReleasePayload payload) {
            lastContext = context;
            lastPublishPayload = payload;
            return Map.of("release_id", "rel-1", "status", "PUBLISHED");
        }
    }
}
