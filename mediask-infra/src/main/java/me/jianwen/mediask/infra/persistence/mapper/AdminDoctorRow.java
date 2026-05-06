package me.jianwen.mediask.infra.persistence.mapper;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminDoctorRow {

    private Long doctorId;
    private Long userId;
    private Integer userVersion;
    private Integer doctorVersion;
    private Long hospitalId;
    private String doctorCode;
    private String username;
    private String displayName;
    private String phone;
    private String professionalTitle;
    private String introductionMasked;
    private String mobileMasked;
    private String primaryDepartmentName;
    private String accountStatus;
}
