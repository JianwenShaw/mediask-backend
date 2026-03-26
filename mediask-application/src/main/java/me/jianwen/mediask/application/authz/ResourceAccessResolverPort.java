package me.jianwen.mediask.application.authz;

import java.util.Optional;

public interface ResourceAccessResolverPort {

    boolean supports(ResourceType resourceType);

    Optional<ResourceAccessContext> resolve(Long resourceId);
}
