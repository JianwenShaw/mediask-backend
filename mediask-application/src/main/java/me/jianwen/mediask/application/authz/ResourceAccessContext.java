package me.jianwen.mediask.application.authz;

import me.jianwen.mediask.common.util.ArgumentChecks;

public record ResourceAccessContext(Long ownerUserId, Long departmentId) {

    public ResourceAccessContext {
        ownerUserId = ArgumentChecks.normalizePositive(ownerUserId, "ownerUserId");
        departmentId = ArgumentChecks.normalizePositive(departmentId, "departmentId");
    }
}
