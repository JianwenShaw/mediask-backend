package me.jianwen.mediask.api.authz;

import java.util.List;
import me.jianwen.mediask.application.authz.AuthzInvocationContext;
import me.jianwen.mediask.application.authz.ResourceRef;
import me.jianwen.mediask.application.authz.ResourceReferenceAssemblerPort;
import me.jianwen.mediask.application.authz.ResourceType;
import me.jianwen.mediask.application.authz.ScenarioCode;
import org.springframework.stereotype.Component;

@Component
public class EmrRecordResourceReferenceAssembler implements ResourceReferenceAssemblerPort {

    public EmrRecordResourceReferenceAssembler() {}

    @Override
    public boolean supports(ScenarioCode scenarioCode) {
        return scenarioCode == ScenarioCode.EMR_RECORD_READ;
    }

    @Override
    public List<ResourceRef> assemble(AuthzInvocationContext invocationContext) {
        Object encounterIdArgument = invocationContext.arguments().get("encounterId");
        if (!(encounterIdArgument instanceof Long encounterId) || encounterId <= 0L) {
            return List.of();
        }
        return List.of(new ResourceRef(ResourceType.EMR_RECORD, encounterId));
    }
}
