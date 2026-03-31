package me.jianwen.mediask.domain.user.exception;

import me.jianwen.mediask.common.exception.ErrorCodeCategory;
import me.jianwen.mediask.common.exception.ErrorCodeType;

public enum UserErrorCode implements ErrorCodeType {
    INVALID_CREDENTIALS(2001, "invalid username or password", ErrorCodeCategory.UNAUTHORIZED),
    ACCOUNT_DISABLED(2002, "account disabled", ErrorCodeCategory.FORBIDDEN),
    ACCOUNT_LOCKED(2003, "account locked", ErrorCodeCategory.FORBIDDEN),
    AUTHENTICATED_USER_NOT_FOUND(2004, "authenticated user not found", ErrorCodeCategory.UNAUTHORIZED),
    ROLE_NOT_ASSIGNED(2005, "role not assigned", ErrorCodeCategory.FORBIDDEN),
    INVALID_REFRESH_TOKEN(2006, "invalid refresh token", ErrorCodeCategory.UNAUTHORIZED),
    REFRESH_TOKEN_EXPIRED(2007, "refresh token expired", ErrorCodeCategory.UNAUTHORIZED),
    ROLE_MISMATCH(2008, "role mismatch", ErrorCodeCategory.FORBIDDEN),
    PATIENT_PROFILE_NOT_FOUND(2009, "patient profile not found", ErrorCodeCategory.NOT_FOUND),
    DOCTOR_PROFILE_NOT_FOUND(2010, "doctor profile not found", ErrorCodeCategory.NOT_FOUND),
    PERMISSION_DENIED(2011, "permission denied", ErrorCodeCategory.FORBIDDEN),
    PATIENT_PROFILE_UPDATE_CONFLICT(2012, "patient profile update conflict", ErrorCodeCategory.CONFLICT),
    DOCTOR_PROFILE_UPDATE_CONFLICT(2013, "doctor profile update conflict", ErrorCodeCategory.CONFLICT),
    ADMIN_PATIENT_NOT_FOUND(2014, "admin patient not found", ErrorCodeCategory.NOT_FOUND),
    ADMIN_PATIENT_USERNAME_CONFLICT(2015, "admin patient username conflict", ErrorCodeCategory.CONFLICT),
    ADMIN_PATIENT_NO_CONFLICT(2016, "admin patient number conflict", ErrorCodeCategory.CONFLICT),
    ADMIN_PATIENT_ROLE_NOT_FOUND(2017, "admin patient role not found", ErrorCodeCategory.NOT_FOUND),
    ADMIN_PATIENT_UPDATE_CONFLICT(2018, "admin patient update conflict", ErrorCodeCategory.CONFLICT),
    ADMIN_PATIENT_DELETE_CONFLICT(2019, "admin patient delete conflict", ErrorCodeCategory.CONFLICT);

    private final int code;
    private final String message;
    private final ErrorCodeCategory category;

    UserErrorCode(int code, String message, ErrorCodeCategory category) {
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
