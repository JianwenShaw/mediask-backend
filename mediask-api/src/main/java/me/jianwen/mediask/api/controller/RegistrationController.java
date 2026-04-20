package me.jianwen.mediask.api.controller;

import me.jianwen.mediask.api.assembler.OutpatientAssembler;
import me.jianwen.mediask.api.dto.CancelRegistrationResponse;
import me.jianwen.mediask.api.dto.CreateRegistrationRequest;
import me.jianwen.mediask.api.dto.CreateRegistrationResponse;
import me.jianwen.mediask.api.dto.RegistrationDetailResponse;
import me.jianwen.mediask.api.dto.RegistrationListResponse;
import me.jianwen.mediask.api.security.AuthenticatedUserPrincipal;
import me.jianwen.mediask.application.outpatient.usecase.CancelRegistrationUseCase;
import me.jianwen.mediask.application.outpatient.command.CreateRegistrationCommand;
import me.jianwen.mediask.application.outpatient.usecase.CreateRegistrationResult;
import me.jianwen.mediask.application.outpatient.usecase.CreateRegistrationUseCase;
import me.jianwen.mediask.application.outpatient.usecase.GetRegistrationDetailUseCase;
import me.jianwen.mediask.application.outpatient.usecase.ListRegistrationsUseCase;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.exception.ErrorCode;
import me.jianwen.mediask.common.result.Result;
import me.jianwen.mediask.domain.user.exception.UserErrorCode;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/registrations")
public class RegistrationController {

    private final CreateRegistrationUseCase createRegistrationUseCase;
    private final ListRegistrationsUseCase listRegistrationsUseCase;
    private final GetRegistrationDetailUseCase getRegistrationDetailUseCase;
    private final CancelRegistrationUseCase cancelRegistrationUseCase;

    public RegistrationController(
            CreateRegistrationUseCase createRegistrationUseCase,
            ListRegistrationsUseCase listRegistrationsUseCase,
            GetRegistrationDetailUseCase getRegistrationDetailUseCase,
            CancelRegistrationUseCase cancelRegistrationUseCase) {
        this.createRegistrationUseCase = createRegistrationUseCase;
        this.listRegistrationsUseCase = listRegistrationsUseCase;
        this.getRegistrationDetailUseCase = getRegistrationDetailUseCase;
        this.cancelRegistrationUseCase = cancelRegistrationUseCase;
    }

    @PostMapping
    public Result<CreateRegistrationResponse> create(
            @RequestBody CreateRegistrationRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (principal.patientId() == null) {
            throw new BizException(UserErrorCode.ROLE_MISMATCH);
        }
        CreateRegistrationCommand command =
                OutpatientAssembler.toCreateRegistrationCommand(principal.userId(), request);
        CreateRegistrationResult result = createRegistrationUseCase.handle(command);
        return Result.ok(OutpatientAssembler.toCreateRegistrationResponse(result));
    }

    @GetMapping
    public Result<RegistrationListResponse> list(
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (principal.patientId() == null) {
            throw new BizException(UserErrorCode.ROLE_MISMATCH);
        }
        return Result.ok(OutpatientAssembler.toRegistrationListResponse(
                listRegistrationsUseCase.handle(OutpatientAssembler.toListRegistrationsQuery(principal.userId(), status))));
    }

    @GetMapping("/{registrationId}")
    public Result<RegistrationDetailResponse> detail(
            @PathVariable Long registrationId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (principal.patientId() == null) {
            throw new BizException(UserErrorCode.ROLE_MISMATCH);
        }
        return Result.ok(OutpatientAssembler.toRegistrationDetailResponse(
                getRegistrationDetailUseCase.handle(
                        OutpatientAssembler.toGetRegistrationDetailQuery(principal.userId(), registrationId))));
    }

    @PatchMapping("/{registrationId}/cancel")
    public Result<CancelRegistrationResponse> cancel(
            @PathVariable Long registrationId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (principal.patientId() == null) {
            throw new BizException(UserErrorCode.ROLE_MISMATCH);
        }
        return Result.ok(OutpatientAssembler.toCancelRegistrationResponse(
                cancelRegistrationUseCase.handle(
                        OutpatientAssembler.toCancelRegistrationCommand(principal.userId(), registrationId))));
    }
}
