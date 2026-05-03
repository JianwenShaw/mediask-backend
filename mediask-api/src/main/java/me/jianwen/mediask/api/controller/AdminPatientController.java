package me.jianwen.mediask.api.controller;

import me.jianwen.mediask.api.assembler.AuthAssembler;
import me.jianwen.mediask.api.audit.AuditActionCodes;
import me.jianwen.mediask.api.audit.AuditApiSupport;
import me.jianwen.mediask.api.audit.AuditResourceTypes;
import me.jianwen.mediask.api.dto.AdminPatientDetailResponse;
import me.jianwen.mediask.api.dto.AdminPatientListItemResponse;
import me.jianwen.mediask.api.dto.CreateAdminPatientRequest;
import me.jianwen.mediask.api.dto.UpdateAdminPatientRequest;
import me.jianwen.mediask.application.authz.AuthorizeScenario;
import me.jianwen.mediask.application.authz.ScenarioCode;
import me.jianwen.mediask.application.user.command.CreateAdminPatientCommand;
import me.jianwen.mediask.application.user.command.DeleteAdminPatientCommand;
import me.jianwen.mediask.application.user.command.UpdateAdminPatientCommand;
import me.jianwen.mediask.application.user.query.GetAdminPatientDetailQuery;
import me.jianwen.mediask.application.user.query.ListAdminPatientsQuery;
import me.jianwen.mediask.application.user.usecase.CreateAdminPatientUseCase;
import me.jianwen.mediask.application.user.usecase.DeleteAdminPatientUseCase;
import me.jianwen.mediask.application.user.usecase.GetAdminPatientDetailUseCase;
import me.jianwen.mediask.application.user.usecase.ListAdminPatientsUseCase;
import me.jianwen.mediask.application.user.usecase.UpdateAdminPatientUseCase;
import me.jianwen.mediask.common.pagination.PageData;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.result.Result;
import me.jianwen.mediask.api.security.AuthenticatedUserPrincipal;
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
@RequestMapping("/api/v1/admin/patients")
public class AdminPatientController {

    private final ListAdminPatientsUseCase listAdminPatientsUseCase;
    private final GetAdminPatientDetailUseCase getAdminPatientDetailUseCase;
    private final CreateAdminPatientUseCase createAdminPatientUseCase;
    private final UpdateAdminPatientUseCase updateAdminPatientUseCase;
    private final DeleteAdminPatientUseCase deleteAdminPatientUseCase;
    private final AuditApiSupport auditApiSupport;

    public AdminPatientController(
            ListAdminPatientsUseCase listAdminPatientsUseCase,
            GetAdminPatientDetailUseCase getAdminPatientDetailUseCase,
            CreateAdminPatientUseCase createAdminPatientUseCase,
            UpdateAdminPatientUseCase updateAdminPatientUseCase,
            DeleteAdminPatientUseCase deleteAdminPatientUseCase,
            AuditApiSupport auditApiSupport) {
        this.listAdminPatientsUseCase = listAdminPatientsUseCase;
        this.getAdminPatientDetailUseCase = getAdminPatientDetailUseCase;
        this.createAdminPatientUseCase = createAdminPatientUseCase;
        this.updateAdminPatientUseCase = updateAdminPatientUseCase;
        this.deleteAdminPatientUseCase = deleteAdminPatientUseCase;
        this.auditApiSupport = auditApiSupport;
    }

    @GetMapping
    @AuthorizeScenario(ScenarioCode.ADMIN_PATIENT_LIST)
    public Result<PageData<AdminPatientListItemResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long pageNum,
            @RequestParam(required = false) Long pageSize) {
        PageData<AdminPatientListItemResponse> response = listAdminPatientsUseCase
                .handle(ListAdminPatientsQuery.page(keyword, pageNum, pageSize))
                .map(AuthAssembler::toAdminPatientListItemResponse);
        return Result.ok(response);
    }

    @GetMapping("/{patientId}")
    @AuthorizeScenario(ScenarioCode.ADMIN_PATIENT_VIEW)
    public Result<AdminPatientDetailResponse> detail(
            @PathVariable Long patientId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return Result.ok(AuthAssembler.toAdminPatientDetailResponse(
                getAdminPatientDetailUseCase.handle(
                        new GetAdminPatientDetailQuery(patientId), auditApiSupport.currentContext(principal))));
    }

    @PostMapping
    @AuthorizeScenario(ScenarioCode.ADMIN_PATIENT_CREATE)
    public Result<AdminPatientDetailResponse> create(
            @RequestBody CreateAdminPatientRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        try {
            return Result.ok(AuthAssembler.toAdminPatientDetailResponse(createAdminPatientUseCase.handle(
                    new CreateAdminPatientCommand(
                            request.username(),
                            request.password(),
                            request.displayName(),
                            request.mobileMasked(),
                            request.gender(),
                            request.birthDate(),
                            request.bloodType(),
                            request.allergySummary()),
                    auditApiSupport.currentContext(principal))));
        } catch (BizException exception) {
            auditApiSupport.recordAuditFailure(
                    AuditActionCodes.ADMIN_PATIENT_CREATE,
                    AuditResourceTypes.PATIENT_PROFILE,
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

    @PutMapping("/{patientId}")
    @AuthorizeScenario(ScenarioCode.ADMIN_PATIENT_UPDATE)
    public Result<AdminPatientDetailResponse> update(
            @PathVariable Long patientId,
            @RequestBody UpdateAdminPatientRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        try {
            return Result.ok(AuthAssembler.toAdminPatientDetailResponse(updateAdminPatientUseCase.handle(
                    new UpdateAdminPatientCommand(
                            patientId,
                            request.displayName(),
                            request.mobileMasked(),
                            request.gender(),
                            request.birthDate(),
                            request.bloodType(),
                            request.allergySummary()),
                    auditApiSupport.currentContext(principal))));
        } catch (BizException exception) {
            auditApiSupport.recordAuditFailure(
                    AuditActionCodes.ADMIN_PATIENT_UPDATE,
                    AuditResourceTypes.PATIENT_PROFILE,
                    auditApiSupport.resourceIdOf(patientId),
                    principal,
                    String.valueOf(exception.getCode()),
                    exception.getMessage(),
                    null,
                    null,
                    null);
            throw exception;
        }
    }

    @DeleteMapping("/{patientId}")
    @AuthorizeScenario(ScenarioCode.ADMIN_PATIENT_DELETE)
    public Result<Void> delete(
            @PathVariable Long patientId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        try {
            deleteAdminPatientUseCase.handle(
                    new DeleteAdminPatientCommand(patientId), auditApiSupport.currentContext(principal));
            return Result.ok();
        } catch (BizException exception) {
            auditApiSupport.recordAuditFailure(
                    AuditActionCodes.ADMIN_PATIENT_DELETE,
                    AuditResourceTypes.PATIENT_PROFILE,
                    auditApiSupport.resourceIdOf(patientId),
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
