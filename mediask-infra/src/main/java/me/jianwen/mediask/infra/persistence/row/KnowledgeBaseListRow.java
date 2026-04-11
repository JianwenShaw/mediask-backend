package me.jianwen.mediask.infra.persistence.row;

public class KnowledgeBaseListRow {

    private Long id;
    private String kbCode;
    private String name;
    private String ownerType;
    private Long ownerDeptId;
    private String visibility;
    private String status;
    private Long docCount;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKbCode() {
        return kbCode;
    }

    public void setKbCode(String kbCode) {
        this.kbCode = kbCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwnerType() {
        return ownerType;
    }

    public void setOwnerType(String ownerType) {
        this.ownerType = ownerType;
    }

    public Long getOwnerDeptId() {
        return ownerDeptId;
    }

    public void setOwnerDeptId(Long ownerDeptId) {
        this.ownerDeptId = ownerDeptId;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getDocCount() {
        return docCount;
    }

    public void setDocCount(Long docCount) {
        this.docCount = docCount;
    }
}
