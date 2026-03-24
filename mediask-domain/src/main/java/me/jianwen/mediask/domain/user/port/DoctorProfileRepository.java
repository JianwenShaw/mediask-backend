package me.jianwen.mediask.domain.user.port;

import java.util.Optional;
import me.jianwen.mediask.domain.user.model.DoctorProfile;

public interface DoctorProfileRepository {

    Optional<DoctorProfile> findByUserId(Long userId);
}
