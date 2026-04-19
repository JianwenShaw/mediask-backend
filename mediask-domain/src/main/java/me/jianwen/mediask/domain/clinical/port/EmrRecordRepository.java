package me.jianwen.mediask.domain.clinical.port;

import me.jianwen.mediask.domain.clinical.model.EmrRecord;

public interface EmrRecordRepository {

    void save(EmrRecord emrRecord);

    boolean existsByEncounterId(Long encounterId);
}
