package me.jianwen.mediask.api.controller;

import me.jianwen.mediask.api.assembler.AuthAssembler;
import me.jianwen.mediask.api.dto.DoctorProfileResponse;
import me.jianwen.mediask.api.security.AuthenticatedUserPrincipal;
import me.jianwen.mediask.application.authz.AuthorizeScenario;
import me.jianwen.mediask.application.authz.ScenarioCode;
import me.jianwen.mediask.application.user.query.GetCurrentUserQuery;
import me.jianwen.mediask.application.user.usecase.GetCurrentDoctorProfileUseCase;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.exception.ErrorCode;
import me.jianwen.mediask.common.result.Result;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/doctors/me")
public class DoctorProfileController {

    private final GetCurrentDoctorProfileUseCase getCurrentDoctorProfileUseCase;

    public DoctorProfileController(GetCurrentDoctorProfileUseCase getCurrentDoctorProfileUseCase) {
        this.getCurrentDoctorProfileUseCase = getCurrentDoctorProfileUseCase;
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
}
