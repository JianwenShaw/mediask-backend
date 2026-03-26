package me.jianwen.mediask.application.authz;

import java.util.Objects;

public record ResourceRef(ResourceType resourceType, Long resourceId) {

    public ResourceRef {
        resourceType = Objects.requireNonNull(resourceType, "resourceType must not be null");
        if (resourceId == null || resourceId <= 0L) {
            throw new IllegalArgumentException("resourceId must be greater than 0");
        }
    }
}
