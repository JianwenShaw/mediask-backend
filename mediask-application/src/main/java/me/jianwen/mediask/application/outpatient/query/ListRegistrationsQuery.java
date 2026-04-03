package me.jianwen.mediask.application.outpatient.query;

import me.jianwen.mediask.domain.outpatient.model.RegistrationStatus;

public record ListRegistrationsQuery(Long patientUserId, RegistrationStatus status) {

    public ListRegistrationsQuery {
        if (patientUserId == null) {
            throw new IllegalArgumentException("patientUserId must not be null");
        }
    }
}
