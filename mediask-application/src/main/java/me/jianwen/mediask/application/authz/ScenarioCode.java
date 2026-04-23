package me.jianwen.mediask.application.authz;

public enum ScenarioCode {
    ADMIN_PATIENT_LIST(ActionType.READ, CombinationMode.ALL, "admin:patient:list", false, false),
    ADMIN_PATIENT_VIEW(ActionType.READ, CombinationMode.ALL, "admin:patient:view", false, false),
    ADMIN_PATIENT_CREATE(ActionType.CREATE, CombinationMode.ALL, "admin:patient:create", false, false),
    ADMIN_PATIENT_UPDATE(ActionType.UPDATE, CombinationMode.ALL, "admin:patient:update", false, false),
    ADMIN_PATIENT_DELETE(ActionType.DELETE, CombinationMode.ALL, "admin:patient:delete", false, false),
    DOCTOR_SELF_PROFILE_VIEW(ActionType.READ, CombinationMode.ALL, "doctor:profile:view:self", false, false),
    DOCTOR_SELF_PROFILE_UPDATE(ActionType.UPDATE, CombinationMode.ALL, "doctor:profile:update:self", false, false),
    PATIENT_SELF_PROFILE_VIEW(ActionType.READ, CombinationMode.ALL, "patient:profile:view:self", false, false),
    PATIENT_SELF_PROFILE_UPDATE(ActionType.UPDATE, CombinationMode.ALL, "patient:profile:update:self", false, false),
    ENCOUNTER_LIST(ActionType.READ, CombinationMode.ALL, "encounter:query", false, false),
    ENCOUNTER_UPDATE(ActionType.UPDATE, CombinationMode.ALL, "encounter:update", false, false),
    EMR_RECORD_CREATE(ActionType.CREATE, CombinationMode.ALL, "emr:create", false, false),
    EMR_RECORD_READ(ActionType.READ, CombinationMode.ALL, "emr:read", true, false),
    EMR_RECORD_UPDATE(ActionType.UPDATE, CombinationMode.ALL, "emr:update", true, false),
    PRESCRIPTION_CREATE(ActionType.CREATE, CombinationMode.ALL, "prescription:create", false, false),
    PRESCRIPTION_READ(ActionType.READ, CombinationMode.ALL, "prescription:read", false, false);

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
