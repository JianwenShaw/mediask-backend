package me.jianwen.mediask.api.authz;

import java.util.List;
import me.jianwen.mediask.application.authz.AuthzInvocationContext;
import me.jianwen.mediask.application.authz.ResourceRef;
import me.jianwen.mediask.application.authz.ResourceReferenceAssemblerPort;
import me.jianwen.mediask.application.authz.ResourceType;
import me.jianwen.mediask.application.authz.ScenarioCode;
import me.jianwen.mediask.domain.clinical.port.EmrRecordQueryRepository;
import org.springframework.stereotype.Component;

@Component
public class EmrRecordResourceReferenceAssembler implements ResourceReferenceAssemblerPort {

    private final EmrRecordQueryRepository emrRecordQueryRepository;

    public EmrRecordResourceReferenceAssembler(EmrRecordQueryRepository emrRecordQueryRepository) {
        this.emrRecordQueryRepository = emrRecordQueryRepository;
    }

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
        return emrRecordQueryRepository.findRecordIdByEncounterId(encounterId)
                .map(recordId -> List.of(new ResourceRef(ResourceType.EMR_RECORD, recordId)))
                .orElseGet(List::of);
    }
}
