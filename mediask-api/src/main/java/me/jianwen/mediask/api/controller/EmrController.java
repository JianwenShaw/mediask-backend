package me.jianwen.mediask.api.controller;

import me.jianwen.mediask.api.assembler.ClinicalAssembler;
import me.jianwen.mediask.api.dto.CreateEmrRequest;
import me.jianwen.mediask.api.dto.CreateEmrResponse;
import me.jianwen.mediask.api.dto.EmrDetailResponse;
import me.jianwen.mediask.api.security.AuthenticatedUserPrincipal;
import me.jianwen.mediask.application.authz.AuthorizeScenario;
import me.jianwen.mediask.application.authz.ScenarioCode;
import me.jianwen.mediask.application.clinical.usecase.CreateEmrUseCase;
import me.jianwen.mediask.application.clinical.usecase.GetEmrDetailUseCase;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.exception.ErrorCode;
import me.jianwen.mediask.common.result.Result;
import me.jianwen.mediask.domain.user.exception.UserErrorCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/emr")
public class EmrController {

    private final CreateEmrUseCase createEmrUseCase;
    private final GetEmrDetailUseCase getEmrDetailUseCase;

    public EmrController(CreateEmrUseCase createEmrUseCase, GetEmrDetailUseCase getEmrDetailUseCase) {
        this.createEmrUseCase = createEmrUseCase;
        this.getEmrDetailUseCase = getEmrDetailUseCase;
    }

    @PostMapping
    @AuthorizeScenario(ScenarioCode.EMR_RECORD_CREATE)
    public Result<CreateEmrResponse> create(
            @RequestBody CreateEmrRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (principal.doctorId() == null) {
            throw new BizException(UserErrorCode.ROLE_MISMATCH);
        }

        var command = ClinicalAssembler.toCreateEmrCommand(request, principal.doctorId());
        var emrRecord = createEmrUseCase.handle(command);
        return Result.ok(ClinicalAssembler.toCreateEmrResponse(emrRecord));
    }

    @GetMapping("/{encounterId}")
    @AuthorizeScenario(ScenarioCode.EMR_RECORD_READ)
    public Result<EmrDetailResponse> detail(
            @PathVariable Long encounterId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (principal.doctorId() == null && principal.patientId() == null) {
            throw new BizException(UserErrorCode.ROLE_MISMATCH);
        }

        var query = ClinicalAssembler.toGetEmrDetailQuery(encounterId);
        var emrRecord = getEmrDetailUseCase.handle(query);
        return Result.ok(ClinicalAssembler.toEmrDetailResponse(emrRecord));
    }
}
