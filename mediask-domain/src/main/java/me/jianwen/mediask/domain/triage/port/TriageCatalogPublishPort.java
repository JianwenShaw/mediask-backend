package me.jianwen.mediask.domain.triage.port;

import java.util.Optional;
import me.jianwen.mediask.domain.triage.model.CatalogVersion;
import me.jianwen.mediask.domain.triage.model.TriageCatalog;

public interface TriageCatalogPublishPort {

    void publish(TriageCatalog catalog);

    Optional<TriageCatalog> findActiveCatalog(String hospitalScope);

    Optional<TriageCatalog> findCatalogByVersion(String hospitalScope, CatalogVersion version);

    Optional<CatalogVersion> findActiveVersion(String hospitalScope);

    CatalogVersion nextVersion(String hospitalScope);
}
