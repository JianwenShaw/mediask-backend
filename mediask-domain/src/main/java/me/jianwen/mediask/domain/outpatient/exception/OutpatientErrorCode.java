package me.jianwen.mediask.domain.outpatient.exception;

import me.jianwen.mediask.common.exception.ErrorCodeCategory;
import me.jianwen.mediask.common.exception.ErrorCodeType;

public enum OutpatientErrorCode implements ErrorCodeType {
    SESSION_NOT_FOUND(3004, "session not found", ErrorCodeCategory.NOT_FOUND),
    SLOT_NOT_AVAILABLE(3005, "slot not available", ErrorCodeCategory.CONFLICT),
    INVALID_STATUS_TRANSITION(3006, "invalid status transition", ErrorCodeCategory.CONFLICT),
    SESSION_UPDATE_CONFLICT(3007, "session update conflict", ErrorCodeCategory.CONFLICT),
    REGISTRATION_NOT_FOUND(3008, "registration not found", ErrorCodeCategory.NOT_FOUND),
    REGISTRATION_CANCEL_NOT_ALLOWED(3009, "registration cancel not allowed", ErrorCodeCategory.CONFLICT);

    private final int code;
    private final String message;
    private final ErrorCodeCategory category;

    OutpatientErrorCode(int code, String message, ErrorCodeCategory category) {
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
