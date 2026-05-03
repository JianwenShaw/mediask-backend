package me.jianwen.mediask.api.controller;

import me.jianwen.mediask.api.assembler.ClinicalAssembler;
import me.jianwen.mediask.api.audit.AuditActionCodes;
import me.jianwen.mediask.api.audit.AuditApiSupport;
import me.jianwen.mediask.api.audit.AuditResourceTypes;
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
import me.jianwen.mediask.domain.audit.model.DataAccessPurposeCode;
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
    private final AuditApiSupport auditApiSupport;

    public PrescriptionController(
            CreatePrescriptionUseCase createPrescriptionUseCase,
            GetPrescriptionDetailUseCase getPrescriptionDetailUseCase,
            AuditApiSupport auditApiSupport) {
        this.createPrescriptionUseCase = createPrescriptionUseCase;
        this.getPrescriptionDetailUseCase = getPrescriptionDetailUseCase;
        this.auditApiSupport = auditApiSupport;
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

        try {
            var prescriptionOrder = createPrescriptionUseCase.handle(
                    ClinicalAssembler.toCreatePrescriptionCommand(request, principal.doctorId()),
                    auditApiSupport.currentContext(principal));
            return Result.ok(ClinicalAssembler.toCreatePrescriptionResponse(prescriptionOrder));
        } catch (BizException exception) {
            auditApiSupport.recordAuditFailure(
                    AuditActionCodes.PRESCRIPTION_CREATE,
                    AuditResourceTypes.PRESCRIPTION_ORDER,
                    auditApiSupport.resourceIdOf(request.encounterId()),
                    principal,
                    String.valueOf(exception.getCode()),
                    exception.getMessage(),
                    null,
                    request.encounterId(),
                    null);
            throw exception;
        }
    }

    @GetMapping("/{encounterId}")
    @AuthorizeScenario(ScenarioCode.PRESCRIPTION_READ)
    public Result<PrescriptionDetailResponse> detail(
            @PathVariable Long encounterId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (principal.doctorId() == null && principal.patientId() == null) {
            throw new BizException(UserErrorCode.ROLE_MISMATCH);
        }

        var prescriptionOrder = getPrescriptionDetailUseCase.handle(
                ClinicalAssembler.toGetPrescriptionDetailQuery(encounterId, principal.doctorId(), principal.userId()),
                auditApiSupport.currentContext(principal),
                principal.patientId() != null ? DataAccessPurposeCode.SELF_SERVICE : DataAccessPurposeCode.TREATMENT);
        return Result.ok(ClinicalAssembler.toPrescriptionDetailResponse(prescriptionOrder));
    }
}
