package me.jianwen.mediask.api.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.List;

public record CurrentUserResponse(
        @JsonSerialize(using = ToStringSerializer.class) Long userId,
        String username,
        String displayName,
        String userType,
        List<String> roles,
        List<String> permissions,
        List<DataScopeRuleResponse> dataScopeRules,
        @JsonSerialize(using = ToStringSerializer.class) Long patientId,
        @JsonSerialize(using = ToStringSerializer.class) Long doctorId,
        @JsonSerialize(using = ToStringSerializer.class) Long primaryDepartmentId) {
}
