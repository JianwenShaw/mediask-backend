package me.jianwen.mediask.domain.user.port;

import me.jianwen.mediask.domain.user.model.DoctorProfileDraft;

public interface DoctorProfileWriteRepository {

    void updateByUserId(Long userId, DoctorProfileDraft draft);
}
