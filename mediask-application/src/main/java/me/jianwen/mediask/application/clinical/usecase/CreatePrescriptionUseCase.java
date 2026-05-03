package me.jianwen.mediask.application.clinical.usecase;

import java.util.List;
import me.jianwen.mediask.application.audit.AuditActionCodes;
import me.jianwen.mediask.application.audit.AuditResourceTypes;
import me.jianwen.mediask.application.audit.model.AuditContext;
import me.jianwen.mediask.application.clinical.command.CreatePrescriptionCommand;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.id.SnowflakeIdGenerator;
import me.jianwen.mediask.domain.clinical.exception.ClinicalErrorCode;
import me.jianwen.mediask.domain.clinical.model.PrescriptionItem;
import me.jianwen.mediask.domain.clinical.model.PrescriptionOrder;
import me.jianwen.mediask.domain.clinical.port.EmrRecordQueryRepository;
import me.jianwen.mediask.domain.clinical.port.EncounterQueryRepository;
import me.jianwen.mediask.domain.clinical.port.PrescriptionOrderRepository;
import org.springframework.transaction.annotation.Transactional;

public class CreatePrescriptionUseCase {

    private final PrescriptionOrderRepository prescriptionOrderRepository;
    private final EncounterQueryRepository encounterQueryRepository;
    private final EmrRecordQueryRepository emrRecordQueryRepository;
    private final me.jianwen.mediask.application.audit.usecase.AuditTrailService auditTrailService;

    public CreatePrescriptionUseCase(
            PrescriptionOrderRepository prescriptionOrderRepository,
            EncounterQueryRepository encounterQueryRepository,
            EmrRecordQueryRepository emrRecordQueryRepository,
            me.jianwen.mediask.application.audit.usecase.AuditTrailService auditTrailService) {
        this.prescriptionOrderRepository = prescriptionOrderRepository;
        this.encounterQueryRepository = encounterQueryRepository;
        this.emrRecordQueryRepository = emrRecordQueryRepository;
        this.auditTrailService = auditTrailService;
    }

    @Transactional
    public PrescriptionOrder handle(CreatePrescriptionCommand command, AuditContext auditContext) {
        var encounter = encounterQueryRepository.findDetailByEncounterId(command.encounterId())
                .orElseThrow(() -> new BizException(ClinicalErrorCode.PRESCRIPTION_ENCOUNTER_NOT_FOUND));

        if (!encounter.doctorId().equals(command.doctorId())) {
            throw new BizException(ClinicalErrorCode.PRESCRIPTION_ENCOUNTER_NOT_FOUND);
        }

        Long recordId = emrRecordQueryRepository.findRecordIdByEncounterId(command.encounterId())
                .orElseThrow(() -> new BizException(ClinicalErrorCode.PRESCRIPTION_EMR_RECORD_NOT_FOUND));

        if (prescriptionOrderRepository.existsByEncounterId(command.encounterId())) {
            throw new BizException(ClinicalErrorCode.PRESCRIPTION_ALREADY_EXISTS);
        }

        List<PrescriptionItem> items = command.items().stream()
                .map(item -> new PrescriptionItem(
                        SnowflakeIdGenerator.nextId(),
                        item.sortOrder(),
                        item.drugName(),
                        item.drugSpecification(),
                        item.dosageText(),
                        item.frequencyText(),
                        item.durationText(),
                        item.quantity(),
                        item.unit(),
                        item.route()))
                .toList();

        PrescriptionOrder prescriptionOrder = PrescriptionOrder.createDraft(
                generatePrescriptionNo(),
                recordId,
                command.encounterId(),
                encounter.patientSummary().patientUserId(),
                command.doctorId(),
                items);
        prescriptionOrderRepository.save(prescriptionOrder);
        auditTrailService.recordAuditSuccess(
                auditContext,
                AuditActionCodes.PRESCRIPTION_CREATE,
                AuditResourceTypes.PRESCRIPTION_ORDER,
                String.valueOf(prescriptionOrder.prescriptionOrderId()),
                prescriptionOrder.patientId(),
                prescriptionOrder.encounterId(),
                null);
        return prescriptionOrder;
    }

    private String generatePrescriptionNo() {
        return "RX" + SnowflakeIdGenerator.nextId();
    }
}
