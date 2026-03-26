package me.jianwen.mediask.application.authz;

import java.util.List;

public interface ResourceReferenceAssemblerPort {

    boolean supports(ScenarioCode scenarioCode);

    List<ResourceRef> assemble(AuthzInvocationContext invocationContext);
}
