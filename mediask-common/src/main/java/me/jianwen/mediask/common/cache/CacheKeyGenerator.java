package me.jianwen.mediask.common.cache;

public final class CacheKeyGenerator {

    private static final String DELIMITER = ":";

    private CacheKeyGenerator() {
    }

    public static String jwtBlacklist(String tokenId) {
        return String.join(DELIMITER, "auth", "jwt", "blacklist", tokenId);
    }

    public static String refreshToken(Long userId, String tokenId) {
        return String.join(DELIMITER, "auth", "refresh", String.valueOf(userId), tokenId);
    }

    public static String doctorProfileByUserId(Long userId) {
        return String.join(DELIMITER, "user", "doctor-profile", String.valueOf(userId));
    }

    public static String patientProfileByUserId(Long userId) {
        return String.join(DELIMITER, "user", "patient-profile", String.valueOf(userId));
    }

    public static String triageCatalogActiveVersion(String hospitalScope) {
        return String.join(DELIMITER, "triage_catalog", "active", hospitalScope);
    }

    public static String triageCatalogContent(String hospitalScope, String catalogVersion) {
        return String.join(DELIMITER, "triage_catalog", hospitalScope, catalogVersion);
    }

    public static String triageCatalogSequenceCounter(String hospitalScope, String dateStr) {
        return String.join(DELIMITER, "triage_catalog", "seq", hospitalScope, dateStr);
    }
}
