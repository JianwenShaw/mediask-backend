package me.jianwen.mediask.domain.clinical.port;

import me.jianwen.mediask.domain.clinical.model.PrescriptionOrder;

public interface PrescriptionOrderRepository {

    void save(PrescriptionOrder prescriptionOrder);

    boolean existsByEncounterId(Long encounterId);

    boolean update(PrescriptionOrder prescriptionOrder);
}
