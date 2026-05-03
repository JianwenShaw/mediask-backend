package me.jianwen.mediask.api.authz;

import java.util.Optional;
import me.jianwen.mediask.application.authz.ResourceAccessContext;
import me.jianwen.mediask.application.authz.ResourceAccessResolverPort;
import me.jianwen.mediask.application.authz.ResourceType;
import me.jianwen.mediask.domain.clinical.port.EncounterQueryRepository;
import org.springframework.stereotype.Component;

@Component
public class PrescriptionResourceAccessResolver implements ResourceAccessResolverPort {

    private final EncounterQueryRepository encounterQueryRepository;

    public PrescriptionResourceAccessResolver(EncounterQueryRepository encounterQueryRepository) {
        this.encounterQueryRepository = encounterQueryRepository;
    }

    @Override
    public boolean supports(ResourceType resourceType) {
        return resourceType == ResourceType.PRESCRIPTION_ORDER;
    }

    @Override
    public Optional<ResourceAccessContext> resolve(Long resourceId) {
        return encounterQueryRepository.findDetailByEncounterId(resourceId)
                .map(encounter -> new ResourceAccessContext(
                        encounter.patientSummary().patientUserId(),
                        encounter.patientSummary().departmentId()));
    }
}
