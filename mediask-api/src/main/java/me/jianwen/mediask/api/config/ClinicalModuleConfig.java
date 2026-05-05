package me.jianwen.mediask.api.config;

import me.jianwen.mediask.application.audit.usecase.AuditTrailService;
import me.jianwen.mediask.application.clinical.usecase.CancelPrescriptionUseCase;
import me.jianwen.mediask.application.clinical.usecase.CreateEmrUseCase;
import me.jianwen.mediask.application.clinical.usecase.CreatePrescriptionUseCase;
import me.jianwen.mediask.application.clinical.usecase.GetEncounterAiSummaryUseCase;
import me.jianwen.mediask.application.clinical.usecase.GetEncounterDetailUseCase;
import me.jianwen.mediask.application.clinical.usecase.GetEmrDetailUseCase;
import me.jianwen.mediask.application.clinical.usecase.GetPrescriptionDetailUseCase;
import me.jianwen.mediask.application.clinical.usecase.IssuePrescriptionUseCase;
import me.jianwen.mediask.application.clinical.usecase.ListEncountersUseCase;
import me.jianwen.mediask.application.clinical.usecase.UpdateEncounterStatusUseCase;
import me.jianwen.mediask.application.clinical.usecase.UpdatePrescriptionItemsUseCase;
import me.jianwen.mediask.domain.clinical.port.EmrRecordQueryRepository;
import me.jianwen.mediask.domain.clinical.port.EmrRecordRepository;
import me.jianwen.mediask.domain.clinical.port.EncounterQueryRepository;
import me.jianwen.mediask.domain.clinical.port.PrescriptionOrderQueryRepository;
import me.jianwen.mediask.domain.clinical.port.PrescriptionOrderRepository;
import me.jianwen.mediask.domain.clinical.port.VisitEncounterRepository;
import me.jianwen.mediask.domain.ai.port.AiTriageGatewayPort;
import me.jianwen.mediask.domain.outpatient.port.RegistrationOrderQueryRepository;
import me.jianwen.mediask.domain.outpatient.port.RegistrationOrderRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClinicalModuleConfig {

    @Bean
    public ListEncountersUseCase listEncountersUseCase(EncounterQueryRepository encounterQueryRepository) {
        return new ListEncountersUseCase(encounterQueryRepository);
    }

    @Bean
    public GetEncounterDetailUseCase getEncounterDetailUseCase(EncounterQueryRepository encounterQueryRepository) {
        return new GetEncounterDetailUseCase(encounterQueryRepository);
    }

    @Bean
    public GetEncounterAiSummaryUseCase getEncounterAiSummaryUseCase(
            EncounterQueryRepository encounterQueryRepository,
            RegistrationOrderQueryRepository registrationOrderQueryRepository,
            AiTriageGatewayPort aiTriageGatewayPort,
            AuditTrailService auditTrailService) {
        return new GetEncounterAiSummaryUseCase(
                encounterQueryRepository,
                registrationOrderQueryRepository,
                aiTriageGatewayPort,
                auditTrailService);
    }

    @Bean
    public UpdateEncounterStatusUseCase updateEncounterStatusUseCase(
            EncounterQueryRepository encounterQueryRepository,
            VisitEncounterRepository visitEncounterRepository,
            RegistrationOrderRepository registrationOrderRepository,
            AuditTrailService auditTrailService) {
        return new UpdateEncounterStatusUseCase(
                encounterQueryRepository, visitEncounterRepository, registrationOrderRepository, auditTrailService);
    }

    @Bean
    public CreateEmrUseCase createEmrUseCase(
            EmrRecordRepository emrRecordRepository,
            EncounterQueryRepository encounterQueryRepository,
            AuditTrailService auditTrailService) {
        return new CreateEmrUseCase(emrRecordRepository, encounterQueryRepository, auditTrailService);
    }

    @Bean
    public GetEmrDetailUseCase getEmrDetailUseCase(
            EmrRecordQueryRepository emrRecordQueryRepository, AuditTrailService auditTrailService) {
        return new GetEmrDetailUseCase(emrRecordQueryRepository, auditTrailService);
    }

    @Bean
    public CreatePrescriptionUseCase createPrescriptionUseCase(
            PrescriptionOrderRepository prescriptionOrderRepository,
            EncounterQueryRepository encounterQueryRepository,
            EmrRecordQueryRepository emrRecordQueryRepository,
            AuditTrailService auditTrailService) {
        return new CreatePrescriptionUseCase(
                prescriptionOrderRepository, encounterQueryRepository, emrRecordQueryRepository, auditTrailService);
    }

    @Bean
    public GetPrescriptionDetailUseCase getPrescriptionDetailUseCase(
            PrescriptionOrderQueryRepository prescriptionOrderQueryRepository,
            EncounterQueryRepository encounterQueryRepository,
            AuditTrailService auditTrailService) {
        return new GetPrescriptionDetailUseCase(
                prescriptionOrderQueryRepository, encounterQueryRepository, auditTrailService);
    }

    @Bean
    public UpdatePrescriptionItemsUseCase updatePrescriptionItemsUseCase(
            PrescriptionOrderQueryRepository prescriptionOrderQueryRepository,
            PrescriptionOrderRepository prescriptionOrderRepository,
            EncounterQueryRepository encounterQueryRepository,
            AuditTrailService auditTrailService) {
        return new UpdatePrescriptionItemsUseCase(
                prescriptionOrderQueryRepository, prescriptionOrderRepository,
                encounterQueryRepository, auditTrailService);
    }

    @Bean
    public IssuePrescriptionUseCase issuePrescriptionUseCase(
            PrescriptionOrderQueryRepository prescriptionOrderQueryRepository,
            PrescriptionOrderRepository prescriptionOrderRepository,
            EncounterQueryRepository encounterQueryRepository,
            AuditTrailService auditTrailService) {
        return new IssuePrescriptionUseCase(
                prescriptionOrderQueryRepository, prescriptionOrderRepository,
                encounterQueryRepository, auditTrailService);
    }

    @Bean
    public CancelPrescriptionUseCase cancelPrescriptionUseCase(
            PrescriptionOrderQueryRepository prescriptionOrderQueryRepository,
            PrescriptionOrderRepository prescriptionOrderRepository,
            EncounterQueryRepository encounterQueryRepository,
            AuditTrailService auditTrailService) {
        return new CancelPrescriptionUseCase(
                prescriptionOrderQueryRepository, prescriptionOrderRepository,
                encounterQueryRepository, auditTrailService);
    }
}
