package me.jianwen.mediask.api.config;

import me.jianwen.mediask.application.clinical.usecase.CreateEmrUseCase;
import me.jianwen.mediask.application.clinical.usecase.GetEncounterAiSummaryUseCase;
import me.jianwen.mediask.application.clinical.usecase.GetEncounterDetailUseCase;
import me.jianwen.mediask.application.clinical.usecase.GetEmrDetailUseCase;
import me.jianwen.mediask.application.clinical.usecase.ListEncountersUseCase;
import me.jianwen.mediask.application.clinical.usecase.UpdateEncounterStatusUseCase;
import me.jianwen.mediask.domain.clinical.port.EmrRecordQueryRepository;
import me.jianwen.mediask.domain.clinical.port.EmrRecordRepository;
import me.jianwen.mediask.domain.clinical.port.EncounterQueryRepository;
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
    public GetEncounterAiSummaryUseCase getEncounterAiSummaryUseCase(EncounterQueryRepository encounterQueryRepository) {
        return new GetEncounterAiSummaryUseCase(encounterQueryRepository);
    }

    @Bean
    public UpdateEncounterStatusUseCase updateEncounterStatusUseCase(
            EncounterQueryRepository encounterQueryRepository,
            VisitEncounterRepository visitEncounterRepository,
            RegistrationOrderRepository registrationOrderRepository) {
        return new UpdateEncounterStatusUseCase(
                encounterQueryRepository, visitEncounterRepository, registrationOrderRepository);
    }

    @Bean
    public CreateEmrUseCase createEmrUseCase(
            EmrRecordRepository emrRecordRepository,
            EncounterQueryRepository encounterQueryRepository) {
        return new CreateEmrUseCase(emrRecordRepository, encounterQueryRepository);
    }

    @Bean
    public GetEmrDetailUseCase getEmrDetailUseCase(EmrRecordQueryRepository emrRecordQueryRepository) {
        return new GetEmrDetailUseCase(emrRecordQueryRepository);
    }
}
