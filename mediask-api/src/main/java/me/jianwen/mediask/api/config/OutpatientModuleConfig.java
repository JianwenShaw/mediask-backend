package me.jianwen.mediask.api.config;

import me.jianwen.mediask.application.audit.usecase.AuditTrailService;
import me.jianwen.mediask.application.outpatient.usecase.ListClinicSessionsUseCase;
import me.jianwen.mediask.application.outpatient.usecase.ListClinicSessionSlotsUseCase;
import me.jianwen.mediask.application.outpatient.usecase.CancelRegistrationUseCase;
import me.jianwen.mediask.application.outpatient.usecase.CreateRegistrationUseCase;
import me.jianwen.mediask.application.outpatient.usecase.GetRegistrationDetailUseCase;
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
    public ListClinicSessionSlotsUseCase listClinicSessionSlotsUseCase(
            ClinicSessionQueryRepository clinicSessionQueryRepository) {
        return new ListClinicSessionSlotsUseCase(clinicSessionQueryRepository);
    }

    @Bean
    public CreateRegistrationUseCase createRegistrationUseCase(
            ClinicSlotReservationRepository clinicSlotReservationRepository,
            RegistrationOrderRepository registrationOrderRepository,
            VisitEncounterRepository visitEncounterRepository,
            AuditTrailService auditTrailService) {
        return new CreateRegistrationUseCase(
                clinicSlotReservationRepository, registrationOrderRepository, visitEncounterRepository, auditTrailService);
    }

    @Bean
    public ListRegistrationsUseCase listRegistrationsUseCase(
            RegistrationOrderQueryRepository registrationOrderQueryRepository) {
        return new ListRegistrationsUseCase(registrationOrderQueryRepository);
    }

    @Bean
    public GetRegistrationDetailUseCase getRegistrationDetailUseCase(
            RegistrationOrderQueryRepository registrationOrderQueryRepository) {
        return new GetRegistrationDetailUseCase(registrationOrderQueryRepository);
    }

    @Bean
    public CancelRegistrationUseCase cancelRegistrationUseCase(
            RegistrationOrderRepository registrationOrderRepository,
            ClinicSlotReservationRepository clinicSlotReservationRepository,
            VisitEncounterRepository visitEncounterRepository,
            AuditTrailService auditTrailService) {
        return new CancelRegistrationUseCase(
                registrationOrderRepository, clinicSlotReservationRepository, visitEncounterRepository, auditTrailService);
    }
}
