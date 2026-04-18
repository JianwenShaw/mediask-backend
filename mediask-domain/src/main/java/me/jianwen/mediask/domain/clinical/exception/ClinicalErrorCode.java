package me.jianwen.mediask.domain.clinical.exception;

import me.jianwen.mediask.common.exception.ErrorCodeCategory;
import me.jianwen.mediask.common.exception.ErrorCodeType;

public enum ClinicalErrorCode implements ErrorCodeType {
    ENCOUNTER_ACCESS_DENIED(4003, "encounter access denied", ErrorCodeCategory.FORBIDDEN),
    ENCOUNTER_NOT_FOUND(4004, "encounter not found", ErrorCodeCategory.NOT_FOUND);

    private final int code;
    private final String message;
    private final ErrorCodeCategory category;

    ClinicalErrorCode(int code, String message, ErrorCodeCategory category) {
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
