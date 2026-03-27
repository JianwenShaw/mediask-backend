package me.jianwen.mediask.infra.cache;

import java.time.Duration;

public final class UserProfileCachePolicy {

    public static final Duration DOCTOR_PROFILE_TTL = Duration.ofMinutes(10);
    public static final Duration PATIENT_PROFILE_TTL = Duration.ofMinutes(10);

    private UserProfileCachePolicy() {
    }
}
