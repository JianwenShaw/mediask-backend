package me.jianwen.mediask.domain.ai.exception;

import me.jianwen.mediask.common.exception.ErrorCodeCategory;
import me.jianwen.mediask.common.exception.ErrorCodeType;

public enum AiErrorCode implements ErrorCodeType {
    TRIAGE_RESULT_NOT_READY(6101, "triage result not ready", ErrorCodeCategory.CONFLICT);

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
