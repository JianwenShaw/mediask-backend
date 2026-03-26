package me.jianwen.mediask.application.authz;

public enum ActionType {
    READ,
    CREATE,
    UPDATE,
    DELETE,
    EXPORT,
    VIEW_RAW;

    public boolean isWriteOperation() {
        return this == CREATE || this == UPDATE || this == DELETE;
    }
}
