package me.jianwen.mediask.api.controller;

import me.jianwen.mediask.api.dto.TriageDepartmentCatalogResponse;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.exception.ErrorCode;
import me.jianwen.mediask.domain.ai.port.TriageDepartmentCatalogPort;
import me.jianwen.mediask.infra.ai.config.AiServiceProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/triage-department-catalogs")
@ConditionalOnProperty(prefix = "mediask.ai.service", name = {"base-url", "api-key"})
public class InternalTriageDepartmentCatalogController {

    private final TriageDepartmentCatalogPort triageDepartmentCatalogPort;
    private final AiServiceProperties aiServiceProperties;

    public InternalTriageDepartmentCatalogController(
            TriageDepartmentCatalogPort triageDepartmentCatalogPort, AiServiceProperties aiServiceProperties) {
        this.triageDepartmentCatalogPort = triageDepartmentCatalogPort;
        this.aiServiceProperties = aiServiceProperties;
    }

    @GetMapping("/{hospitalScope}")
    public TriageDepartmentCatalogResponse getCatalog(
            @PathVariable String hospitalScope, @RequestHeader("X-API-Key") String apiKey) {
        if (!aiServiceProperties.apiKey().equals(apiKey)) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        var catalog = triageDepartmentCatalogPort.getCatalog(hospitalScope);
        return new TriageDepartmentCatalogResponse(
                catalog.hospitalScope(),
                catalog.departmentCatalogVersion(),
                catalog.departmentCandidates().stream()
                        .map(candidate -> new TriageDepartmentCatalogResponse.TriageDepartmentCandidateResponse(
                                candidate.departmentId(),
                                candidate.departmentName(),
                                candidate.routingHint(),
                                candidate.aliases(),
                                candidate.sortOrder()))
                        .toList());
    }
}
