package me.jianwen.mediask.application.authz;

public enum ScenarioCode {
    ADMIN_PATIENT_LIST(ActionType.READ, CombinationMode.ALL, "admin:patient:list", false, false),
    ADMIN_PATIENT_VIEW(ActionType.READ, CombinationMode.ALL, "admin:patient:view", false, false),
    ADMIN_PATIENT_CREATE(ActionType.CREATE, CombinationMode.ALL, "admin:patient:create", false, false),
    ADMIN_PATIENT_UPDATE(ActionType.UPDATE, CombinationMode.ALL, "admin:patient:update", false, false),
    ADMIN_PATIENT_DELETE(ActionType.DELETE, CombinationMode.ALL, "admin:patient:delete", false, false),
    ADMIN_KNOWLEDGE_BASE_LIST(ActionType.READ, CombinationMode.ALL, "admin:knowledge:base:list", false, false),
    ADMIN_KNOWLEDGE_BASE_CREATE(ActionType.CREATE, CombinationMode.ALL, "admin:knowledge:base:create", false, false),
    ADMIN_KNOWLEDGE_BASE_UPDATE(ActionType.UPDATE, CombinationMode.ALL, "admin:knowledge:base:update", false, false),
    ADMIN_KNOWLEDGE_BASE_DELETE(ActionType.DELETE, CombinationMode.ALL, "admin:knowledge:base:delete", false, false),
    ADMIN_KNOWLEDGE_DOCUMENT_LIST(ActionType.READ, CombinationMode.ALL, "admin:knowledge:document:list", false, false),
    ADMIN_KNOWLEDGE_DOCUMENT_IMPORT(ActionType.CREATE, CombinationMode.ALL, "admin:knowledge:document:import", false, false),
    ADMIN_KNOWLEDGE_DOCUMENT_DELETE(ActionType.DELETE, CombinationMode.ALL, "admin:knowledge:document:delete", false, false),
    DOCTOR_SELF_PROFILE_VIEW(ActionType.READ, CombinationMode.ALL, "doctor:profile:view:self", false, false),
    DOCTOR_SELF_PROFILE_UPDATE(ActionType.UPDATE, CombinationMode.ALL, "doctor:profile:update:self", false, false),
    PATIENT_SELF_PROFILE_VIEW(ActionType.READ, CombinationMode.ALL, "patient:profile:view:self", false, false),
    PATIENT_SELF_PROFILE_UPDATE(ActionType.UPDATE, CombinationMode.ALL, "patient:profile:update:self", false, false),
    ENCOUNTER_LIST(ActionType.READ, CombinationMode.ALL, "encounter:query", false, false),
    EMR_RECORD_CREATE(ActionType.CREATE, CombinationMode.ALL, "emr:create", false, false),
    EMR_RECORD_READ(ActionType.READ, CombinationMode.ALL, "emr:read", true, false),
    EMR_RECORD_UPDATE(ActionType.UPDATE, CombinationMode.ALL, "emr:update", true, false),
    AI_SESSION_READ(ActionType.READ, CombinationMode.ALL, "ai:session:read", true, true),
    AI_SESSION_EXPORT(ActionType.EXPORT, CombinationMode.ALL, "ai:session:export", true, true);

    private final ActionType actionType;
    private final CombinationMode combinationMode;
    private final String permissionCode;
    private final boolean objectScoped;
    private final boolean sensitive;

    ScenarioCode(
            ActionType actionType,
            CombinationMode combinationMode,
            String permissionCode,
            boolean objectScoped,
            boolean sensitive) {
        this.actionType = actionType;
        this.combinationMode = combinationMode;
        this.permissionCode = permissionCode;
        this.objectScoped = objectScoped;
        this.sensitive = sensitive;
    }

    public ActionType actionType() {
        return actionType;
    }

    public CombinationMode combinationMode() {
        return combinationMode;
    }

    public String permissionCode() {
        return permissionCode;
    }

    public boolean objectScoped() {
        return objectScoped;
    }

    public boolean sensitive() {
        return sensitive;
    }
}
