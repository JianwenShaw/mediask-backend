package me.jianwen.mediask.api.controller;
import me.jianwen.mediask.api.assembler.OutpatientAssembler;
import me.jianwen.mediask.api.dto.CreateRegistrationRequest;
import me.jianwen.mediask.api.dto.CreateRegistrationResponse;
import me.jianwen.mediask.api.security.AuthenticatedUserPrincipal;
import me.jianwen.mediask.application.outpatient.command.CreateRegistrationCommand;
import me.jianwen.mediask.application.outpatient.usecase.CreateRegistrationResult;
import me.jianwen.mediask.application.outpatient.usecase.CreateRegistrationUseCase;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.exception.ErrorCode;
import me.jianwen.mediask.common.result.Result;
import me.jianwen.mediask.domain.user.exception.UserErrorCode;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/registrations")
public class RegistrationController {

    private final CreateRegistrationUseCase createRegistrationUseCase;

    public RegistrationController(CreateRegistrationUseCase createRegistrationUseCase) {
        this.createRegistrationUseCase = createRegistrationUseCase;
    }

    @PostMapping
    public Result<CreateRegistrationResponse> create(
            @RequestBody CreateRegistrationRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (principal.patientId() == null) {
            throw new BizException(UserErrorCode.ROLE_MISMATCH);
        }
        CreateRegistrationCommand command =
                OutpatientAssembler.toCreateRegistrationCommand(principal.patientId(), request);
        CreateRegistrationResult result = createRegistrationUseCase.handle(command);
        return Result.ok(OutpatientAssembler.toCreateRegistrationResponse(result));
    }
}
