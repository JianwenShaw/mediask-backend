package me.jianwen.mediask.domain.ai.exception;

import me.jianwen.mediask.common.exception.ErrorCodeCategory;
import me.jianwen.mediask.common.exception.ErrorCodeType;

public enum AiErrorCode implements ErrorCodeType {
    SERVICE_UNAVAILABLE(6001, "ai service unavailable", ErrorCodeCategory.INTERNAL_ERROR),
    SERVICE_TIMEOUT(6002, "ai service timeout", ErrorCodeCategory.INTERNAL_ERROR),
    INVALID_RESPONSE(6003, "ai response invalid", ErrorCodeCategory.INTERNAL_ERROR),
    KNOWLEDGE_BASE_NOT_FOUND(6004, "knowledge base not found", ErrorCodeCategory.NOT_FOUND),
    KNOWLEDGE_DOCUMENT_DUPLICATE(6005, "knowledge document duplicate", ErrorCodeCategory.CONFLICT),
    KNOWLEDGE_DOCUMENT_NOT_FOUND(6006, "knowledge document not found", ErrorCodeCategory.NOT_FOUND),
    KNOWLEDGE_DOCUMENT_STATUS_INVALID(6007, "knowledge document status invalid", ErrorCodeCategory.CONFLICT),
    KNOWLEDGE_DOCUMENT_UPDATE_CONFLICT(6008, "knowledge document update conflict", ErrorCodeCategory.CONFLICT);

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
