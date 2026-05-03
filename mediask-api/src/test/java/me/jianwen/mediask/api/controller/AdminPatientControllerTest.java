package me.jianwen.mediask.api.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import me.jianwen.mediask.api.TestAuditSupport;
import me.jianwen.mediask.api.advice.ResultResponseBodyAdvice;
import me.jianwen.mediask.api.exception.GlobalExceptionHandler;
import me.jianwen.mediask.api.filter.RequestCorrelationFilter;
import me.jianwen.mediask.api.security.AuthenticatedUserPrincipal;
import me.jianwen.mediask.api.security.ApiSecurityProperties;
import me.jianwen.mediask.api.security.JsonAuthenticationEntryPoint;
import me.jianwen.mediask.api.security.JwtAuthenticationFilter;
import me.jianwen.mediask.api.security.ScenarioAuthorizationAspect;
import me.jianwen.mediask.api.security.SecurityConfig;
import me.jianwen.mediask.application.authz.AuthorizationDecisionService;
import me.jianwen.mediask.application.user.usecase.CreateAdminPatientUseCase;
import me.jianwen.mediask.application.user.usecase.DeleteAdminPatientUseCase;
import me.jianwen.mediask.application.user.usecase.GetAdminPatientDetailUseCase;
import me.jianwen.mediask.application.user.usecase.ListAdminPatientsUseCase;
import me.jianwen.mediask.application.user.usecase.UpdateAdminPatientUseCase;
import me.jianwen.mediask.common.pagination.PageData;
import me.jianwen.mediask.common.pagination.PageQuery;
import me.jianwen.mediask.domain.user.model.AccessToken;
import me.jianwen.mediask.domain.user.model.AccessTokenClaims;
import me.jianwen.mediask.domain.user.model.AdminPatientDetail;
import me.jianwen.mediask.domain.user.model.AdminPatientListItem;
import me.jianwen.mediask.domain.user.model.AuthenticatedUser;
import me.jianwen.mediask.domain.user.model.LoginAccount;
import me.jianwen.mediask.domain.user.model.RoleCode;
import me.jianwen.mediask.domain.user.model.UserType;
import me.jianwen.mediask.domain.user.port.AdminPatientQueryRepository;
import me.jianwen.mediask.domain.user.port.AdminPatientWriteRepository;
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

class AdminPatientControllerTest {

