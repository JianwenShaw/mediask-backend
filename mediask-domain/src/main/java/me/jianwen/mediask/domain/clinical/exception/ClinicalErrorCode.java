package me.jianwen.mediask.domain.clinical.exception;

import me.jianwen.mediask.common.exception.ErrorCodeCategory;
import me.jianwen.mediask.common.exception.ErrorCodeType;

public enum ClinicalErrorCode implements ErrorCodeType {
    ENCOUNTER_ACCESS_DENIED(4003, "encounter access denied", ErrorCodeCategory.FORBIDDEN),
    ENCOUNTER_NOT_FOUND(4004, "encounter not found", ErrorCodeCategory.NOT_FOUND),
    ENCOUNTER_AI_SUMMARY_NOT_FOUND(4005, "encounter ai summary not found", ErrorCodeCategory.NOT_FOUND),
    EMR_RECORD_ALREADY_EXISTS(4006, "emr record already exists for this encounter", ErrorCodeCategory.CONFLICT),
    EMR_ENCOUNTER_NOT_FOUND(4007, "encounter not found or access denied", ErrorCodeCategory.NOT_FOUND),
    EMR_RECORD_NOT_FOUND(4008, "emr record not found", ErrorCodeCategory.NOT_FOUND),
    EMR_RECORD_ACCESS_DENIED(4009, "emr record access denied", ErrorCodeCategory.FORBIDDEN);

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
