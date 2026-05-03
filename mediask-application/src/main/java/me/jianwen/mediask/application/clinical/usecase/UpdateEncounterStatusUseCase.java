package me.jianwen.mediask.application.clinical.usecase;

import java.time.OffsetDateTime;
import me.jianwen.mediask.application.audit.AuditActionCodes;
import me.jianwen.mediask.application.audit.AuditResourceTypes;
import me.jianwen.mediask.application.audit.model.AuditContext;
import me.jianwen.mediask.application.clinical.command.UpdateEncounterStatusCommand;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.clinical.exception.ClinicalErrorCode;
import me.jianwen.mediask.domain.clinical.model.VisitEncounterStatus;
import me.jianwen.mediask.domain.clinical.port.EncounterQueryRepository;
import me.jianwen.mediask.domain.clinical.port.VisitEncounterRepository;
import me.jianwen.mediask.domain.outpatient.port.RegistrationOrderRepository;
import org.springframework.transaction.annotation.Transactional;

public class UpdateEncounterStatusUseCase {

    private final EncounterQueryRepository encounterQueryRepository;
    private final VisitEncounterRepository visitEncounterRepository;
    private final RegistrationOrderRepository registrationOrderRepository;
    private final me.jianwen.mediask.application.audit.usecase.AuditTrailService auditTrailService;

    public UpdateEncounterStatusUseCase(
            EncounterQueryRepository encounterQueryRepository,
            VisitEncounterRepository visitEncounterRepository,
            RegistrationOrderRepository registrationOrderRepository,
            me.jianwen.mediask.application.audit.usecase.AuditTrailService auditTrailService) {
        this.encounterQueryRepository = encounterQueryRepository;
        this.visitEncounterRepository = visitEncounterRepository;
        this.registrationOrderRepository = registrationOrderRepository;
        this.auditTrailService = auditTrailService;
    }

    @Transactional
    public UpdateEncounterStatusResult handle(UpdateEncounterStatusCommand command, AuditContext auditContext) {
        var encounter = encounterQueryRepository
                .findDetailByEncounterId(command.encounterId())
                .orElseThrow(() -> new BizException(ClinicalErrorCode.ENCOUNTER_NOT_FOUND));
        if (!encounter.doctorId().equals(command.doctorId())) {
            throw new BizException(ClinicalErrorCode.ENCOUNTER_ACCESS_DENIED);
        }

        OffsetDateTime now = OffsetDateTime.now();
        UpdateEncounterStatusResult result = switch (command.action()) {
            case START -> startEncounter(command.encounterId(), encounter.patientSummary().encounterStatus(), now);
            case COMPLETE -> completeEncounter(
                    command.encounterId(),
                    encounter.registrationId(),
                    encounter.patientSummary().encounterStatus(),
                    encounter.patientSummary().startedAt(),
                    now);
        };
        auditTrailService.recordAuditSuccess(
                auditContext,
                AuditActionCodes.ENCOUNTER_UPDATE,
                AuditResourceTypes.ENCOUNTER,
                String.valueOf(command.encounterId()),
                encounter.patientSummary().patientUserId(),
                command.encounterId(),
                command.action().name());
        return result;
    }

    private UpdateEncounterStatusResult startEncounter(
            Long encounterId, VisitEncounterStatus currentStatus, OffsetDateTime startedAt) {
        if (currentStatus != VisitEncounterStatus.SCHEDULED) {
            throw new BizException(ClinicalErrorCode.ENCOUNTER_STATUS_TRANSITION_NOT_ALLOWED);
        }
        if (!visitEncounterRepository.startScheduledByEncounterId(encounterId, startedAt)) {
            throw new BizException(ClinicalErrorCode.ENCOUNTER_STATUS_UPDATE_CONFLICT);
        }
        return new UpdateEncounterStatusResult(encounterId, VisitEncounterStatus.IN_PROGRESS, startedAt, null);
    }

    private UpdateEncounterStatusResult completeEncounter(
            Long encounterId,
            Long registrationId,
            VisitEncounterStatus currentStatus,
            OffsetDateTime startedAt,
            OffsetDateTime endedAt) {
        if (currentStatus != VisitEncounterStatus.IN_PROGRESS) {
            throw new BizException(ClinicalErrorCode.ENCOUNTER_STATUS_TRANSITION_NOT_ALLOWED);
        }
        if (!visitEncounterRepository.completeInProgressByEncounterId(encounterId, endedAt)) {
            throw new BizException(ClinicalErrorCode.ENCOUNTER_STATUS_UPDATE_CONFLICT);
        }
        if (!registrationOrderRepository.completeConfirmedByRegistrationId(registrationId)) {
            throw new BizException(ClinicalErrorCode.ENCOUNTER_REGISTRATION_SYNC_CONFLICT);
        }
        return new UpdateEncounterStatusResult(encounterId, VisitEncounterStatus.COMPLETED, startedAt, endedAt);
    }
}
