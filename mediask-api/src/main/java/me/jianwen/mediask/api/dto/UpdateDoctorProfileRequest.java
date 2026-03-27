package me.jianwen.mediask.api.dto;

import me.jianwen.mediask.common.util.ArgumentChecks;

public record UpdateDoctorProfileRequest(String professionalTitle, String introductionMasked) {

    public UpdateDoctorProfileRequest {
        professionalTitle = ArgumentChecks.blankToNull(professionalTitle);
        introductionMasked = ArgumentChecks.blankToNull(introductionMasked);
    }
}
