package me.jianwen.mediask.domain.user.port;

import java.util.Optional;
import me.jianwen.mediask.domain.user.model.PatientProfileSnapshot;

public interface PatientProfileRepository {

    Optional<PatientProfileSnapshot> findByUserId(Long userId);
}
