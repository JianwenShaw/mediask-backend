package me.jianwen.mediask.common.request;

public record RequestContextSnapshot(String requestId, String requestUri, String userId) {

    public RequestContextSnapshot {
        requestId = normalize(requestId);
        requestUri = normalize(requestUri);
        userId = normalize(userId);
    }

    public boolean hasRequestId() {
        return requestId != null;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
