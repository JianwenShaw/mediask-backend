package me.jianwen.mediask.domain.ai.port;

import me.jianwen.mediask.domain.ai.model.TriageDepartmentCatalog;

public interface TriageDepartmentCatalogPort {

    TriageDepartmentCatalog getCatalog(String hospitalScope);
}
