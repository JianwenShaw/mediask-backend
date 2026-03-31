package me.jianwen.mediask.api.controller;

import java.util.List;
import me.jianwen.mediask.api.assembler.AuthAssembler;
import me.jianwen.mediask.api.dto.AdminPatientDetailResponse;
import me.jianwen.mediask.api.dto.AdminPatientListItemResponse;
import me.jianwen.mediask.api.dto.CreateAdminPatientRequest;
import me.jianwen.mediask.api.dto.UpdateAdminPatientRequest;
import me.jianwen.mediask.application.authz.AuthorizeScenario;
import me.jianwen.mediask.application.authz.ScenarioCode;
import me.jianwen.mediask.application.user.command.CreateAdminPatientCommand;
import me.jianwen.mediask.application.user.command.DeleteAdminPatientCommand;
import me.jianwen.mediask.application.user.command.UpdateAdminPatientCommand;
import me.jianwen.mediask.application.user.query.GetAdminPatientDetailQuery;
import me.jianwen.mediask.application.user.query.ListAdminPatientsQuery;
import me.jianwen.mediask.application.user.usecase.CreateAdminPatientUseCase;
import me.jianwen.mediask.application.user.usecase.DeleteAdminPatientUseCase;
import me.jianwen.mediask.application.user.usecase.GetAdminPatientDetailUseCase;
import me.jianwen.mediask.application.user.usecase.ListAdminPatientsUseCase;
import me.jianwen.mediask.application.user.usecase.UpdateAdminPatientUseCase;
import me.jianwen.mediask.common.result.Result;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/patients")
public class AdminPatientController {

    private final ListAdminPatientsUseCase listAdminPatientsUseCase;
    private final GetAdminPatientDetailUseCase getAdminPatientDetailUseCase;
    private final CreateAdminPatientUseCase createAdminPatientUseCase;
    private final UpdateAdminPatientUseCase updateAdminPatientUseCase;
    private final DeleteAdminPatientUseCase deleteAdminPatientUseCase;

    public AdminPatientController(
            ListAdminPatientsUseCase listAdminPatientsUseCase,
            GetAdminPatientDetailUseCase getAdminPatientDetailUseCase,
            CreateAdminPatientUseCase createAdminPatientUseCase,
            UpdateAdminPatientUseCase updateAdminPatientUseCase,
            DeleteAdminPatientUseCase deleteAdminPatientUseCase) {
        this.listAdminPatientsUseCase = listAdminPatientsUseCase;
        this.getAdminPatientDetailUseCase = getAdminPatientDetailUseCase;
        this.createAdminPatientUseCase = createAdminPatientUseCase;
        this.updateAdminPatientUseCase = updateAdminPatientUseCase;
        this.deleteAdminPatientUseCase = deleteAdminPatientUseCase;
    }

    @GetMapping
    @AuthorizeScenario(ScenarioCode.ADMIN_PATIENT_LIST)
    public Result<List<AdminPatientListItemResponse>> list(@RequestParam(required = false) String keyword) {
        return Result.ok(listAdminPatientsUseCase.handle(new ListAdminPatientsQuery(keyword)).stream()
                .map(AuthAssembler::toAdminPatientListItemResponse)
                .toList());
    }

    @GetMapping("/{patientId}")
    @AuthorizeScenario(ScenarioCode.ADMIN_PATIENT_VIEW)
    public Result<AdminPatientDetailResponse> detail(@PathVariable Long patientId) {
        return Result.ok(AuthAssembler.toAdminPatientDetailResponse(
                getAdminPatientDetailUseCase.handle(new GetAdminPatientDetailQuery(patientId))));
    }

    @PostMapping
    @AuthorizeScenario(ScenarioCode.ADMIN_PATIENT_CREATE)
    public Result<AdminPatientDetailResponse> create(@RequestBody CreateAdminPatientRequest request) {
        return Result.ok(AuthAssembler.toAdminPatientDetailResponse(createAdminPatientUseCase.handle(
                new CreateAdminPatientCommand(
                        request.username(),
                        request.password(),
                        request.displayName(),
                        request.mobileMasked(),
                        request.gender(),
                        request.birthDate(),
                        request.bloodType(),
                        request.allergySummary()))));
    }

    @PutMapping("/{patientId}")
    @AuthorizeScenario(ScenarioCode.ADMIN_PATIENT_UPDATE)
    public Result<AdminPatientDetailResponse> update(
            @PathVariable Long patientId, @RequestBody UpdateAdminPatientRequest request) {
        return Result.ok(AuthAssembler.toAdminPatientDetailResponse(updateAdminPatientUseCase.handle(
                new UpdateAdminPatientCommand(
                        patientId,
                        request.displayName(),
                        request.mobileMasked(),
                        request.gender(),
                        request.birthDate(),
                        request.bloodType(),
                        request.allergySummary()))));
    }

    @DeleteMapping("/{patientId}")
    @AuthorizeScenario(ScenarioCode.ADMIN_PATIENT_DELETE)
    public Result<Void> delete(@PathVariable Long patientId) {
        deleteAdminPatientUseCase.handle(new DeleteAdminPatientCommand(patientId));
        return Result.ok();
    }
}
