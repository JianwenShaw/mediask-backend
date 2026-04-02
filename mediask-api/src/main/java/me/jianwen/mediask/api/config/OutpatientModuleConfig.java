package me.jianwen.mediask.api.config;

import me.jianwen.mediask.application.outpatient.usecase.ListClinicSessionsUseCase;
import me.jianwen.mediask.domain.outpatient.port.ClinicSessionQueryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OutpatientModuleConfig {

    @Bean
    public ListClinicSessionsUseCase listClinicSessionsUseCase(ClinicSessionQueryRepository clinicSessionQueryRepository) {
        return new ListClinicSessionsUseCase(clinicSessionQueryRepository);
    }
}
