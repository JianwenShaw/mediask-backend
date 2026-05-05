package me.jianwen.mediask.domain.clinical.port;

import java.util.Optional;
import me.jianwen.mediask.domain.clinical.model.PrescriptionOrder;

public interface PrescriptionOrderQueryRepository {

    Optional<PrescriptionOrder> findByEncounterId(Long encounterId);

    Optional<PrescriptionOrder> findById(Long prescriptionOrderId);
}
