package me.jianwen.mediask.api.controller;

import me.jianwen.mediask.api.assembler.AuthAssembler;
import me.jianwen.mediask.api.dto.PatientProfileResponse;
import me.jianwen.mediask.api.security.AuthenticatedUserPrincipal;
import me.jianwen.mediask.application.user.query.GetCurrentUserQuery;
import me.jianwen.mediask.application.user.usecase.GetCurrentPatientProfileUseCase;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.exception.ErrorCode;
import me.jianwen.mediask.common.result.Result;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/patients/me")
public class PatientProfileController {

    private final GetCurrentPatientProfileUseCase getCurrentPatientProfileUseCase;

    public PatientProfileController(GetCurrentPatientProfileUseCase getCurrentPatientProfileUseCase) {
        this.getCurrentPatientProfileUseCase = getCurrentPatientProfileUseCase;
    }

    @GetMapping("/profile")
    @PreAuthorize("hasAuthority('patient:profile:view:self')")
    public Result<PatientProfileResponse> profile(@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        PatientProfileResponse response = AuthAssembler.toPatientProfileResponse(
                getCurrentPatientProfileUseCase.handle(new GetCurrentUserQuery(principal.userId())));
        return Result.ok(response);
    }
}
