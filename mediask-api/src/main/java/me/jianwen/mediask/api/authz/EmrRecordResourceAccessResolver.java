package me.jianwen.mediask.api.authz;

import java.util.Optional;
import me.jianwen.mediask.application.authz.ResourceAccessContext;
import me.jianwen.mediask.application.authz.ResourceAccessResolverPort;
import me.jianwen.mediask.application.authz.ResourceType;
import me.jianwen.mediask.domain.clinical.port.EmrRecordQueryRepository;
import org.springframework.stereotype.Component;

@Component
public class EmrRecordResourceAccessResolver implements ResourceAccessResolverPort {

    private final EmrRecordQueryRepository emrRecordQueryRepository;

    public EmrRecordResourceAccessResolver(EmrRecordQueryRepository emrRecordQueryRepository) {
        this.emrRecordQueryRepository = emrRecordQueryRepository;
    }

    @Override
    public boolean supports(ResourceType resourceType) {
        return resourceType == ResourceType.EMR_RECORD;
    }

    @Override
    public Optional<ResourceAccessContext> resolve(Long resourceId) {
        return emrRecordQueryRepository.findAccessByRecordId(resourceId)
                .map(access -> new ResourceAccessContext(access.patientId(), access.departmentId()));
    }
}
