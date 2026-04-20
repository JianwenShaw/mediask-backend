package me.jianwen.mediask.api.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

public record CreateRegistrationResponse(
        @JsonSerialize(using = ToStringSerializer.class) Long registrationId,
        String orderNo,
        String status) {}
