package me.jianwen.mediask.application.authz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import me.jianwen.mediask.domain.user.model.DataScopeRule;
import me.jianwen.mediask.domain.user.model.DataScopeType;
import org.junit.jupiter.api.Test;

class AuthorizationDecisionServiceTest {

    @Test
    void decide_WhenPermissionMatchesAndNoObjectScope_Allow() {
        AuthorizationDecisionService decisionService = new AuthorizationDecisionService(List.of(), List.of());
        AuthzDecision decision = decisionService.decide(new AuthzInvocationContext(
                ScenarioCode.DOCTOR_SELF_PROFILE_VIEW,
                new AuthzSubject(2003L, Set.of("doctor:profile:view:self"), Set.of(), null),
                Map.of()));

        assertTrue(decision.allowed());
        assertEquals(AuthzDecisionReason.ALLOWED, decision.reason());
    }

    @Test
    void decide_WhenPermissionMissing_Deny() {
        AuthorizationDecisionService decisionService = new AuthorizationDecisionService(List.of(), List.of());
        AuthzDecision decision = decisionService.decide(new AuthzInvocationContext(
                ScenarioCode.PATIENT_SELF_PROFILE_VIEW,
                new AuthzSubject(2003L, Set.of("auth:refresh"), Set.of(), null),
                Map.of()));

        assertEquals(AuthzDecisionReason.MISSING_PERMISSION, decision.reason());
    }

    @Test
    void decide_WhenObjectScopedScenarioAndResourceReferenceMissing_Deny() {
        ResourceReferenceAssemblerPort assembler = new ResourceReferenceAssemblerPort() {
            @Override
            public boolean supports(ScenarioCode scenarioCode) {
                return scenarioCode == ScenarioCode.EMR_RECORD_READ;
            }

            @Override
            public List<ResourceRef> assemble(AuthzInvocationContext invocationContext) {
                return List.of();
            }
        };
        AuthorizationDecisionService decisionService = new AuthorizationDecisionService(List.of(assembler), List.of());
        AuthzDecision decision = decisionService.decide(new AuthzInvocationContext(
                ScenarioCode.EMR_RECORD_READ,
                new AuthzSubject(
                        2003L,
                        Set.of("emr:read"),
                        Set.of(new DataScopeRule("EMR_RECORD", DataScopeType.SELF, null)),
                        3103L),
                Map.of("encounterId", 1001L)));

        assertEquals(AuthzDecisionReason.RESOURCE_REFERENCE_MISSING, decision.reason());
    }

    @Test
    void decide_WhenObjectScopeScenarioAndSelfScopeMatched_Allow() {
        ResourceReferenceAssemblerPort assembler = new ResourceReferenceAssemblerPort() {
            @Override
            public boolean supports(ScenarioCode scenarioCode) {
                return scenarioCode == ScenarioCode.EMR_RECORD_READ;
            }

            @Override
            public List<ResourceRef> assemble(AuthzInvocationContext invocationContext) {
                return List.of(new ResourceRef(ResourceType.EMR_RECORD, 1001L));
            }
        };
        ResourceAccessResolverPort resolver = new ResourceAccessResolverPort() {
            @Override
            public boolean supports(ResourceType resourceType) {
                return resourceType == ResourceType.EMR_RECORD;
            }

            @Override
            public Optional<ResourceAccessContext> resolve(Long resourceId) {
                return Optional.of(new ResourceAccessContext(2003L, 3103L));
            }
        };
        AuthorizationDecisionService decisionService = new AuthorizationDecisionService(List.of(assembler), List.of(resolver));
        AuthzDecision decision = decisionService.decide(new AuthzInvocationContext(
                ScenarioCode.EMR_RECORD_READ,
                new AuthzSubject(
                        2003L,
                        Set.of("emr:read"),
                        Set.of(new DataScopeRule("EMR_RECORD", DataScopeType.SELF, null)),
                        3103L),
                Map.of("id", 1001L)));

        assertTrue(decision.allowed());
        assertEquals(AuthzDecisionReason.ALLOWED, decision.reason());
    }

