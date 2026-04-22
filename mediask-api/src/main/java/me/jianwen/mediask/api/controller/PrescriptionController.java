package me.jianwen.mediask.api.controller;

import me.jianwen.mediask.api.assembler.ClinicalAssembler;
import me.jianwen.mediask.api.dto.CreatePrescriptionRequest;
import me.jianwen.mediask.api.dto.CreatePrescriptionResponse;
import me.jianwen.mediask.api.dto.PrescriptionDetailResponse;
import me.jianwen.mediask.api.security.AuthenticatedUserPrincipal;
import me.jianwen.mediask.application.authz.AuthorizeScenario;
import me.jianwen.mediask.application.authz.ScenarioCode;
import me.jianwen.mediask.application.clinical.usecase.CreatePrescriptionUseCase;
import me.jianwen.mediask.application.clinical.usecase.GetPrescriptionDetailUseCase;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.exception.ErrorCode;
import me.jianwen.mediask.common.result.Result;
import me.jianwen.mediask.domain.user.exception.UserErrorCode;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/prescriptions")
public class PrescriptionController {

    private final CreatePrescriptionUseCase createPrescriptionUseCase;
    private final GetPrescriptionDetailUseCase getPrescriptionDetailUseCase;

    public PrescriptionController(
            CreatePrescriptionUseCase createPrescriptionUseCase,
            GetPrescriptionDetailUseCase getPrescriptionDetailUseCase) {
        this.createPrescriptionUseCase = createPrescriptionUseCase;
        this.getPrescriptionDetailUseCase = getPrescriptionDetailUseCase;
    }

    @PostMapping
    @AuthorizeScenario(ScenarioCode.PRESCRIPTION_CREATE)
    public Result<CreatePrescriptionResponse> create(
            @RequestBody CreatePrescriptionRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (principal.doctorId() == null) {
            throw new BizException(UserErrorCode.ROLE_MISMATCH);
        }

        var prescriptionOrder = createPrescriptionUseCase.handle(
                ClinicalAssembler.toCreatePrescriptionCommand(request, principal.doctorId()));
        return Result.ok(ClinicalAssembler.toCreatePrescriptionResponse(prescriptionOrder));
    }

    @GetMapping("/{encounterId}")
    @AuthorizeScenario(ScenarioCode.PRESCRIPTION_READ)
    public Result<PrescriptionDetailResponse> detail(
            @PathVariable Long encounterId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (principal.doctorId() == null) {
            throw new BizException(UserErrorCode.ROLE_MISMATCH);
        }

        var prescriptionOrder = getPrescriptionDetailUseCase.handle(
                ClinicalAssembler.toGetPrescriptionDetailQuery(encounterId, principal.doctorId()));
        return Result.ok(ClinicalAssembler.toPrescriptionDetailResponse(prescriptionOrder));
    }
}
