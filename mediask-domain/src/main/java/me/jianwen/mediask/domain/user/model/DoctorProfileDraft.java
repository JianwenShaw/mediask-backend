package me.jianwen.mediask.domain.user.model;

import me.jianwen.mediask.common.util.ArgumentChecks;

public record DoctorProfileDraft(String professionalTitle, String introductionMasked) {

    public DoctorProfileDraft {
        professionalTitle = ArgumentChecks.blankToNull(professionalTitle);
        introductionMasked = ArgumentChecks.blankToNull(introductionMasked);
    }
}
