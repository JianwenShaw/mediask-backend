package me.jianwen.mediask.api.controller;

import me.jianwen.mediask.api.assembler.ClinicalAssembler;
import me.jianwen.mediask.api.audit.AuditActionCodes;
import me.jianwen.mediask.api.audit.AuditApiSupport;
import me.jianwen.mediask.api.audit.AuditResourceTypes;
import me.jianwen.mediask.api.dto.EncounterDetailResponse;
import me.jianwen.mediask.api.dto.EncounterListResponse;
import me.jianwen.mediask.api.dto.UpdateEncounterStatusRequest;
import me.jianwen.mediask.api.dto.UpdateEncounterStatusResponse;
import me.jianwen.mediask.api.security.AuthenticatedUserPrincipal;
import me.jianwen.mediask.application.authz.AuthorizeScenario;
import me.jianwen.mediask.application.authz.ScenarioCode;
import me.jianwen.mediask.application.clinical.usecase.GetEncounterDetailUseCase;
import me.jianwen.mediask.application.clinical.usecase.ListEncountersUseCase;
import me.jianwen.mediask.application.clinical.usecase.UpdateEncounterStatusUseCase;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.exception.ErrorCode;
import me.jianwen.mediask.common.result.Result;
import me.jianwen.mediask.domain.user.exception.UserErrorCode;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/encounters")
public class EncounterController {

    private final ListEncountersUseCase listEncountersUseCase;
    private final GetEncounterDetailUseCase getEncounterDetailUseCase;
    private final UpdateEncounterStatusUseCase updateEncounterStatusUseCase;
    private final AuditApiSupport auditApiSupport;

    public EncounterController(
            ListEncountersUseCase listEncountersUseCase,
            GetEncounterDetailUseCase getEncounterDetailUseCase,
            UpdateEncounterStatusUseCase updateEncounterStatusUseCase,
            AuditApiSupport auditApiSupport) {
        this.listEncountersUseCase = listEncountersUseCase;
        this.getEncounterDetailUseCase = getEncounterDetailUseCase;
        this.updateEncounterStatusUseCase = updateEncounterStatusUseCase;
        this.auditApiSupport = auditApiSupport;
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

    @PatchMapping("/{encounterId}")
    @AuthorizeScenario(ScenarioCode.ENCOUNTER_UPDATE)
    public Result<UpdateEncounterStatusResponse> updateStatus(
            @PathVariable Long encounterId,
            @RequestBody UpdateEncounterStatusRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (principal.doctorId() == null) {
            throw new BizException(UserErrorCode.ROLE_MISMATCH);
        }
        try {
            return Result.ok(ClinicalAssembler.toUpdateEncounterStatusResponse(updateEncounterStatusUseCase.handle(
                    ClinicalAssembler.toUpdateEncounterStatusCommand(encounterId, principal.doctorId(), request),
                    auditApiSupport.currentContext(principal))));
        } catch (BizException exception) {
            auditApiSupport.recordAuditFailure(
                    AuditActionCodes.ENCOUNTER_UPDATE,
                    AuditResourceTypes.ENCOUNTER,
                    auditApiSupport.resourceIdOf(encounterId),
                    principal,
                    String.valueOf(exception.getCode()),
                    exception.getMessage(),
                    null,
                    encounterId,
                    request.action());
            throw exception;
        }
    }
}
