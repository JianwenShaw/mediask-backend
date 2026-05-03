package me.jianwen.mediask.api.config;

import me.jianwen.mediask.application.audit.usecase.AuditTrailService;
import me.jianwen.mediask.application.clinical.usecase.CreateEmrUseCase;
import me.jianwen.mediask.application.clinical.usecase.CreatePrescriptionUseCase;
import me.jianwen.mediask.application.clinical.usecase.GetEncounterDetailUseCase;
import me.jianwen.mediask.application.clinical.usecase.GetEmrDetailUseCase;
import me.jianwen.mediask.application.clinical.usecase.GetPrescriptionDetailUseCase;
import me.jianwen.mediask.application.clinical.usecase.ListEncountersUseCase;
import me.jianwen.mediask.application.clinical.usecase.UpdateEncounterStatusUseCase;
import me.jianwen.mediask.domain.clinical.port.EmrRecordQueryRepository;
import me.jianwen.mediask.domain.clinical.port.EmrRecordRepository;
import me.jianwen.mediask.domain.clinical.port.EncounterQueryRepository;
import me.jianwen.mediask.domain.clinical.port.PrescriptionOrderQueryRepository;
import me.jianwen.mediask.domain.clinical.port.PrescriptionOrderRepository;
import me.jianwen.mediask.domain.clinical.port.VisitEncounterRepository;
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
}
