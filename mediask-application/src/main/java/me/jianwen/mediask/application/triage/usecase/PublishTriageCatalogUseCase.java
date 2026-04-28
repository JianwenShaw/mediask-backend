package me.jianwen.mediask.application.triage.usecase;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import me.jianwen.mediask.application.triage.command.PublishCatalogCommand;
import me.jianwen.mediask.application.triage.result.PublishCatalogResult;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.triage.exception.TriageErrorCode;
import me.jianwen.mediask.domain.triage.model.CatalogVersion;
import me.jianwen.mediask.domain.triage.model.DepartmentCandidate;
import me.jianwen.mediask.domain.triage.model.TriageCatalog;
import me.jianwen.mediask.domain.triage.port.DepartmentCatalogSourcePort;
import me.jianwen.mediask.domain.triage.port.TriageCatalogPublishPort;

public class PublishTriageCatalogUseCase {

    private final TriageCatalogPublishPort publishPort;
    private final DepartmentCatalogSourcePort sourcePort;

    public PublishTriageCatalogUseCase(
            TriageCatalogPublishPort publishPort, DepartmentCatalogSourcePort sourcePort) {
        this.publishPort = publishPort;
        this.sourcePort = sourcePort;
    }

    public PublishCatalogResult handle(PublishCatalogCommand command) {
        String scope = command.hospitalScope();

        List<DepartmentCandidate> candidates = sourcePort.loadCandidates(scope);
        if (candidates.isEmpty()) {
            throw new BizException(TriageErrorCode.NO_DEPARTMENTS_CONFIGURED);
        }

        CatalogVersion version = publishPort.nextVersion(scope);
        OffsetDateTime publishedAt = OffsetDateTime.now(ZoneOffset.UTC);

        TriageCatalog catalog = new TriageCatalog(scope, version, publishedAt, candidates);
        publishPort.publish(catalog);

        return new PublishCatalogResult(version.value(), candidates.size(), publishedAt);
    }
}
