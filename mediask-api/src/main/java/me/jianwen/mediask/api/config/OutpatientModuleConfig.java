package me.jianwen.mediask.api.config;

import me.jianwen.mediask.application.outpatient.usecase.ListClinicSessionsUseCase;
import me.jianwen.mediask.application.outpatient.usecase.CreateRegistrationUseCase;
import me.jianwen.mediask.application.outpatient.usecase.ListRegistrationsUseCase;
import me.jianwen.mediask.domain.clinical.port.VisitEncounterRepository;
import me.jianwen.mediask.domain.outpatient.port.ClinicSlotReservationRepository;
import me.jianwen.mediask.domain.outpatient.port.ClinicSessionQueryRepository;
import me.jianwen.mediask.domain.outpatient.port.RegistrationOrderRepository;
import me.jianwen.mediask.domain.outpatient.port.RegistrationOrderQueryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OutpatientModuleConfig {

    @Bean
    public ListClinicSessionsUseCase listClinicSessionsUseCase(ClinicSessionQueryRepository clinicSessionQueryRepository) {
        return new ListClinicSessionsUseCase(clinicSessionQueryRepository);
    }

    @Bean
    public CreateRegistrationUseCase createRegistrationUseCase(
            ClinicSlotReservationRepository clinicSlotReservationRepository,
            RegistrationOrderRepository registrationOrderRepository,
            VisitEncounterRepository visitEncounterRepository) {
        return new CreateRegistrationUseCase(
                clinicSlotReservationRepository, registrationOrderRepository, visitEncounterRepository);
    }

    @Bean
    public ListRegistrationsUseCase listRegistrationsUseCase(
            RegistrationOrderQueryRepository registrationOrderQueryRepository) {
        return new ListRegistrationsUseCase(registrationOrderQueryRepository);
    }
}
