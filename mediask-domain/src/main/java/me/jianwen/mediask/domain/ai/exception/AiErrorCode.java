package me.jianwen.mediask.domain.ai.exception;

import me.jianwen.mediask.common.exception.ErrorCodeCategory;
import me.jianwen.mediask.common.exception.ErrorCodeType;

public enum AiErrorCode implements ErrorCodeType {
    SERVICE_UNAVAILABLE(6001, "ai service unavailable", ErrorCodeCategory.INTERNAL_ERROR),
    SERVICE_TIMEOUT(6002, "ai service timeout", ErrorCodeCategory.INTERNAL_ERROR),
    INVALID_RESPONSE(6003, "ai response invalid", ErrorCodeCategory.INTERNAL_ERROR);

    private final int code;
    private final String message;
    private final ErrorCodeCategory category;

    AiErrorCode(int code, String message, ErrorCodeCategory category) {
        this.code = code;
        this.message = message;
        this.category = category;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public ErrorCodeCategory getCategory() {
        return category;
    }
}
