package me.jianwen.mediask.api.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.Filter;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import me.jianwen.mediask.api.advice.ResultResponseBodyAdvice;
import me.jianwen.mediask.api.exception.GlobalExceptionHandler;
import me.jianwen.mediask.api.filter.RequestCorrelationFilter;
import me.jianwen.mediask.api.security.ApiSecurityProperties;
import me.jianwen.mediask.api.security.JsonAuthenticationEntryPoint;
import me.jianwen.mediask.api.security.JwtAuthenticationFilter;
import me.jianwen.mediask.api.security.ScenarioAuthorizationAspect;
import me.jianwen.mediask.api.security.SecurityConfig;
import me.jianwen.mediask.application.ai.usecase.CreateKnowledgeBaseUseCase;
import me.jianwen.mediask.application.ai.usecase.DeleteKnowledgeBaseUseCase;
import me.jianwen.mediask.application.ai.usecase.DeleteKnowledgeDocumentUseCase;
import me.jianwen.mediask.application.ai.usecase.ListKnowledgeBasesUseCase;
import me.jianwen.mediask.application.ai.usecase.ListKnowledgeDocumentsUseCase;
import me.jianwen.mediask.application.ai.usecase.UpdateKnowledgeBaseUseCase;
import me.jianwen.mediask.application.authz.AuthorizationDecisionService;
import me.jianwen.mediask.common.pagination.PageData;
import me.jianwen.mediask.common.pagination.PageQuery;
import me.jianwen.mediask.domain.ai.model.KnowledgeBase;
import me.jianwen.mediask.domain.ai.model.KnowledgeBaseOwnerType;
import me.jianwen.mediask.domain.ai.model.KnowledgeBaseStatus;
import me.jianwen.mediask.domain.ai.model.KnowledgeBaseSummary;
import me.jianwen.mediask.domain.ai.model.KnowledgeBaseVisibility;
import me.jianwen.mediask.domain.ai.model.KnowledgeDocument;
import me.jianwen.mediask.domain.ai.model.KnowledgeDocumentStatus;
import me.jianwen.mediask.domain.ai.model.KnowledgeDocumentSummary;
import me.jianwen.mediask.domain.ai.model.KnowledgeSourceType;
import me.jianwen.mediask.domain.ai.port.KnowledgeBaseRepository;
import me.jianwen.mediask.domain.ai.port.KnowledgeDocumentRepository;
import me.jianwen.mediask.domain.user.model.AccessToken;
import me.jianwen.mediask.domain.user.model.AccessTokenClaims;
import me.jianwen.mediask.domain.user.model.AuthenticatedUser;
import me.jianwen.mediask.domain.user.model.LoginAccount;
import me.jianwen.mediask.domain.user.model.RoleCode;
import me.jianwen.mediask.domain.user.model.UserType;
import me.jianwen.mediask.domain.user.port.AccessTokenBlocklistPort;
import me.jianwen.mediask.domain.user.port.AccessTokenCodec;
import me.jianwen.mediask.domain.user.port.UserAuthenticationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class KnowledgeAdminControllerTest {

    private static final String ADMIN_TOKEN = "admin-token";
    private static final String PATIENT_TOKEN = "patient-token";
    private static final Instant TOKEN_EXPIRES_AT = Instant.parse("2026-03-30T08:00:00Z");

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AuthenticatedUser adminUser = new AuthenticatedUser(
                2001L,
                "admin",
                "系统管理员",
                UserType.ADMIN,
                new LinkedHashSet<>(List.of(RoleCode.ADMIN)),
                Set.of(
                        "admin:knowledge:base:list",
                        "admin:knowledge:base:create",
                        "admin:knowledge:base:update",
                        "admin:knowledge:base:delete",
                        "admin:knowledge:document:list",
                        "admin:knowledge:document:delete"),
                Set.of(),
                null,
                null,
                null);
        AuthenticatedUser patientUser = new AuthenticatedUser(
                2003L,
                "patient_li",
                "李患者",
                UserType.PATIENT,
                new LinkedHashSet<>(List.of(RoleCode.PATIENT)),
                Set.of("patient:profile:view:self"),
                Set.of(),
                2201L,
                null,
                null);

        StubUserAuthenticationRepository repository = new StubUserAuthenticationRepository(adminUser, patientUser);
        StubAccessTokenCodec accessTokenCodec = new StubAccessTokenCodec(Map.of(
                ADMIN_TOKEN, adminUser,
                PATIENT_TOKEN, patientUser));
        AccessTokenBlocklistPort accessTokenBlocklistPort = new StubAccessTokenBlocklistPort();

        InMemoryKnowledgeBaseRepository knowledgeBaseRepository = new InMemoryKnowledgeBaseRepository();
        InMemoryKnowledgeDocumentRepository knowledgeDocumentRepository = new InMemoryKnowledgeDocumentRepository();
        knowledgeBaseRepository.seed();
        knowledgeDocumentRepository.seed();

        AdminKnowledgeBaseController knowledgeBaseTarget = new AdminKnowledgeBaseController(
                new ListKnowledgeBasesUseCase(knowledgeBaseRepository),
                new CreateKnowledgeBaseUseCase(knowledgeBaseRepository),
                new UpdateKnowledgeBaseUseCase(knowledgeBaseRepository),
                new DeleteKnowledgeBaseUseCase(knowledgeBaseRepository));
        AdminKnowledgeDocumentController knowledgeDocumentTarget =
                new AdminKnowledgeDocumentController(
                        new ListKnowledgeDocumentsUseCase(knowledgeDocumentRepository),
                        new DeleteKnowledgeDocumentUseCase(knowledgeDocumentRepository));

        ScenarioAuthorizationAspect aspect = new ScenarioAuthorizationAspect(new AuthorizationDecisionService(List.of(), List.of()));
        AspectJProxyFactory kbProxyFactory = new AspectJProxyFactory(knowledgeBaseTarget);
        kbProxyFactory.setProxyTargetClass(true);
        kbProxyFactory.addAspect(aspect);
        AspectJProxyFactory docProxyFactory = new AspectJProxyFactory(knowledgeDocumentTarget);
        docProxyFactory.setProxyTargetClass(true);
        docProxyFactory.addAspect(aspect);

        JsonAuthenticationEntryPoint authenticationEntryPoint =
                new JsonAuthenticationEntryPoint(new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules());
        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(
                accessTokenCodec,
                accessTokenBlocklistPort,
                repository,
                authenticationEntryPoint,
                new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules(),
                new SecurityConfig().publicRequestMatcher(new ApiSecurityProperties(false)));

        Filter securityContextCleanupFilter = (request, response, chain) -> {
            try {
                chain.doFilter(request, response);
            } finally {
                SecurityContextHolder.clearContext();
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(kbProxyFactory.getProxy(), docProxyFactory.getProxy())
                .setControllerAdvice(new GlobalExceptionHandler(), new ResultResponseBodyAdvice())
                .setMessageConverters(
                        new StringHttpMessageConverter(),
                        new MappingJackson2HttpMessageConverter(new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules()))
                .addFilters(new RequestCorrelationFilter(), jwtAuthenticationFilter, securityContextCleanupFilter)
                .build();
    }

    @Test
    void listKnowledgeBases_WhenMissingPermission_ReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/knowledge-bases").header("Authorization", "Bearer " + PATIENT_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1003));
    }

    @Test
    void listKnowledgeBases_WhenAuthorized_ReturnPagedData() throws Exception {
        mockMvc.perform(get("/api/v1/admin/knowledge-bases")
                        .param("keyword", "系统")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].kbCode").value("KB_SYSTEM_TRIAGE"))
                .andExpect(jsonPath("$.data.items[0].docCount").value(2));
    }

    @Test
    void createKnowledgeBase_WhenAuthorized_ReturnCreatedData() throws Exception {
        mockMvc.perform(post("/api/v1/admin/knowledge-bases")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "心内科知识库",
                                  "kbCode": "KB_CARDIOLOGY",
                                  "ownerType": "DEPARTMENT",
                                  "ownerDeptId": 3103,
                                  "visibility": "DEPT"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.kbCode").value("KB_CARDIOLOGY"))
                .andExpect(jsonPath("$.data.docCount").value(0))
                .andExpect(jsonPath("$.data.status").value("ENABLED"));
    }

    @Test
    void updateKnowledgeBase_WhenAuthorized_ReturnUpdatedData() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/knowledge-bases/4001")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "DISABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("系统导诊知识库"))
                .andExpect(jsonPath("$.data.status").value("DISABLED"))
                .andExpect(jsonPath("$.data.docCount").value(2));
    }

    @Test
    void deleteKnowledgeBase_WhenAuthorized_ReturnSuccess() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/knowledge-bases/4001").header("Authorization", "Bearer " + ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void listKnowledgeDocuments_WhenAuthorized_ReturnPagedData() throws Exception {
        mockMvc.perform(get("/api/v1/admin/knowledge-documents")
                        .param("knowledgeBaseId", "4001")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].title").value("高血压指南"))
                .andExpect(jsonPath("$.data.items[0].chunkCount").value(6));
    }

    @Test
    void deleteKnowledgeDocument_WhenAuthorized_ReturnSuccess() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/knowledge-documents/5001")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private record StubUserAuthenticationRepository(AuthenticatedUser adminUser, AuthenticatedUser patientUser)
            implements UserAuthenticationRepository {

        @Override
        public Optional<LoginAccount> findLoginAccountByUsername(String username) {
            throw new UnsupportedOperationException("not needed");
        }

        @Override
        public Optional<AuthenticatedUser> findAuthenticatedUserById(Long userId) {
            if (userId.equals(adminUser.userId())) {
                return Optional.of(adminUser);
            }
            if (userId.equals(patientUser.userId())) {
                return Optional.of(patientUser);
            }
            return Optional.empty();
        }

        @Override
        public void updateLastLoginAt(Long userId) {
            throw new UnsupportedOperationException("not needed");
        }
    }

    private static final class StubAccessTokenBlocklistPort implements AccessTokenBlocklistPort {

        @Override
        public void block(String tokenId, Instant expiresAt) {
            throw new UnsupportedOperationException("not needed");
        }

        @Override
        public boolean isBlocked(String tokenId) {
            return false;
        }
    }

    private static final class StubAccessTokenCodec implements AccessTokenCodec {

        private final Map<String, AuthenticatedUser> usersByToken;

        private StubAccessTokenCodec(Map<String, AuthenticatedUser> usersByToken) {
            this.usersByToken = usersByToken;
        }

        @Override
        public AccessToken issueAccessToken(AuthenticatedUser authenticatedUser, String sessionId) {
            throw new UnsupportedOperationException("not needed");
        }

        @Override
        public AccessTokenClaims parseAccessToken(String accessToken) {
            AuthenticatedUser user = usersByToken.get(accessToken);
            if (user == null) {
                throw new IllegalArgumentException("invalid token");
            }
            return new AccessTokenClaims(user.userId(), accessToken + "-id", "session-" + user.userId(), TOKEN_EXPIRES_AT);
        }
    }

    private static final class InMemoryKnowledgeBaseRepository implements KnowledgeBaseRepository {

        private final Map<Long, KnowledgeBase> items = new LinkedHashMap<>();

        private void seed() {
            KnowledgeBase knowledgeBase = KnowledgeBase.rehydrate(
                    4001L,
                    "KB_SYSTEM_TRIAGE",
                    "系统导诊知识库",
                    KnowledgeBaseOwnerType.SYSTEM,
                    null,
                    KnowledgeBaseVisibility.PUBLIC,
                    KnowledgeBaseStatus.ENABLED,
                    0);
            items.put(knowledgeBase.id(), knowledgeBase);
        }

        @Override
        public boolean existsEnabled(Long knowledgeBaseId) {
            KnowledgeBase knowledgeBase = items.get(knowledgeBaseId);
            return knowledgeBase != null && knowledgeBase.status() == KnowledgeBaseStatus.ENABLED;
        }

        @Override
        public PageData<KnowledgeBaseSummary> pageByKeyword(String keyword, PageQuery pageQuery) {
            return new PageData<>(
                    List.of(new KnowledgeBaseSummary(
                            4001L,
                            "KB_SYSTEM_TRIAGE",
                            "系统导诊知识库",
                            KnowledgeBaseOwnerType.SYSTEM,
                            null,
                            KnowledgeBaseVisibility.PUBLIC,
                            KnowledgeBaseStatus.ENABLED,
                            2L)),
                    pageQuery.pageNum(),
                    pageQuery.pageSize(),
                    1,
                    1,
                    false);
        }

        @Override
        public void save(KnowledgeBase knowledgeBase) {
            items.put(knowledgeBase.id(), knowledgeBase);
        }

        @Override
        public Optional<KnowledgeBase> findById(Long knowledgeBaseId) {
            return Optional.ofNullable(items.get(knowledgeBaseId));
        }

        @Override
        public Optional<KnowledgeBaseSummary> findSummaryById(Long knowledgeBaseId) {
            KnowledgeBase knowledgeBase = items.get(knowledgeBaseId);
            if (knowledgeBase == null) {
                return Optional.empty();
            }
            long docCount = knowledgeBaseId.equals(4001L) ? 2L : 0L;
            return Optional.of(new KnowledgeBaseSummary(
                    knowledgeBase.id(),
                    knowledgeBase.kbCode(),
                    knowledgeBase.name(),
                    knowledgeBase.ownerType(),
                    knowledgeBase.ownerDeptId(),
                    knowledgeBase.visibility(),
                    knowledgeBase.status(),
                    docCount));
        }

        @Override
        public void update(KnowledgeBase knowledgeBase) {
            items.put(knowledgeBase.id(), knowledgeBase);
        }

        @Override
        public void deleteById(Long knowledgeBaseId) {
            items.remove(knowledgeBaseId);
        }
    }

    private static final class InMemoryKnowledgeDocumentRepository implements KnowledgeDocumentRepository {

        private final Map<Long, KnowledgeDocument> items = new LinkedHashMap<>();

        private void seed() {
            KnowledgeDocument document = KnowledgeDocument.rehydrate(
                    5001L,
                    4001L,
                    java.util.UUID.fromString("11111111-1111-1111-1111-111111111111"),
                    "高血压指南",
                    KnowledgeSourceType.PDF,
                    "file:///tmp/htn-guide.pdf",
                    "hash-1",
                    "zh-CN",
                    1,
                    "JAVA",
                    KnowledgeDocumentStatus.ACTIVE,
                    0);
            items.put(document.id(), document);
        }

        @Override
        public void save(KnowledgeDocument knowledgeDocument) {
            items.put(knowledgeDocument.id(), knowledgeDocument);
        }

        @Override
        public Optional<KnowledgeDocument> findById(Long documentId) {
            return Optional.ofNullable(items.get(documentId));
        }

        @Override
        public boolean existsEffectiveByKnowledgeBaseIdAndContentHash(Long knowledgeBaseId, String contentHash) {
            return false;
        }

        @Override
        public void update(KnowledgeDocument knowledgeDocument) {
            items.put(knowledgeDocument.id(), knowledgeDocument);
        }

        @Override
        public PageData<KnowledgeDocumentSummary> pageByKnowledgeBaseId(Long knowledgeBaseId, PageQuery pageQuery) {
            return new PageData<>(
                    List.of(new KnowledgeDocumentSummary(
                            5001L,
                            "11111111-1111-1111-1111-111111111111",
                            "高血压指南",
                            KnowledgeSourceType.PDF,
                            KnowledgeDocumentStatus.ACTIVE,
                            6L)),
                    pageQuery.pageNum(),
                    pageQuery.pageSize(),
                    1,
                    1,
                    false);
        }

        @Override
        public void deleteById(Long documentId) {
            items.remove(documentId);
        }
    }
}
