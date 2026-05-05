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
    EMR_RECORD_ACCESS_DENIED(4009, "emr record access denied", ErrorCodeCategory.FORBIDDEN),
    ENCOUNTER_STATUS_TRANSITION_NOT_ALLOWED(4010, "encounter status transition not allowed", ErrorCodeCategory.CONFLICT),
    ENCOUNTER_STATUS_UPDATE_CONFLICT(4011, "encounter status update conflict", ErrorCodeCategory.CONFLICT),
    ENCOUNTER_REGISTRATION_SYNC_CONFLICT(4012, "encounter registration sync conflict", ErrorCodeCategory.CONFLICT),
    PRESCRIPTION_ENCOUNTER_NOT_FOUND(4013, "encounter not found or access denied", ErrorCodeCategory.NOT_FOUND),
    PRESCRIPTION_EMR_RECORD_NOT_FOUND(4014, "emr record not found for encounter", ErrorCodeCategory.NOT_FOUND),
    PRESCRIPTION_ALREADY_EXISTS(4015, "prescription already exists for this encounter", ErrorCodeCategory.CONFLICT),
    PRESCRIPTION_NOT_FOUND(4016, "prescription not found", ErrorCodeCategory.NOT_FOUND),
    PRESCRIPTION_STATUS_TRANSITION_NOT_ALLOWED(4017, "prescription status transition not allowed", ErrorCodeCategory.CONFLICT),
    PRESCRIPTION_UPDATE_CONFLICT(4018, "prescription update conflict", ErrorCodeCategory.CONFLICT),
    PRESCRIPTION_ACCESS_DENIED(4019, "prescription access denied", ErrorCodeCategory.FORBIDDEN);

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