    @Test
    void decide_WhenWriteActionOnlyHasAllScope_Deny() {
        ResourceReferenceAssemblerPort assembler = new ResourceReferenceAssemblerPort() {
            @Override
            public boolean supports(ScenarioCode scenarioCode) {
                return scenarioCode == ScenarioCode.EMR_RECORD_UPDATE;
            }

            @Override
            public List<ResourceRef> assemble(AuthzInvocationContext invocationContext) {
                return List.of(new ResourceRef(ResourceType.EMR_RECORD, 1001L));
            }
        };
        ResourceAccessResolverPort resolver = new ResourceAccessResolverPort() {
            @Override
            public boolean supports(ResourceType resourceType) {
                return resourceType == ResourceType.EMR_RECORD;
            }

            @Override
            public Optional<ResourceAccessContext> resolve(Long resourceId) {
                return Optional.of(new ResourceAccessContext(2009L, 3201L));
            }
        };
        AuthorizationDecisionService decisionService = new AuthorizationDecisionService(List.of(assembler), List.of(resolver));
        AuthzDecision decision = decisionService.decide(new AuthzInvocationContext(
                ScenarioCode.EMR_RECORD_UPDATE,
                new AuthzSubject(
                        2003L,
                        Set.of("emr:update"),
                        Set.of(new DataScopeRule("EMR_RECORD", DataScopeType.ALL, null)),
                        3103L),
                Map.of("id", 1001L)));

        assertEquals(AuthzDecisionReason.OBJECT_SCOPE_DENIED, decision.reason());
    }

    @Test
    void decide_WhenDepartmentScopeUsesRuleDepartmentId_AllowEvenIfPrimaryDepartmentDiffers() {
        ResourceReferenceAssemblerPort assembler = new ResourceReferenceAssemblerPort() {
            @Override
            public boolean supports(ScenarioCode scenarioCode) {
                return scenarioCode == ScenarioCode.EMR_RECORD_READ;
            }

            @Override
            public List<ResourceRef> assemble(AuthzInvocationContext invocationContext) {
                return List.of(new ResourceRef(ResourceType.EMR_RECORD, 1001L));
            }
        };
        ResourceAccessResolverPort resolver = new ResourceAccessResolverPort() {
            @Override
            public boolean supports(ResourceType resourceType) {
                return resourceType == ResourceType.EMR_RECORD;
            }

            @Override
            public Optional<ResourceAccessContext> resolve(Long resourceId) {
                return Optional.of(new ResourceAccessContext(2010L, 5201L));
            }
        };
        AuthorizationDecisionService decisionService = new AuthorizationDecisionService(List.of(assembler), List.of(resolver));
        AuthzDecision decision = decisionService.decide(new AuthzInvocationContext(
                ScenarioCode.EMR_RECORD_READ,
                new AuthzSubject(
                        2003L,
                        Set.of("emr:read"),
                        Set.of(new DataScopeRule("EMR_RECORD", DataScopeType.DEPARTMENT, 5201L)),
                        3103L),
                Map.of("id", 1001L)));

        assertTrue(decision.allowed());
        assertEquals(AuthzDecisionReason.ALLOWED, decision.reason());
    }

