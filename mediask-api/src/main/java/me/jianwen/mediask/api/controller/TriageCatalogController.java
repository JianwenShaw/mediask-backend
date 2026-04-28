package me.jianwen.mediask.api.controller;

import me.jianwen.mediask.api.dto.PublishCatalogResponse;
import me.jianwen.mediask.application.authz.AuthorizeScenario;
import me.jianwen.mediask.application.authz.ScenarioCode;
import me.jianwen.mediask.application.triage.command.PublishCatalogCommand;
import me.jianwen.mediask.application.triage.result.PublishCatalogResult;
import me.jianwen.mediask.application.triage.usecase.PublishTriageCatalogUseCase;
import me.jianwen.mediask.common.result.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/triage-catalog")
public class TriageCatalogController {

    private final PublishTriageCatalogUseCase publishTriageCatalogUseCase;

    public TriageCatalogController(PublishTriageCatalogUseCase publishTriageCatalogUseCase) {
        this.publishTriageCatalogUseCase = publishTriageCatalogUseCase;
    }

    @PostMapping("/publish")
    @AuthorizeScenario(ScenarioCode.ADMIN_TRIAGE_CATALOG_PUBLISH)
    public Result<PublishCatalogResponse> publish(
            @RequestParam(defaultValue = "default") String hospitalScope) {
        PublishCatalogResult result = publishTriageCatalogUseCase.handle(
                new PublishCatalogCommand(hospitalScope));
        return Result.ok(new PublishCatalogResponse(
                result.catalogVersion(),
                result.candidateCount(),
                result.publishedAt()));
    }
}
