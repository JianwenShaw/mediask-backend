package me.jianwen.mediask.api.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import me.jianwen.mediask.api.advice.ResultResponseBodyAdvice;
import me.jianwen.mediask.api.audit.AuditApiSupport;
import me.jianwen.mediask.api.exception.GlobalExceptionHandler;
import me.jianwen.mediask.api.filter.RequestCorrelationFilter;
import me.jianwen.mediask.api.security.ApiSecurityProperties;
import me.jianwen.mediask.api.security.JsonAuthenticationEntryPoint;
import me.jianwen.mediask.api.security.JwtAuthenticationFilter;
import me.jianwen.mediask.api.security.ScenarioAuthorizationAspect;
import me.jianwen.mediask.api.security.SecurityConfig;
import me.jianwen.mediask.application.audit.usecase.AuditTrailService;
import me.jianwen.mediask.application.audit.usecase.QueryAuditEventsUseCase;
import me.jianwen.mediask.application.audit.usecase.QueryDataAccessLogsUseCase;
import me.jianwen.mediask.application.audit.usecase.RecordAuditEventUseCase;
import me.jianwen.mediask.application.audit.usecase.RecordDataAccessLogUseCase;
import me.jianwen.mediask.application.authz.AuthorizationDecisionService;
import me.jianwen.mediask.common.pagination.PageData;
import me.jianwen.mediask.domain.audit.model.AuditEventItem;
import me.jianwen.mediask.domain.audit.model.DataAccessAction;
import me.jianwen.mediask.domain.audit.model.DataAccessLogItem;
import me.jianwen.mediask.domain.audit.model.DataAccessPurposeCode;
import me.jianwen.mediask.domain.audit.port.AuditQueryRepository;
import me.jianwen.mediask.domain.user.model.AccessToken;
import me.jianwen.mediask.domain.user.model.AccessTokenClaims;
import me.jianwen.mediask.domain.user.model.AuthenticatedUser;
import me.jianwen.mediask.domain.user.model.DataScopeRule;
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
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AuditControllerTest {

    private static final String ADMIN_TOKEN = "admin-token";
    private static final String PATIENT_TOKEN = "patient-token";

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private MockMvc adminMockMvc;
    private MockMvc patientMockMvc;

    @BeforeEach
    void setUp() {
        AuthenticatedUser adminUser = new AuthenticatedUser(
                2001L,
                "admin",
                "系统管理员",
                UserType.ADMIN,
                new LinkedHashSet<>(List.of(RoleCode.ADMIN)),
                Set.of("audit:query"),
                Set.of(new DataScopeRule("AUDIT_EVENT", me.jianwen.mediask.domain.user.model.DataScopeType.ALL, null)),
                null,
                null,
                null);
        AuthenticatedUser patientUser = new AuthenticatedUser(
                2003L,
                "patient_li",
                "李患者",
                UserType.PATIENT,
                new LinkedHashSet<>(List.of(RoleCode.PATIENT)),
                Set.of(),
                Set.of(),
                2201L,
                null,
                null);
        StubUserAuthenticationRepository repository = new StubUserAuthenticationRepository(adminUser, patientUser);
        StubAccessTokenCodec accessTokenCodec =
                new StubAccessTokenCodec(adminUser, patientUser);
        StubAuditQueryRepository auditQueryRepository = new StubAuditQueryRepository();
        AuditApiSupport auditApiSupport = new AuditApiSupport(new AuditTrailService(
                new RecordAuditEventUseCase(record -> {}),
                new RecordDataAccessLogUseCase(record -> {})));
        AuditController target = new AuditController(
                new QueryAuditEventsUseCase(auditQueryRepository),
                new QueryDataAccessLogsUseCase(auditQueryRepository),
                auditApiSupport);
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(target);
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAspect(new ScenarioAuthorizationAspect(
                new AuthorizationDecisionService(List.of(), List.of()),
                auditApiSupport,
                me.jianwen.mediask.api.TestAuditSupport.emptyEncounterQueryRepository(),
                me.jianwen.mediask.api.TestAuditSupport.emptyAdminPatientQueryRepository()));
        AuditController controller = proxyFactory.getProxy();

        JsonAuthenticationEntryPoint authenticationEntryPoint = new JsonAuthenticationEntryPoint(objectMapper);
        SecurityConfig securityConfig = new SecurityConfig();
        RequestMatcher publicRequestMatcher = securityConfig.publicRequestMatcher(new ApiSecurityProperties(false));
        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(
                accessTokenCodec,
                new StubAccessTokenBlocklistPort(),
                repository,
                authenticationEntryPoint,
                objectMapper,
                publicRequestMatcher);
        Filter securityContextCleanupFilter = (request, response, chain) -> {
            try {
                chain.doFilter(request, response);
            } finally {
                SecurityContextHolder.clearContext();
            }
        };

        adminMockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(), new ResultResponseBodyAdvice())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setMessageConverters(
                        new StringHttpMessageConverter(),
                        new MappingJackson2HttpMessageConverter(objectMapper))
                .addFilters(new RequestCorrelationFilter(), jwtAuthenticationFilter, securityContextCleanupFilter)
                .build();
        patientMockMvc = adminMockMvc;
    }

    @Test
    void queryEvents_WhenAuthorized_ReturnPagedItems() throws Exception {
        adminMockMvc.perform(get("/api/v1/audit/events")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN)
                        .param("pageNo", "2")
                        .param("pageSize", "5")
                        .param("resourceId", "session-1")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.pageNum").value(2))
                .andExpect(jsonPath("$.data.pageSize").value(5))
                .andExpect(jsonPath("$.data.total").value(11))
                .andExpect(jsonPath("$.data.totalPages").value(3))
                .andExpect(jsonPath("$.data.hasNext").value(true))
                .andExpect(jsonPath("$.data.items[0].resourceId").value("session-1"))
                .andExpect(jsonPath("$.data.items[0].operatorUserId").value("2001"))
                .andExpect(jsonPath("$.data.items[0].operatorUsername").value("admin"))
                .andExpect(jsonPath("$.data.items[0].actorDepartmentId").value("3101"))
                .andExpect(jsonPath("$.data.items[0].reasonText").value("scope=events, action=-"))
                .andExpect(jsonPath("$.data.items[0].clientIp").value("10.0.0.8"))
                .andExpect(jsonPath("$.data.items[0].userAgent").value("AuditConsole/1.0"));
    }

    @Test
    void queryDataAccess_WhenAuthorized_ReturnPagedItems() throws Exception {
        adminMockMvc.perform(get("/api/v1/audit/data-access")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN)
                        .param("pageNo", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.pageNum").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(10))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].resourceId").value("8101"))
                .andExpect(jsonPath("$.data.items[0].accessAction").value("VIEW"))
                .andExpect(jsonPath("$.data.items[0].operatorUsername").value("admin"))
                .andExpect(jsonPath("$.data.items[0].actorDepartmentId").value("3101"))
                .andExpect(jsonPath("$.data.items[0].clientIp").value("10.0.0.9"))
                .andExpect(jsonPath("$.data.items[0].userAgent").value("AuditConsole/1.0"));
    }

    @Test
    void queryEvents_WhenMissingPermission_ReturnForbidden() throws Exception {
        patientMockMvc.perform(get("/api/v1/audit/events")
                        .header("Authorization", "Bearer " + PATIENT_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1003));
    }

    private static final class StubAuditQueryRepository implements AuditQueryRepository {

        @Override
        public PageData<AuditEventItem> queryAuditEvents(
                OffsetDateTime from,
                OffsetDateTime to,
                String actionCode,
                Long operatorUserId,
                Long patientUserId,
                Long encounterId,
                String resourceType,
                String resourceId,
                Boolean successFlag,
                String requestId,
                me.jianwen.mediask.common.pagination.PageQuery pageQuery) {
            return new PageData<>(
                    List.of(new AuditEventItem(
                            1L,
                            "req-audit-1",
                            2001L,
                            "admin",
                            "ADMIN",
                            3101L,
                            "AUDIT_QUERY",
                            "AI_SESSION",
                            resourceId == null ? "session-1" : resourceId,
                            2003L,
                            null,
                            true,
                            null,
                            null,
                            "scope=events, action=-",
                            "10.0.0.8",
                            "AuditConsole/1.0",
                            OffsetDateTime.parse("2026-05-01T09:00:00+08:00"))),
                    2,
                    5,
                    11,
                    3,
                    true);
        }

        @Override
        public PageData<DataAccessLogItem> queryDataAccessLogs(
                OffsetDateTime from,
                OffsetDateTime to,
                String resourceType,
                String resourceId,
                Long operatorUserId,
                Long patientUserId,
                Long encounterId,
                DataAccessAction accessAction,
                String accessResult,
                String requestId,
                me.jianwen.mediask.common.pagination.PageQuery pageQuery) {
            return new PageData<>(
                    List.of(new DataAccessLogItem(
                            2L,
                            "req-access-1",
                            2001L,
                            "admin",
                            "ADMIN",
                            3101L,
                            2003L,
                            8101L,
                            DataAccessAction.VIEW,
                            DataAccessPurposeCode.ADMIN_OPERATION,
                            "EMR_CONTENT",
                            "8101",
                            "ALLOWED",
                            null,
                            "10.0.0.9",
                            "AuditConsole/1.0",
                            OffsetDateTime.parse("2026-05-01T09:05:00+08:00"))),
                    1,
                    10,
                    1,
                    1,
                    false);
        }
    }

    private static final class StubAccessTokenCodec implements AccessTokenCodec {

        private final AuthenticatedUser adminUser;
        private final AuthenticatedUser patientUser;

        private StubAccessTokenCodec(AuthenticatedUser adminUser, AuthenticatedUser patientUser) {
            this.adminUser = adminUser;
            this.patientUser = patientUser;
        }

        @Override
        public AccessToken issueAccessToken(AuthenticatedUser authenticatedUser, String sessionId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AccessTokenClaims parseAccessToken(String accessToken) {
            if (ADMIN_TOKEN.equals(accessToken)) {
                return new AccessTokenClaims(adminUser.userId(), "admin-token-id", "admin-session", Instant.now().plusSeconds(3600));
            }
            if (PATIENT_TOKEN.equals(accessToken)) {
                return new AccessTokenClaims(patientUser.userId(), "patient-token-id", "patient-session", Instant.now().plusSeconds(3600));
            }
            throw new IllegalArgumentException("unsupported access token");
        }
    }

    private static final class StubAccessTokenBlocklistPort implements AccessTokenBlocklistPort {

        @Override
        public void block(String tokenId, Instant expiresAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isBlocked(String tokenId) {
            return false;
        }
    }

    private static final class StubUserAuthenticationRepository implements UserAuthenticationRepository {

        private final AuthenticatedUser adminUser;
        private final AuthenticatedUser patientUser;

        private StubUserAuthenticationRepository(AuthenticatedUser adminUser, AuthenticatedUser patientUser) {
            this.adminUser = adminUser;
            this.patientUser = patientUser;
        }

        @Override
        public Optional<me.jianwen.mediask.domain.user.model.LoginAccount> findLoginAccountByUsername(String username) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<AuthenticatedUser> findAuthenticatedUserById(Long userId) {
            if (adminUser.userId().equals(userId)) {
                return Optional.of(adminUser);
            }
            if (patientUser.userId().equals(userId)) {
                return Optional.of(patientUser);
            }
            return Optional.empty();
        }

        @Override
        public void updateLastLoginAt(Long userId) {
            throw new UnsupportedOperationException();
        }
    }
}
