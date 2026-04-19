package me.jianwen.mediask.api.config;

import me.jianwen.mediask.application.clinical.usecase.CreateEmrUseCase;
import me.jianwen.mediask.application.clinical.usecase.ListEncountersUseCase;
import me.jianwen.mediask.application.clinical.usecase.GetEncounterAiSummaryUseCase;
import me.jianwen.mediask.application.clinical.usecase.GetEncounterDetailUseCase;
import me.jianwen.mediask.domain.clinical.port.EmrRecordRepository;
import me.jianwen.mediask.domain.clinical.port.EncounterQueryRepository;
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
    public CreateEmrUseCase createEmrUseCase(
            EmrRecordRepository emrRecordRepository,
            EncounterQueryRepository encounterQueryRepository) {
        return new CreateEmrUseCase(emrRecordRepository, encounterQueryRepository);
    }
}
