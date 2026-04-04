package me.jianwen.mediask.api.controller;

import me.jianwen.mediask.api.assembler.ClinicalAssembler;
import me.jianwen.mediask.api.dto.EncounterListResponse;
import me.jianwen.mediask.api.security.AuthenticatedUserPrincipal;
import me.jianwen.mediask.application.authz.AuthorizeScenario;
import me.jianwen.mediask.application.authz.ScenarioCode;
import me.jianwen.mediask.application.clinical.usecase.ListEncountersUseCase;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.exception.ErrorCode;
import me.jianwen.mediask.common.result.Result;
import me.jianwen.mediask.domain.user.exception.UserErrorCode;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/encounters")
public class EncounterController {

    private final ListEncountersUseCase listEncountersUseCase;

    public EncounterController(ListEncountersUseCase listEncountersUseCase) {
        this.listEncountersUseCase = listEncountersUseCase;
    }

    @GetMapping
    @AuthorizeScenario(ScenarioCode.ENCOUNTER_LIST)
    public Result<EncounterListResponse> list(
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (principal.doctorId() == null) {
            throw new BizException(UserErrorCode.ROLE_MISMATCH);
        }
        return Result.ok(ClinicalAssembler.toEncounterListResponse(
                listEncountersUseCase.handle(ClinicalAssembler.toListEncountersQuery(principal.doctorId(), status))));
    }
}
