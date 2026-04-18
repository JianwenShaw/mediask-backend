package me.jianwen.mediask.api.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import me.jianwen.mediask.api.advice.ResultResponseBodyAdvice;
import me.jianwen.mediask.api.exception.GlobalExceptionHandler;
import me.jianwen.mediask.domain.ai.model.TriageDepartmentCandidate;
import me.jianwen.mediask.domain.ai.model.TriageDepartmentCatalog;
import me.jianwen.mediask.domain.ai.port.TriageDepartmentCatalogPort;
import me.jianwen.mediask.infra.ai.config.AiServiceProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class InternalTriageDepartmentCatalogControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        InternalTriageDepartmentCatalogController controller = new InternalTriageDepartmentCatalogController(
                hospitalScope -> new TriageDepartmentCatalog(
                        hospitalScope,
                        "deptcat-test-v1",
                        List.of(new TriageDepartmentCandidate(
                                101L,
                                "神经内科",
                                "headache, dizziness",
                                List.of("神内"),
                                10))),
                new AiServiceProperties(
                        URI.create("http://localhost:8000"),
                        "test-key",
                        Duration.ofSeconds(3),
                        Duration.ofSeconds(30),
                        Duration.ofMinutes(5)));
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(), new ResultResponseBodyAdvice())
                .build();
    }

    @Test
    void getCatalog_WhenAuthorized_ReturnSnakeCaseRawJson() throws Exception {
        mockMvc.perform(get("/api/v1/internal/triage-department-catalogs/default-hospital")
                        .header("X-API-Key", "test-key"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("X-Request-Id"))
                .andExpect(jsonPath("$.hospital_scope").value("default-hospital"))
                .andExpect(jsonPath("$.department_catalog_version").value("deptcat-test-v1"))
                .andExpect(jsonPath("$.department_candidates[0].department_id").value("101"))
                .andExpect(jsonPath("$.department_candidates[0].department_name").value("神经内科"))
                .andExpect(jsonPath("$.department_candidates[0].routing_hint").value("headache, dizziness"))
                .andExpect(jsonPath("$.department_candidates[0].aliases[0]").value("神内"))
                .andExpect(jsonPath("$.department_candidates[0].sort_order").value(10))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.requestId").doesNotExist())
                .andExpect(jsonPath("$.timestamp").doesNotExist());
    }

    @Test
    void getCatalog_WhenUnauthorized_Return401() throws Exception {
        mockMvc.perform(get("/api/v1/internal/triage-department-catalogs/default-hospital")
                        .header("X-API-Key", "wrong-key"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1001));
    }
}
