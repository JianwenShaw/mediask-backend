package me.jianwen.mediask.domain.triage.exception;

import me.jianwen.mediask.common.exception.ErrorCodeCategory;
import me.jianwen.mediask.common.exception.ErrorCodeType;

public enum TriageErrorCode implements ErrorCodeType {
    NO_DEPARTMENTS_CONFIGURED(5001, "no departments configured for catalog", ErrorCodeCategory.BAD_REQUEST),
    CATALOG_VERSION_NOT_FOUND(5002, "catalog version not found", ErrorCodeCategory.NOT_FOUND),
    DEPARTMENT_NOT_IN_CATALOG(5003, "department not in catalog", ErrorCodeCategory.BAD_REQUEST),
    DEPARTMENT_NAME_MISMATCH(5004, "department name mismatch", ErrorCodeCategory.CONFLICT),
    CATALOG_NOT_FOUND(5005, "active catalog not found", ErrorCodeCategory.NOT_FOUND);

    private final int code;
    private final String message;
    private final ErrorCodeCategory category;

    TriageErrorCode(int code, String message, ErrorCodeCategory category) {
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
