package me.jianwen.mediask.api.controller;

import me.jianwen.mediask.api.assembler.ClinicalAssembler;
import me.jianwen.mediask.api.dto.EncounterAiSummaryResponse;
import me.jianwen.mediask.api.dto.EncounterDetailResponse;
import me.jianwen.mediask.api.dto.EncounterListResponse;
import me.jianwen.mediask.api.security.AuthenticatedUserPrincipal;
import me.jianwen.mediask.application.authz.AuthorizeScenario;
import me.jianwen.mediask.application.authz.ScenarioCode;
import me.jianwen.mediask.application.clinical.usecase.GetEncounterAiSummaryUseCase;
import me.jianwen.mediask.application.clinical.usecase.GetEncounterDetailUseCase;
import me.jianwen.mediask.application.clinical.usecase.ListEncountersUseCase;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.exception.ErrorCode;
import me.jianwen.mediask.common.result.Result;
import me.jianwen.mediask.domain.user.exception.UserErrorCode;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/encounters")
public class EncounterController {

    private final ListEncountersUseCase listEncountersUseCase;
    private final GetEncounterDetailUseCase getEncounterDetailUseCase;
    private final GetEncounterAiSummaryUseCase getEncounterAiSummaryUseCase;

    public EncounterController(
            ListEncountersUseCase listEncountersUseCase,
            GetEncounterDetailUseCase getEncounterDetailUseCase,
            GetEncounterAiSummaryUseCase getEncounterAiSummaryUseCase) {
        this.listEncountersUseCase = listEncountersUseCase;
        this.getEncounterDetailUseCase = getEncounterDetailUseCase;
        this.getEncounterAiSummaryUseCase = getEncounterAiSummaryUseCase;
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

    @GetMapping("/{encounterId}")
    @AuthorizeScenario(ScenarioCode.ENCOUNTER_LIST)
    public Result<EncounterDetailResponse> detail(
            @PathVariable Long encounterId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (principal.doctorId() == null) {
            throw new BizException(UserErrorCode.ROLE_MISMATCH);
        }
        return Result.ok(ClinicalAssembler.toEncounterDetailResponse(getEncounterDetailUseCase.handle(
                ClinicalAssembler.toGetEncounterDetailQuery(encounterId, principal.doctorId()))));
    }

    @GetMapping("/{encounterId}/ai-summary")
    @AuthorizeScenario(ScenarioCode.ENCOUNTER_LIST)
    public Result<EncounterAiSummaryResponse> aiSummary(
            @PathVariable Long encounterId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (principal.doctorId() == null) {
            throw new BizException(UserErrorCode.ROLE_MISMATCH);
        }
        return Result.ok(ClinicalAssembler.toEncounterAiSummaryResponse(getEncounterAiSummaryUseCase.handle(
                ClinicalAssembler.toGetEncounterAiSummaryQuery(encounterId, principal.doctorId()))));
    }
}
