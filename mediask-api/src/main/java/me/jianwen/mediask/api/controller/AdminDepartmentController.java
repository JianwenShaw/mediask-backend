package me.jianwen.mediask.api.controller;

import me.jianwen.mediask.api.assembler.AuthAssembler;
import me.jianwen.mediask.api.audit.AuditActionCodes;
import me.jianwen.mediask.api.audit.AuditApiSupport;
import me.jianwen.mediask.api.audit.AuditResourceTypes;
import me.jianwen.mediask.api.dto.AdminDepartmentDetailResponse;
import me.jianwen.mediask.api.dto.AdminDepartmentListItemResponse;
import me.jianwen.mediask.api.dto.CreateAdminDepartmentRequest;
import me.jianwen.mediask.api.dto.UpdateAdminDepartmentRequest;
import me.jianwen.mediask.api.security.AuthenticatedUserPrincipal;
import me.jianwen.mediask.application.authz.AuthorizeScenario;
import me.jianwen.mediask.application.authz.ScenarioCode;
import me.jianwen.mediask.application.user.command.CreateAdminDepartmentCommand;
import me.jianwen.mediask.application.user.command.DeleteAdminDepartmentCommand;
import me.jianwen.mediask.application.user.command.UpdateAdminDepartmentCommand;
import me.jianwen.mediask.application.user.query.GetAdminDepartmentDetailQuery;
import me.jianwen.mediask.application.user.query.ListAdminDepartmentsQuery;
import me.jianwen.mediask.application.user.usecase.CreateAdminDepartmentUseCase;
import me.jianwen.mediask.application.user.usecase.DeleteAdminDepartmentUseCase;
import me.jianwen.mediask.application.user.usecase.GetAdminDepartmentDetailUseCase;
import me.jianwen.mediask.application.user.usecase.ListAdminDepartmentsUseCase;
import me.jianwen.mediask.application.user.usecase.UpdateAdminDepartmentUseCase;
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
@RequestMapping("/api/v1/admin/departments")
public class AdminDepartmentController {

    private final ListAdminDepartmentsUseCase listAdminDepartmentsUseCase;
    private final GetAdminDepartmentDetailUseCase getAdminDepartmentDetailUseCase;
    private final CreateAdminDepartmentUseCase createAdminDepartmentUseCase;
    private final UpdateAdminDepartmentUseCase updateAdminDepartmentUseCase;
    private final DeleteAdminDepartmentUseCase deleteAdminDepartmentUseCase;
    private final AuditApiSupport auditApiSupport;

    public AdminDepartmentController(
            ListAdminDepartmentsUseCase listAdminDepartmentsUseCase,
            GetAdminDepartmentDetailUseCase getAdminDepartmentDetailUseCase,
            CreateAdminDepartmentUseCase createAdminDepartmentUseCase,
            UpdateAdminDepartmentUseCase updateAdminDepartmentUseCase,
            DeleteAdminDepartmentUseCase deleteAdminDepartmentUseCase,
            AuditApiSupport auditApiSupport) {
        this.listAdminDepartmentsUseCase = listAdminDepartmentsUseCase;
        this.getAdminDepartmentDetailUseCase = getAdminDepartmentDetailUseCase;
        this.createAdminDepartmentUseCase = createAdminDepartmentUseCase;
        this.updateAdminDepartmentUseCase = updateAdminDepartmentUseCase;
        this.deleteAdminDepartmentUseCase = deleteAdminDepartmentUseCase;
        this.auditApiSupport = auditApiSupport;
    }

    @GetMapping
    @AuthorizeScenario(ScenarioCode.ADMIN_DEPARTMENT_LIST)
    public Result<PageData<AdminDepartmentListItemResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long pageNum,
            @RequestParam(required = false) Long pageSize) {
        PageData<AdminDepartmentListItemResponse> response = listAdminDepartmentsUseCase
                .handle(ListAdminDepartmentsQuery.page(keyword, pageNum, pageSize))
                .map(AuthAssembler::toAdminDepartmentListItemResponse);
        return Result.ok(response);
    }

    @GetMapping("/{id}")
    @AuthorizeScenario(ScenarioCode.ADMIN_DEPARTMENT_VIEW)
    public Result<AdminDepartmentDetailResponse> detail(@PathVariable Long id) {
        return Result.ok(AuthAssembler.toAdminDepartmentDetailResponse(
                getAdminDepartmentDetailUseCase.handle(new GetAdminDepartmentDetailQuery(id))));
    }

    @PostMapping
    @AuthorizeScenario(ScenarioCode.ADMIN_DEPARTMENT_CREATE)
    public Result<AdminDepartmentDetailResponse> create(
            @RequestBody CreateAdminDepartmentRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        try {
            return Result.ok(AuthAssembler.toAdminDepartmentDetailResponse(createAdminDepartmentUseCase.handle(
                    new CreateAdminDepartmentCommand(
                            request.hospitalId(),
                            request.name(),
                            request.deptType()),
                    auditApiSupport.currentContext(principal))));
        } catch (BizException exception) {
            auditApiSupport.recordAuditFailure(
                    AuditActionCodes.ADMIN_DEPARTMENT_CREATE,
                    AuditResourceTypes.DEPARTMENT,
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

    @PutMapping("/{id}")
    @AuthorizeScenario(ScenarioCode.ADMIN_DEPARTMENT_UPDATE)
    public Result<AdminDepartmentDetailResponse> update(
            @PathVariable Long id,
            @RequestBody UpdateAdminDepartmentRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        try {
            return Result.ok(AuthAssembler.toAdminDepartmentDetailResponse(updateAdminDepartmentUseCase.handle(
                    new UpdateAdminDepartmentCommand(
                            id,
                            request.name(),
                            request.deptType(),
                            request.status(),
                            request.sortOrder()),
                    auditApiSupport.currentContext(principal))));
        } catch (BizException exception) {
            auditApiSupport.recordAuditFailure(
                    AuditActionCodes.ADMIN_DEPARTMENT_UPDATE,
                    AuditResourceTypes.DEPARTMENT,
                    auditApiSupport.resourceIdOf(id),
                    principal,
                    String.valueOf(exception.getCode()),
                    exception.getMessage(),
                    null,
                    null,
                    null);
            throw exception;
        }
    }

    @DeleteMapping("/{id}")
    @AuthorizeScenario(ScenarioCode.ADMIN_DEPARTMENT_DELETE)
    public Result<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        try {
            deleteAdminDepartmentUseCase.handle(
                    new DeleteAdminDepartmentCommand(id), auditApiSupport.currentContext(principal));
            return Result.ok();
        } catch (BizException exception) {
            auditApiSupport.recordAuditFailure(
                    AuditActionCodes.ADMIN_DEPARTMENT_DELETE,
                    AuditResourceTypes.DEPARTMENT,
                    auditApiSupport.resourceIdOf(id),
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