    @Test
    void decide_WhenDepartmentScopeAndResourceDepartmentMissing_Deny() {
        ResourceReferenceAssemblerPort assembler = new ResourceReferenceAssemblerPort() {
            @Override
            public boolean supports(ScenarioCode scenarioCode) {
                return scenarioCode == ScenarioCode.EMR_RECORD_READ;
            }

            @Override
            public List<ResourceRef> assemble(AuthzInvocationContext invocationContext) {
                return List.of(new ResourceRef(ResourceType.EMR_RECORD, 1001L));
            }
        };
        ResourceAccessResolverPort resolver = new ResourceAccessResolverPort() {
            @Override
            public boolean supports(ResourceType resourceType) {
                return resourceType == ResourceType.EMR_RECORD;
            }

            @Override
            public Optional<ResourceAccessContext> resolve(Long resourceId) {
                return Optional.of(new ResourceAccessContext(2010L, null));
            }
        };
        AuthorizationDecisionService decisionService = new AuthorizationDecisionService(List.of(assembler), List.of(resolver));
        AuthzDecision decision = decisionService.decide(new AuthzInvocationContext(
                ScenarioCode.EMR_RECORD_READ,
                new AuthzSubject(
                        2003L,
                        Set.of("emr:read"),
                        Set.of(new DataScopeRule("EMR_RECORD", DataScopeType.DEPARTMENT, 5201L)),
                        3103L),
                Map.of("id", 1001L)));

        assertEquals(AuthzDecisionReason.OBJECT_SCOPE_DENIED, decision.reason());
    }

    @Test
    void decide_WhenDepartmentScopeWithoutRuleDepartmentAndPrimaryDepartmentMissing_Deny() {
        ResourceReferenceAssemblerPort assembler = new ResourceReferenceAssemblerPort() {
            @Override
            public boolean supports(ScenarioCode scenarioCode) {
                return scenarioCode == ScenarioCode.EMR_RECORD_READ;
            }

            @Override
            public List<ResourceRef> assemble(AuthzInvocationContext invocationContext) {
                return List.of(new ResourceRef(ResourceType.EMR_RECORD, 1001L));
            }
        };
        ResourceAccessResolverPort resolver = new ResourceAccessResolverPort() {
            @Override
            public boolean supports(ResourceType resourceType) {
                return resourceType == ResourceType.EMR_RECORD;
            }

            @Override
            public Optional<ResourceAccessContext> resolve(Long resourceId) {
                return Optional.of(new ResourceAccessContext(2010L, 5201L));
            }
        };
        AuthorizationDecisionService decisionService = new AuthorizationDecisionService(List.of(assembler), List.of(resolver));
        AuthzDecision decision = decisionService.decide(new AuthzInvocationContext(
                ScenarioCode.EMR_RECORD_READ,
                new AuthzSubject(
                        2003L,
                        Set.of("emr:read"),
                        Set.of(new DataScopeRule("EMR_RECORD", DataScopeType.DEPARTMENT, null)),
                        null),
                Map.of("id", 1001L)));

        assertEquals(AuthzDecisionReason.OBJECT_SCOPE_DENIED, decision.reason());
    }

    @Test
    void decide_WhenDepartmentScopeAndBothDepartmentsMissing_Deny() {
        ResourceReferenceAssemblerPort assembler = new ResourceReferenceAssemblerPort() {
            @Override
            public boolean supports(ScenarioCode scenarioCode) {
                return scenarioCode == ScenarioCode.EMR_RECORD_READ;
            }

            @Override
            public List<ResourceRef> assemble(AuthzInvocationContext invocationContext) {
                return List.of(new ResourceRef(ResourceType.EMR_RECORD, 1001L));
            }
        };
        ResourceAccessResolverPort resolver = new ResourceAccessResolverPort() {
            @Override
            public boolean supports(ResourceType resourceType) {
                return resourceType == ResourceType.EMR_RECORD;
            }

            @Override
            public Optional<ResourceAccessContext> resolve(Long resourceId) {
                return Optional.of(new ResourceAccessContext(2010L, null));
            }
        };
        AuthorizationDecisionService decisionService = new AuthorizationDecisionService(List.of(assembler), List.of(resolver));
        AuthzDecision decision = decisionService.decide(new AuthzInvocationContext(
                ScenarioCode.EMR_RECORD_READ,
                new AuthzSubject(
                        2003L,
                        Set.of("emr:read"),
                        Set.of(new DataScopeRule("EMR_RECORD", DataScopeType.DEPARTMENT, null)),
                        null),
                Map.of("id", 1001L)));

        assertEquals(AuthzDecisionReason.OBJECT_SCOPE_DENIED, decision.reason());
    }
}
