package me.jianwen.mediask.domain.clinical.port;

import java.util.Optional;
import java.util.List;
import me.jianwen.mediask.domain.clinical.model.EmrRecord;
import me.jianwen.mediask.domain.clinical.model.EmrRecordListItem;
import me.jianwen.mediask.domain.clinical.model.EmrRecordAccess;

public interface EmrRecordQueryRepository {

    Optional<EmrRecord> findByEncounterId(Long encounterId);

    List<EmrRecordListItem> listByPatientUserId(Long patientUserId, Long excludeEncounterId);

    Optional<Long> findRecordIdByEncounterId(Long encounterId);

    Optional<EmrRecordAccess> findAccessByRecordId(Long recordId);
}
