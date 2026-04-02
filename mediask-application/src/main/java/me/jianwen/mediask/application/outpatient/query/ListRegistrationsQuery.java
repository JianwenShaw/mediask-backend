package me.jianwen.mediask.application.outpatient.query;

import me.jianwen.mediask.domain.outpatient.model.RegistrationStatus;

public record ListRegistrationsQuery(Long patientId, RegistrationStatus status) {

    public ListRegistrationsQuery {
        if (patientId == null) {
            throw new IllegalArgumentException("patientId must not be null");
        }
    }
}
