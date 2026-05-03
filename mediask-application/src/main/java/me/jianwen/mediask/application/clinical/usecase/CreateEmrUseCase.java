package me.jianwen.mediask.application.clinical.usecase;

import me.jianwen.mediask.application.audit.AuditActionCodes;
import me.jianwen.mediask.application.audit.AuditResourceTypes;
import me.jianwen.mediask.application.audit.model.AuditContext;
import me.jianwen.mediask.application.clinical.command.CreateEmrCommand;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.id.SnowflakeIdGenerator;
import me.jianwen.mediask.domain.clinical.exception.ClinicalErrorCode;
import me.jianwen.mediask.domain.clinical.model.EmrDiagnosis;
import me.jianwen.mediask.domain.clinical.model.EmrRecord;
import me.jianwen.mediask.domain.clinical.port.EmrRecordRepository;
import me.jianwen.mediask.domain.clinical.port.EncounterQueryRepository;
import org.springframework.transaction.annotation.Transactional;

public class CreateEmrUseCase {

    private final EmrRecordRepository emrRecordRepository;
    private final EncounterQueryRepository encounterQueryRepository;
    private final me.jianwen.mediask.application.audit.usecase.AuditTrailService auditTrailService;

    public CreateEmrUseCase(
            EmrRecordRepository emrRecordRepository,
            EncounterQueryRepository encounterQueryRepository,
            me.jianwen.mediask.application.audit.usecase.AuditTrailService auditTrailService) {
        this.emrRecordRepository = emrRecordRepository;
        this.encounterQueryRepository = encounterQueryRepository;
        this.auditTrailService = auditTrailService;
    }

    @Transactional
    public EmrRecord handle(CreateEmrCommand command, AuditContext auditContext) {
        // Validate encounter exists and belongs to the doctor
        var encounter = encounterQueryRepository
                .findDetailByEncounterId(command.encounterId())
                .orElseThrow(() -> new BizException(ClinicalErrorCode.EMR_ENCOUNTER_NOT_FOUND));

        if (!encounter.doctorId().equals(command.doctorId())) {
            throw new BizException(ClinicalErrorCode.EMR_ENCOUNTER_NOT_FOUND);
        }

        // TODO: add unique constraint on (encounter_id, deleted_at) to prevent TOCTOU race
        if (emrRecordRepository.existsByEncounterId(command.encounterId())) {
            throw new BizException(ClinicalErrorCode.EMR_RECORD_ALREADY_EXISTS);
        }

        // Convert command diagnoses to domain diagnoses
        var diagnoses = command.diagnoses().stream()
                .map(cmd -> new EmrDiagnosis(
                        cmd.diagnosisType(),
                        cmd.diagnosisCode(),
                        cmd.diagnosisName(),
                        cmd.isPrimary(),
                        cmd.sortOrder()))
                .toList();

        // Generate record number
        String recordNo = generateRecordNo();

        // Create EMR record
        EmrRecord emrRecord = EmrRecord.createDraft(
                recordNo,
                command.encounterId(),
                encounter.patientSummary().patientUserId(),
                command.doctorId(),
                encounter.patientSummary().departmentId(),
                command.chiefComplaintSummary(),
                command.content(),
                diagnoses);

        // Save EMR record
        emrRecordRepository.save(emrRecord);
        auditTrailService.recordAuditSuccess(
                auditContext,
                AuditActionCodes.EMR_CREATE,
                AuditResourceTypes.EMR_RECORD,
                String.valueOf(emrRecord.recordId()),
                emrRecord.patientId(),
                emrRecord.encounterId(),
                null);

        return emrRecord;
    }

    private String generateRecordNo() {
        return "EMR" + SnowflakeIdGenerator.nextId();
    }
}
