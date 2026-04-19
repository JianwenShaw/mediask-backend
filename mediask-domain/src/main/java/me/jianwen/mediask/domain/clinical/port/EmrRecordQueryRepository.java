package me.jianwen.mediask.domain.clinical.port;

import java.util.Optional;
import me.jianwen.mediask.domain.clinical.model.EmrRecord;
import me.jianwen.mediask.domain.clinical.model.EmrRecordAccess;

public interface EmrRecordQueryRepository {

    Optional<EmrRecord> findByEncounterId(Long encounterId);

    Optional<Long> findRecordIdByEncounterId(Long encounterId);

    Optional<EmrRecordAccess> findAccessByRecordId(Long recordId);
}
