package me.jianwen.mediask.infra.persistence.mapper;

public class ActiveDataScopeRuleRow {

    private String resourceType;
    private String scopeType;
    private Long scopeDeptId;

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getScopeType() {
        return scopeType;
    }

    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    public Long getScopeDeptId() {
        return scopeDeptId;
    }

    public void setScopeDeptId(Long scopeDeptId) {
        this.scopeDeptId = scopeDeptId;
    }
}
