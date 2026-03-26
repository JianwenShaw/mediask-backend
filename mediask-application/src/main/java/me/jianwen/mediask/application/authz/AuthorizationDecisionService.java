package me.jianwen.mediask.application.authz;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import me.jianwen.mediask.domain.user.model.DataScopeRule;
import me.jianwen.mediask.domain.user.model.DataScopeType;

public class AuthorizationDecisionService {

    private final List<ResourceReferenceAssemblerPort> resourceReferenceAssemblers;
    private final List<ResourceAccessResolverPort> resourceAccessResolvers;

    public AuthorizationDecisionService(
            List<ResourceReferenceAssemblerPort> resourceReferenceAssemblers,
            List<ResourceAccessResolverPort> resourceAccessResolvers) {
        this.resourceReferenceAssemblers = List.copyOf(
                resourceReferenceAssemblers == null ? List.of() : resourceReferenceAssemblers);
        this.resourceAccessResolvers = List.copyOf(resourceAccessResolvers == null ? List.of() : resourceAccessResolvers);
    }

    public AuthzDecision decide(AuthzInvocationContext invocationContext) {
        if (invocationContext == null || invocationContext.subject() == null) {
            return AuthzDecision.deny(AuthzDecisionReason.UNAUTHENTICATED);
        }

        ScenarioCode scenarioCode = invocationContext.scenarioCode();
        AuthzSubject subject = invocationContext.subject();
        if (!subject.hasPermission(scenarioCode.permissionCode())) {
            return AuthzDecision.deny(AuthzDecisionReason.MISSING_PERMISSION);
        }

        List<ResourceRef> resources;
        try {
            resources = assembleResources(invocationContext);
        } catch (RuntimeException exception) {
            return AuthzDecision.deny(AuthzDecisionReason.INVALID_RESOURCE_REFERENCE);
        }

        if (scenarioCode.objectScoped() && resources.isEmpty()) {
            return AuthzDecision.deny(AuthzDecisionReason.RESOURCE_REFERENCE_MISSING);
        }
        if (resources.isEmpty()) {
            return AuthzDecision.allow();
        }

        return evaluateResources(subject, scenarioCode.actionType(), scenarioCode.combinationMode(), resources);
    }

    private AuthzDecision evaluateResources(
            AuthzSubject subject, ActionType actionType, CombinationMode combinationMode, List<ResourceRef> resources) {
        if (combinationMode == CombinationMode.ALL) {
            for (ResourceRef resource : resources) {
                AuthzDecision singleDecision = evaluateSingleResource(subject, actionType, resource);
                if (!singleDecision.allowed()) {
                    return singleDecision;
                }
            }
            return AuthzDecision.allow();
        }

        AuthzDecisionReason lastDeniedReason = AuthzDecisionReason.OBJECT_SCOPE_DENIED;
        for (ResourceRef resource : resources) {
            AuthzDecision singleDecision = evaluateSingleResource(subject, actionType, resource);
            if (singleDecision.allowed()) {
                return AuthzDecision.allow();
            }
            lastDeniedReason = singleDecision.reason();
        }
        return AuthzDecision.deny(lastDeniedReason);
    }

    private AuthzDecision evaluateSingleResource(AuthzSubject subject, ActionType actionType, ResourceRef resourceRef) {
        ResourceAccessResolverPort resolver = resourceAccessResolvers.stream()
                .filter(candidate -> candidate.supports(resourceRef.resourceType()))
                .findFirst()
                .orElse(null);
        if (resolver == null) {
            return AuthzDecision.deny(AuthzDecisionReason.NO_RESOURCE_RESOLVER);
        }

        return resolver.resolve(resourceRef.resourceId())
                .map(resourceContext -> hasScopeAccess(subject, actionType, resourceRef, resourceContext)
                        ? AuthzDecision.allow()
                        : AuthzDecision.deny(AuthzDecisionReason.OBJECT_SCOPE_DENIED))
                .orElseGet(() -> AuthzDecision.deny(AuthzDecisionReason.RESOURCE_NOT_FOUND));
    }

    private boolean hasScopeAccess(
            AuthzSubject subject, ActionType actionType, ResourceRef resourceRef, ResourceAccessContext resourceContext) {
        for (DataScopeRule dataScopeRule : subject.dataScopeRules()) {
            if (!dataScopeRule.matchesResourceType(resourceRef.resourceType().code())) {
                continue;
            }
            DataScopeType scopeType = dataScopeRule.scopeType();
            if (scopeType == DataScopeType.ALL && !actionType.isWriteOperation()) {
                return true;
            }
            if (scopeType == DataScopeType.SELF
                    && Objects.equals(resourceContext.ownerUserId(), subject.userId())) {
                return true;
            }
            if (scopeType == DataScopeType.DEPARTMENT
                    && hasDepartmentScopeAccess(dataScopeRule, subject, resourceContext)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasDepartmentScopeAccess(
            DataScopeRule dataScopeRule, AuthzSubject subject, ResourceAccessContext resourceContext) {
        Long resourceDepartmentId = resourceContext.departmentId();
        if (resourceDepartmentId == null) {
            return false;
        }

        Long allowedDepartmentId = dataScopeRule.scopeDepartmentId();
        if (allowedDepartmentId == null) {
            allowedDepartmentId = subject.primaryDepartmentId();
        }
        return allowedDepartmentId != null && Objects.equals(resourceDepartmentId, allowedDepartmentId);
    }

    private List<ResourceRef> assembleResources(AuthzInvocationContext invocationContext) {
        ArrayList<ResourceRef> resources = new ArrayList<>();
        for (ResourceReferenceAssemblerPort assembler : resourceReferenceAssemblers) {
            if (!assembler.supports(invocationContext.scenarioCode())) {
                continue;
            }
            List<ResourceRef> assembledResources = assembler.assemble(invocationContext);
            if (assembledResources == null || assembledResources.isEmpty()) {
                continue;
            }
            resources.addAll(assembledResources);
        }
        return List.copyOf(resources);
    }
}