    private static final String ADMIN_TOKEN = "admin-token";
    private static final String PATIENT_TOKEN = "patient-token";
    private static final Instant TOKEN_EXPIRES_AT = Instant.parse("2026-03-30T08:00:00Z");

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AuthenticatedUser adminUser = new AuthenticatedUser(
                2001L,
                "admin",
                "系统管理员",
                UserType.ADMIN,
                new LinkedHashSet<>(List.of(RoleCode.ADMIN)),
                Set.of("admin:patient:list", "admin:patient:view", "admin:patient:create", "admin:patient:update", "admin:patient:delete"),
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

        AdminPatientController target = new AdminPatientController(
                new ListAdminPatientsUseCase(new StubAdminPatientQueryRepository()),
                new GetAdminPatientDetailUseCase(new StubAdminPatientQueryRepository(), TestAuditSupport.auditTrailService()),
                new CreateAdminPatientUseCase(
                        new StubAdminPatientWriteRepository(), rawPassword -> "ignored", TestAuditSupport.auditTrailService()),
                new UpdateAdminPatientUseCase(new StubAdminPatientWriteRepository(), TestAuditSupport.auditTrailService()),
                new DeleteAdminPatientUseCase(
                        new StubAdminPatientQueryRepository(),
                        new StubAdminPatientWriteRepository(),
                        TestAuditSupport.auditTrailService()),
                TestAuditSupport.auditApiSupport());
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(target);
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAspect(new ScenarioAuthorizationAspect(
                new AuthorizationDecisionService(List.of(), List.of()),
                TestAuditSupport.auditApiSupport(),
                TestAuditSupport.emptyEncounterQueryRepository(),
                TestAuditSupport.emptyAdminPatientQueryRepository()));
        AdminPatientController controller = proxyFactory.getProxy();

        JsonAuthenticationEntryPoint authenticationEntryPoint = new JsonAuthenticationEntryPoint(objectMapper);
        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(
                accessTokenCodec,
                accessTokenBlocklistPort,
                repository,
                authenticationEntryPoint,
                objectMapper,
                new SecurityConfig().publicRequestMatcher(new ApiSecurityProperties(false)));

        Filter securityContextCleanupFilter = (request, response, chain) -> {
            try {
                chain.doFilter(request, response);
            } finally {
                SecurityContextHolder.clearContext();
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(), new ResultResponseBodyAdvice())
                .setMessageConverters(
                        new StringHttpMessageConverter(),
                        new MappingJackson2HttpMessageConverter(objectMapper))
                .addFilters(new RequestCorrelationFilter(), jwtAuthenticationFilter, securityContextCleanupFilter)
                .build();
    }

    @Test
    void list_WhenUnauthenticated_ReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/admin/patients"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void list_WhenMissingPermission_ReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/patients").header("Authorization", "Bearer " + PATIENT_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1003));
    }

    @Test
    void list_WhenAuthorized_ReturnPatientList() throws Exception {
        mockMvc.perform(get("/api/v1/admin/patients")
                        .param("keyword", "patient")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.pageNum").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.items[0].patientId").value(2208))
                .andExpect(jsonPath("$.data.items[0].username").value("patient_new"))
                .andExpect(jsonPath("$.data.items[0].displayName").value("李新患者"))
                .andExpect(jsonPath("$.data.items[0].accountStatus").value("ACTIVE"));
    }

    @Test
    void list_WhenExplicitPagingProvided_ReturnRequestedPaging() throws Exception {
        mockMvc.perform(get("/api/v1/admin/patients")
                        .param("pageNum", "2")
                        .param("pageSize", "5")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.pageNum").value(2))
                .andExpect(jsonPath("$.data.pageSize").value(5));
    }

    @Test
    void list_WhenPageNumExceedsMax_ReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/admin/patients")
                        .param("pageNum", String.valueOf(PageQuery.MAX_PAGE_NUM + 1L))
                        .header("Authorization", "Bearer " + ADMIN_TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value(1002));
    }

    @Test
    void detail_WhenAuthorized_ReturnPatientDetail() throws Exception {
        mockMvc.perform(get("/api/v1/admin/patients/2208").header("Authorization", "Bearer " + ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.patientId").value(2208))
                .andExpect(jsonPath("$.data.username").value("patient_new"))
                .andExpect(jsonPath("$.data.allergySummary").value("Peanut"));
    }

    @Test
    void create_WhenAuthorized_ReturnPatientDetail() throws Exception {
        mockMvc.perform(post("/api/v1/admin/patients")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "patient_new",
                                  "password": "patient123",
                                  "displayName": "李新患者",
                                  "mobileMasked": "137****1234",
                                  "gender": "FEMALE",
                                  "birthDate": "1995-06-01",
                                  "bloodType": "A",
                                  "allergySummary": "Peanut"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.patientId").value(2208))
                .andExpect(jsonPath("$.data.username").value("patient_new"));
    }

    @Test
    void update_WhenAuthorized_ReturnUpdatedPatientDetail() throws Exception {
        mockMvc.perform(put("/api/v1/admin/patients/2208")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "李修改",
                                  "mobileMasked": "137****9999",
                                  "gender": "MALE",
                                  "birthDate": "1990-01-02",
                                  "bloodType": "B",
                                  "allergySummary": "Dust"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.patientId").value(2208))
                .andExpect(jsonPath("$.data.displayName").value("李修改"))
                .andExpect(jsonPath("$.data.mobileMasked").value("137****9999"))
                .andExpect(jsonPath("$.data.bloodType").value("B"))
                .andExpect(jsonPath("$.data.allergySummary").value("Dust"));
    }

    @Test
    void delete_WhenAuthorized_ReturnSuccess() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/patients/2208").header("Authorization", "Bearer " + ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private static AdminPatientDetail detail() {
        return new AdminPatientDetail(
                2208L,
                2008L,
                "P20260008",
                "patient_new",
                "李新患者",
                "137****1234",
                "FEMALE",
                LocalDate.of(1995, 6, 1),
                "A",
                "Peanut",
                "ACTIVE");
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

    private static final class StubAdminPatientQueryRepository implements AdminPatientQueryRepository {

        @Override
        public PageData<AdminPatientListItem> pageByKeyword(String keyword, PageQuery pageQuery) {
            List<AdminPatientListItem> items = List.of(new AdminPatientListItem(
                    2208L, 2008L, "P20260008", "patient_new", "李新患者", "137****1234", "FEMALE", LocalDate.of(1995, 6, 1), "A", "ACTIVE"));
            return new PageData<>(items, pageQuery.pageNum(), pageQuery.pageSize(), 1L, 1L, false);
        }

        @Override
        public Optional<AdminPatientDetail> findDetailByPatientId(Long patientId) {
            return Optional.of(detail());
        }
    }

    private static final class StubAdminPatientWriteRepository implements AdminPatientWriteRepository {

        @Override
        public AdminPatientDetail create(me.jianwen.mediask.domain.user.model.AdminPatientCreateDraft draft) {
            return detail();
        }

        @Override
        public AdminPatientDetail update(Long patientId, me.jianwen.mediask.domain.user.model.AdminPatientUpdateDraft draft) {
            return new AdminPatientDetail(
                    patientId,
                    2008L,
                    "P20260008",
                    "patient_new",
                    draft.displayName(),
                    draft.mobileMasked(),
                    draft.gender(),
                    draft.birthDate(),
                    draft.bloodType(),
                    draft.allergySummary(),
                    "ACTIVE");
        }

        @Override
        public void softDelete(Long patientId) {}
    }
}
