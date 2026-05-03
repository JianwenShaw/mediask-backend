package me.jianwen.mediask.api.audit;

public final class AuditActionCodes {

    public static final String AUTH_LOGIN_SUCCESS = me.jianwen.mediask.application.audit.AuditActionCodes.AUTH_LOGIN_SUCCESS;
    public static final String AUTH_LOGIN_FAILED = me.jianwen.mediask.application.audit.AuditActionCodes.AUTH_LOGIN_FAILED;
    public static final String AUTH_LOGOUT = me.jianwen.mediask.application.audit.AuditActionCodes.AUTH_LOGOUT;
    public static final String REGISTRATION_CREATE = me.jianwen.mediask.application.audit.AuditActionCodes.REGISTRATION_CREATE;
    public static final String REGISTRATION_CANCEL = me.jianwen.mediask.application.audit.AuditActionCodes.REGISTRATION_CANCEL;
    public static final String ENCOUNTER_UPDATE = me.jianwen.mediask.application.audit.AuditActionCodes.ENCOUNTER_UPDATE;
    public static final String EMR_CREATE = me.jianwen.mediask.application.audit.AuditActionCodes.EMR_CREATE;
    public static final String PATIENT_PROFILE_UPDATE = me.jianwen.mediask.application.audit.AuditActionCodes.PATIENT_PROFILE_UPDATE;
    public static final String ADMIN_PATIENT_CREATE = me.jianwen.mediask.application.audit.AuditActionCodes.ADMIN_PATIENT_CREATE;
    public static final String ADMIN_PATIENT_UPDATE = me.jianwen.mediask.application.audit.AuditActionCodes.ADMIN_PATIENT_UPDATE;
    public static final String ADMIN_PATIENT_DELETE = me.jianwen.mediask.application.audit.AuditActionCodes.ADMIN_PATIENT_DELETE;
    public static final String PRESCRIPTION_CREATE = me.jianwen.mediask.application.audit.AuditActionCodes.PRESCRIPTION_CREATE;
    public static final String AI_SESSION_VIEW_FAILED = me.jianwen.mediask.application.audit.AuditActionCodes.AI_SESSION_VIEW_FAILED;
    public static final String AI_TRIAGE_RESULT_VIEW_FAILED =
            me.jianwen.mediask.application.audit.AuditActionCodes.AI_TRIAGE_RESULT_VIEW_FAILED;
    public static final String AUDIT_QUERY = me.jianwen.mediask.application.audit.AuditActionCodes.AUDIT_QUERY;
    public static final String AUDIT_QUERY_DENIED = me.jianwen.mediask.application.audit.AuditActionCodes.AUDIT_QUERY_DENIED;

    private AuditActionCodes() {
    }
}
