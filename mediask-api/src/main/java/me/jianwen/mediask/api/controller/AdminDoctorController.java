package me.jianwen.mediask.api.controller;

import me.jianwen.mediask.api.assembler.AuthAssembler;
import me.jianwen.mediask.api.audit.AuditActionCodes;
import me.jianwen.mediask.api.audit.AuditApiSupport;
import me.jianwen.mediask.api.audit.AuditResourceTypes;
import me.jianwen.mediask.api.dto.AdminDoctorDetailResponse;
import me.jianwen.mediask.api.dto.AdminDoctorListItemResponse;
import me.jianwen.mediask.api.dto.CreateAdminDoctorRequest;
import me.jianwen.mediask.api.dto.UpdateAdminDoctorRequest;
import me.jianwen.mediask.api.security.AuthenticatedUserPrincipal;
import me.jianwen.mediask.application.authz.AuthorizeScenario;
import me.jianwen.mediask.application.authz.ScenarioCode;
import me.jianwen.mediask.application.user.command.CreateAdminDoctorCommand;
import me.jianwen.mediask.application.user.command.DeleteAdminDoctorCommand;
import me.jianwen.mediask.application.user.command.UpdateAdminDoctorCommand;
import me.jianwen.mediask.application.user.query.GetAdminDoctorDetailQuery;
import me.jianwen.mediask.application.user.query.ListAdminDoctorsQuery;
import me.jianwen.mediask.application.user.usecase.CreateAdminDoctorUseCase;
import me.jianwen.mediask.application.user.usecase.DeleteAdminDoctorUseCase;
import me.jianwen.mediask.application.user.usecase.GetAdminDoctorDetailUseCase;
import me.jianwen.mediask.application.user.usecase.ListAdminDoctorsUseCase;
import me.jianwen.mediask.application.user.usecase.UpdateAdminDoctorUseCase;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.pagination.PageData;
import me.jianwen.mediask.common.result.Result;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/doctors")
public class AdminDoctorController {

    private final ListAdminDoctorsUseCase listAdminDoctorsUseCase;
    private final GetAdminDoctorDetailUseCase getAdminDoctorDetailUseCase;
    private final CreateAdminDoctorUseCase createAdminDoctorUseCase;
    private final UpdateAdminDoctorUseCase updateAdminDoctorUseCase;
    private final DeleteAdminDoctorUseCase deleteAdminDoctorUseCase;
    private final AuditApiSupport auditApiSupport;

    public AdminDoctorController(
            ListAdminDoctorsUseCase listAdminDoctorsUseCase,
            GetAdminDoctorDetailUseCase getAdminDoctorDetailUseCase,
            CreateAdminDoctorUseCase createAdminDoctorUseCase,
            UpdateAdminDoctorUseCase updateAdminDoctorUseCase,
            DeleteAdminDoctorUseCase deleteAdminDoctorUseCase,
            AuditApiSupport auditApiSupport) {
        this.listAdminDoctorsUseCase = listAdminDoctorsUseCase;
        this.getAdminDoctorDetailUseCase = getAdminDoctorDetailUseCase;
        this.createAdminDoctorUseCase = createAdminDoctorUseCase;
        this.updateAdminDoctorUseCase = updateAdminDoctorUseCase;
        this.deleteAdminDoctorUseCase = deleteAdminDoctorUseCase;
        this.auditApiSupport = auditApiSupport;
    }

    @GetMapping
    @AuthorizeScenario(ScenarioCode.ADMIN_DOCTOR_LIST)
    public Result<PageData<AdminDoctorListItemResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long pageNum,
            @RequestParam(required = false) Long pageSize) {
        PageData<AdminDoctorListItemResponse> response = listAdminDoctorsUseCase
                .handle(ListAdminDoctorsQuery.page(keyword, pageNum, pageSize))
                .map(AuthAssembler::toAdminDoctorListItemResponse);
        return Result.ok(response);
    }

    @GetMapping("/{doctorId}")
    @AuthorizeScenario(ScenarioCode.ADMIN_DOCTOR_VIEW)
    public Result<AdminDoctorDetailResponse> detail(
            @PathVariable Long doctorId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return Result.ok(AuthAssembler.toAdminDoctorDetailResponse(
                getAdminDoctorDetailUseCase.handle(
                        new GetAdminDoctorDetailQuery(doctorId), auditApiSupport.currentContext(principal))));
    }

    @PostMapping
    @AuthorizeScenario(ScenarioCode.ADMIN_DOCTOR_CREATE)
    public Result<AdminDoctorDetailResponse> create(
            @RequestBody CreateAdminDoctorRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        try {
            return Result.ok(AuthAssembler.toAdminDoctorDetailResponse(createAdminDoctorUseCase.handle(
                    new CreateAdminDoctorCommand(
                            request.username(),
                            request.phone(),
                            request.password(),
                            request.displayName(),
                            request.hospitalId(),
                            request.professionalTitle(),
                            request.introductionMasked(),
                            request.departmentIds()),
                    auditApiSupport.currentContext(principal))));
        } catch (BizException exception) {
            auditApiSupport.recordAuditFailure(
                    AuditActionCodes.ADMIN_DOCTOR_CREATE,
                    AuditResourceTypes.DOCTOR_PROFILE,
                    null,
                    principal,
                    String.valueOf(exception.getCode()),
                    exception.getMessage(),
                    null,
                    null,
                    null);
            throw exception;
        }
    }

    @PutMapping("/{doctorId}")
    @AuthorizeScenario(ScenarioCode.ADMIN_DOCTOR_UPDATE)
    public Result<AdminDoctorDetailResponse> update(
            @PathVariable Long doctorId,
            @RequestBody UpdateAdminDoctorRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        try {
            return Result.ok(AuthAssembler.toAdminDoctorDetailResponse(updateAdminDoctorUseCase.handle(
                    new UpdateAdminDoctorCommand(
                            doctorId,
                            request.displayName(),
                            request.phone(),
                            request.professionalTitle(),
                            request.introductionMasked(),
                            request.departmentIds()),
                    auditApiSupport.currentContext(principal))));
        } catch (BizException exception) {
            auditApiSupport.recordAuditFailure(
                    AuditActionCodes.ADMIN_DOCTOR_UPDATE,
                    AuditResourceTypes.DOCTOR_PROFILE,
                    auditApiSupport.resourceIdOf(doctorId),
                    principal,
                    String.valueOf(exception.getCode()),
                    exception.getMessage(),
                    null,
                    null,
                    null);
            throw exception;
        }
    }

    @DeleteMapping("/{doctorId}")
    @AuthorizeScenario(ScenarioCode.ADMIN_DOCTOR_DELETE)
    public Result<Void> delete(
            @PathVariable Long doctorId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        try {
            deleteAdminDoctorUseCase.handle(
                    new DeleteAdminDoctorCommand(doctorId), auditApiSupport.currentContext(principal));
            return Result.ok();
        } catch (BizException exception) {
            auditApiSupport.recordAuditFailure(
                    AuditActionCodes.ADMIN_DOCTOR_DELETE,
                    AuditResourceTypes.DOCTOR_PROFILE,
                    auditApiSupport.resourceIdOf(doctorId),
                    principal,
                    String.valueOf(exception.getCode()),
                    exception.getMessage(),
                    null,
                    null,
                    null);
            throw exception;
        }
    }
}
