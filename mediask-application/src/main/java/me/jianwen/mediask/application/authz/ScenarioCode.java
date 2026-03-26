package me.jianwen.mediask.application.authz;

public enum ScenarioCode {
    DOCTOR_SELF_PROFILE_VIEW(ActionType.READ, CombinationMode.ALL, "doctor:profile:view:self", false, false),
    PATIENT_SELF_PROFILE_VIEW(ActionType.READ, CombinationMode.ALL, "patient:profile:view:self", false, false),
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
