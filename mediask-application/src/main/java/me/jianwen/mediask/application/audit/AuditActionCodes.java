package me.jianwen.mediask.application.audit;

public final class AuditActionCodes {

    public static final String AUTH_LOGIN_SUCCESS = "AUTH_LOGIN_SUCCESS";
    public static final String AUTH_LOGIN_FAILED = "AUTH_LOGIN_FAILED";
    public static final String AUTH_LOGOUT = "AUTH_LOGOUT";
    public static final String REGISTRATION_CREATE = "REGISTRATION_CREATE";
    public static final String REGISTRATION_CANCEL = "REGISTRATION_CANCEL";
    public static final String ENCOUNTER_UPDATE = "ENCOUNTER_UPDATE";
    public static final String ENCOUNTER_AI_SUMMARY_VIEW_FAILED = "ENCOUNTER_AI_SUMMARY_VIEW_FAILED";
    public static final String EMR_CREATE = "EMR_CREATE";
    public static final String PRESCRIPTION_CREATE = "PRESCRIPTION_CREATE";
    public static final String PRESCRIPTION_UPDATE = "PRESCRIPTION_UPDATE";
    public static final String PRESCRIPTION_ISSUE = "PRESCRIPTION_ISSUE";
    public static final String PRESCRIPTION_CANCEL = "PRESCRIPTION_CANCEL";
    public static final String AI_SESSION_VIEW_FAILED = "AI_SESSION_VIEW_FAILED";
    public static final String AI_TRIAGE_RESULT_VIEW_FAILED = "AI_TRIAGE_RESULT_VIEW_FAILED";
    public static final String PATIENT_PROFILE_UPDATE = "PATIENT_PROFILE_UPDATE";
    public static final String ADMIN_PATIENT_CREATE = "ADMIN_PATIENT_CREATE";
    public static final String ADMIN_PATIENT_UPDATE = "ADMIN_PATIENT_UPDATE";
    public static final String ADMIN_PATIENT_DELETE = "ADMIN_PATIENT_DELETE";
    public static final String ADMIN_DOCTOR_CREATE = "ADMIN_DOCTOR_CREATE";
    public static final String ADMIN_DOCTOR_UPDATE = "ADMIN_DOCTOR_UPDATE";
    public static final String ADMIN_DOCTOR_DELETE = "ADMIN_DOCTOR_DELETE";
    public static final String AUDIT_QUERY = "AUDIT_QUERY";
    public static final String AUDIT_QUERY_DENIED = "AUDIT_QUERY_DENIED";

    private AuditActionCodes() {
    }
}
