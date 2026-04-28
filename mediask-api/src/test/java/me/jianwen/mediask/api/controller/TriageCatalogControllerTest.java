package me.jianwen.mediask.api.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import me.jianwen.mediask.api.advice.ResultResponseBodyAdvice;
import me.jianwen.mediask.api.exception.GlobalExceptionHandler;
import me.jianwen.mediask.api.security.AuthenticatedUserPrincipal;
import me.jianwen.mediask.api.security.ScenarioAuthorizationAspect;
import me.jianwen.mediask.application.authz.AuthorizationDecisionService;
import me.jianwen.mediask.application.triage.usecase.PublishTriageCatalogUseCase;
import me.jianwen.mediask.domain.triage.model.CatalogVersion;
import me.jianwen.mediask.domain.triage.model.DepartmentCandidate;
import me.jianwen.mediask.domain.triage.model.TriageCatalog;
import me.jianwen.mediask.domain.triage.port.DepartmentCatalogSourcePort;
import me.jianwen.mediask.domain.triage.port.TriageCatalogPublishPort;
import me.jianwen.mediask.domain.user.model.AuthenticatedUser;
import me.jianwen.mediask.domain.user.model.RoleCode;
import me.jianwen.mediask.domain.user.model.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TriageCatalogControllerTest {

    private static final String ADMIN_PERMISSION = "admin:triage-catalog:publish";

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        StubPublishPort publishPort = new StubPublishPort();
        DepartmentCatalogSourcePort sourcePort = ignored -> List.of(
                new DepartmentCandidate(3101L, "神经内科", "神经内科相关问题优先考虑", List.of(), 10));
        TriageCatalogController target = new TriageCatalogController(
                new PublishTriageCatalogUseCase(publishPort, sourcePort));
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(target);
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAspect(new ScenarioAuthorizationAspect(new AuthorizationDecisionService(List.of(), List.of())));
        TriageCatalogController controller = proxyFactory.getProxy();

        Filter testAuthenticationFilter = (request, response, chain) -> {
            try {
                String permissionHeader = ((HttpServletRequest) request).getHeader("X-Test-Permission");
                Set<String> permissions = ADMIN_PERMISSION.equals(permissionHeader) ? Set.of(ADMIN_PERMISSION) : Set.of();
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
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .addFilters(testAuthenticationFilter)
                .build();
    }

    @Test
    void publish_WhenAdminHasPermission_ReturnsPublishResult() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/api/v1/admin/triage-catalog/publish")
                        .header("X-Test-Permission", ADMIN_PERMISSION)
                        .param("hospitalScope", "default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.catalogVersion").value(
                        "deptcat-v" + LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE) + "-01"))
                .andExpect(jsonPath("$.data.candidateCount").value(1))
                .andReturn();

        JsonNode data = objectMapper.readTree(mvcResult.getResponse().getContentAsString()).get("data");
        assertTrue(data.get("publishedAt").asText().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z"));
    }

    @Test
    void publish_WhenAdminMissingPermission_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/admin/triage-catalog/publish"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1003));
    }

    private static class StubPublishPort implements TriageCatalogPublishPort {

        @Override
        public void publish(TriageCatalog catalog) {
        }

        @Override
        public Optional<TriageCatalog> findActiveCatalog(String hospitalScope) {
            return Optional.empty();
        }

        @Override
        public Optional<TriageCatalog> findCatalogByVersion(String hospitalScope, CatalogVersion version) {
            return Optional.empty();
        }

        @Override
        public Optional<CatalogVersion> findActiveVersion(String hospitalScope) {
            return Optional.empty();
        }

        @Override
        public CatalogVersion nextVersion(String hospitalScope) {
            return CatalogVersion.of(LocalDate.now(), 1);
        }
    }
}
