package me.jianwen.mediask.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.List;

public record TriageDepartmentCatalogResponse(
        @JsonProperty("hospital_scope") String hospitalScope,
        @JsonProperty("department_catalog_version") String departmentCatalogVersion,
        @JsonProperty("department_candidates") List<TriageDepartmentCandidateResponse> departmentCandidates) {

    public record TriageDepartmentCandidateResponse(
            @JsonProperty("department_id")
                    @JsonSerialize(using = ToStringSerializer.class)
                    Long departmentId,
            @JsonProperty("department_name") String departmentName,
            @JsonProperty("routing_hint") String routingHint,
            @JsonProperty("aliases") List<String> aliases,
            @JsonProperty("sort_order") Integer sortOrder) {}
}
