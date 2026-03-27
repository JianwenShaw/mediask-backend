package me.jianwen.mediask.domain.user.port;

import me.jianwen.mediask.domain.user.model.PatientProfileDraft;

public interface PatientProfileWriteRepository {

    void updateByUserId(Long userId, PatientProfileDraft draft);
}
