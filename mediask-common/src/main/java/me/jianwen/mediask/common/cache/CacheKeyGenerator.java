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
}
