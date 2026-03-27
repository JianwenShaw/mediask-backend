package me.jianwen.mediask.api.controller;

import me.jianwen.mediask.api.assembler.AuthAssembler;
import me.jianwen.mediask.api.dto.DoctorProfileResponse;
import me.jianwen.mediask.api.dto.UpdateDoctorProfileRequest;
import me.jianwen.mediask.api.security.AuthenticatedUserPrincipal;
import me.jianwen.mediask.application.authz.AuthorizeScenario;
import me.jianwen.mediask.application.authz.ScenarioCode;
import me.jianwen.mediask.application.user.command.UpdateCurrentDoctorProfileCommand;
import me.jianwen.mediask.application.user.query.GetCurrentUserQuery;
import me.jianwen.mediask.application.user.usecase.GetCurrentDoctorProfileUseCase;
import me.jianwen.mediask.application.user.usecase.UpdateCurrentDoctorProfileUseCase;
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
@RequestMapping("/api/v1/doctors/me")
public class DoctorProfileController {

    private final GetCurrentDoctorProfileUseCase getCurrentDoctorProfileUseCase;
    private final UpdateCurrentDoctorProfileUseCase updateCurrentDoctorProfileUseCase;

    public DoctorProfileController(
            GetCurrentDoctorProfileUseCase getCurrentDoctorProfileUseCase,
            UpdateCurrentDoctorProfileUseCase updateCurrentDoctorProfileUseCase) {
        this.getCurrentDoctorProfileUseCase = getCurrentDoctorProfileUseCase;
        this.updateCurrentDoctorProfileUseCase = updateCurrentDoctorProfileUseCase;
    }

    @GetMapping("/profile")
    @AuthorizeScenario(ScenarioCode.DOCTOR_SELF_PROFILE_VIEW)
    public Result<DoctorProfileResponse> profile(@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        DoctorProfileResponse response = AuthAssembler.toDoctorProfileResponse(
                getCurrentDoctorProfileUseCase.handle(new GetCurrentUserQuery(principal.userId())));
        return Result.ok(response);
    }

    @PutMapping("/profile")
    @AuthorizeScenario(ScenarioCode.DOCTOR_SELF_PROFILE_UPDATE)
    public Result<Void> updateProfile(
            @RequestBody UpdateDoctorProfileRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        updateCurrentDoctorProfileUseCase.handle(new UpdateCurrentDoctorProfileCommand(
                principal.userId(),
                request.professionalTitle(),
                request.introductionMasked()));
        return Result.ok();
    }
}
