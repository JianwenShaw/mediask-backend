package me.jianwen.mediask.api.controller;

import me.jianwen.mediask.api.assembler.AuthAssembler;
import me.jianwen.mediask.api.audit.AuditActionCodes;
import me.jianwen.mediask.api.audit.AuditApiSupport;
import me.jianwen.mediask.api.audit.AuditResourceTypes;
import me.jianwen.mediask.api.dto.PatientProfileResponse;
import me.jianwen.mediask.api.dto.UpdatePatientProfileRequest;
import me.jianwen.mediask.api.security.AuthenticatedUserPrincipal;
import me.jianwen.mediask.application.authz.AuthorizeScenario;
import me.jianwen.mediask.application.authz.ScenarioCode;
import me.jianwen.mediask.application.user.command.UpdateCurrentPatientProfileCommand;
import me.jianwen.mediask.application.user.query.GetCurrentUserQuery;
import me.jianwen.mediask.application.user.usecase.GetCurrentPatientProfileUseCase;
import me.jianwen.mediask.application.user.usecase.UpdateCurrentPatientProfileUseCase;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.exception.ErrorCode;
import me.jianwen.mediask.common.result.Result;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/patients/me")
public class PatientProfileController {

    private final GetCurrentPatientProfileUseCase getCurrentPatientProfileUseCase;
    private final UpdateCurrentPatientProfileUseCase updateCurrentPatientProfileUseCase;
    private final AuditApiSupport auditApiSupport;

    public PatientProfileController(
            GetCurrentPatientProfileUseCase getCurrentPatientProfileUseCase,
            UpdateCurrentPatientProfileUseCase updateCurrentPatientProfileUseCase,
            AuditApiSupport auditApiSupport) {
        this.getCurrentPatientProfileUseCase = getCurrentPatientProfileUseCase;
        this.updateCurrentPatientProfileUseCase = updateCurrentPatientProfileUseCase;
        this.auditApiSupport = auditApiSupport;
    }

    @GetMapping("/profile")
    @AuthorizeScenario(ScenarioCode.PATIENT_SELF_PROFILE_VIEW)
    public Result<PatientProfileResponse> profile(@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        PatientProfileResponse response = AuthAssembler.toPatientProfileResponse(
                getCurrentPatientProfileUseCase.handle(
                        new GetCurrentUserQuery(principal.userId()), auditApiSupport.currentContext(principal)));
        return Result.ok(response);
    }

    @PutMapping("/profile")
    @AuthorizeScenario(ScenarioCode.PATIENT_SELF_PROFILE_UPDATE)
    public Result<Void> updateProfile(
            @RequestBody UpdatePatientProfileRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        try {
            updateCurrentPatientProfileUseCase.handle(new UpdateCurrentPatientProfileCommand(
                    principal.userId(),
                    request.gender(),
                    request.birthDate(),
                    request.bloodType(),
                    request.allergySummary()), auditApiSupport.currentContext(principal));
            return Result.ok();
        } catch (BizException exception) {
            auditApiSupport.recordAuditFailure(
                    AuditActionCodes.PATIENT_PROFILE_UPDATE,
                    AuditResourceTypes.PATIENT_PROFILE,
                    principal.patientId() == null ? null : String.valueOf(principal.patientId()),
                    principal,
                    String.valueOf(exception.getCode()),
                    exception.getMessage(),
                    principal.userId(),
                    null,
                    null);
            throw exception;
        }
    }
}
