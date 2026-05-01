package me.jianwen.mediask.application.ai.usecase;

import java.util.List;
import me.jianwen.mediask.application.ai.command.SubmitAiTriageQueryCommand;
import me.jianwen.mediask.common.exception.ErrorCode;
import me.jianwen.mediask.common.exception.SysException;
import me.jianwen.mediask.domain.ai.model.AiTriageCitation;
import me.jianwen.mediask.domain.ai.model.AiTriageGatewayContext;
import me.jianwen.mediask.domain.ai.model.AiTriageQuery;
import me.jianwen.mediask.domain.ai.model.AiTriageQueryResponse;
import me.jianwen.mediask.domain.ai.model.AiTriageRecommendedDepartment;
import me.jianwen.mediask.domain.ai.model.AiTriageResult;
import me.jianwen.mediask.domain.ai.model.AiTriageResultSnapshot;
import me.jianwen.mediask.domain.ai.port.AiTriageGatewayPort;
import me.jianwen.mediask.domain.ai.port.AiTriageResultSnapshotRepository;
import me.jianwen.mediask.domain.triage.model.CatalogVersion;
import me.jianwen.mediask.domain.triage.model.TriageCatalog;
import me.jianwen.mediask.domain.triage.port.TriageCatalogPublishPort;
import me.jianwen.mediask.domain.triage.service.TriageCatalogValidator;

public class SubmitAiTriageQueryUseCase {

    private final AiTriageGatewayPort aiTriageGatewayPort;
    private final AiTriageResultSnapshotRepository snapshotRepository;
    private final TriageCatalogPublishPort triageCatalogPublishPort;

    public SubmitAiTriageQueryUseCase(
            AiTriageGatewayPort aiTriageGatewayPort,
            AiTriageResultSnapshotRepository snapshotRepository,
            TriageCatalogPublishPort triageCatalogPublishPort) {
        this.aiTriageGatewayPort = aiTriageGatewayPort;
        this.snapshotRepository = snapshotRepository;
        this.triageCatalogPublishPort = triageCatalogPublishPort;
    }

    public AiTriageQueryResponse handle(SubmitAiTriageQueryCommand command) {
        AiTriageQueryResponse response = aiTriageGatewayPort.query(
                new AiTriageGatewayContext(command.requestId(), command.patientUserId()),
                new AiTriageQuery(command.sessionId(), command.hospitalScope(), command.userMessage()));
        persistIfFinalized(command.hospitalScope(), response);
        return response;
    }

    public void persistIfFinalized(String hospitalScope, AiTriageQueryResponse response) {
        AiTriageResult triageResult = requireResponse(response).triageResult();
        if (triageResult.isCollecting()) {
            validateCollecting(triageResult);
            return;
        }
        if (triageResult.isReady()) {
            validateReady(hospitalScope, triageResult);
        } else if (triageResult.isBlocked()) {
            validateBlocked(triageResult);
        } else {
            throw invalid("unsupported triage stage: " + triageResult.triageStage());
        }
        snapshotRepository.save(toSnapshot(hospitalScope, response));
    }

    private AiTriageQueryResponse requireResponse(AiTriageQueryResponse response) {
        if (response == null) {
            throw invalid("query response is null");
        }
        if (isBlank(response.requestId())
                || isBlank(response.sessionId())
                || isBlank(response.turnId())
                || isBlank(response.queryRunId())
                || response.triageResult() == null) {
            throw invalid("query response envelope is incomplete");
        }
        return response;
    }

    private void validateCollecting(AiTriageResult triageResult) {
        if (!"CONTINUE_TRIAGE".equals(triageResult.nextAction())) {
            throw invalid("collecting result nextAction is invalid");
        }
        List<String> followUpQuestions = defaultList(triageResult.followUpQuestions());
        if (followUpQuestions.isEmpty() || followUpQuestions.size() > 2) {
            throw invalid("collecting result followUpQuestions is invalid");
        }
        if (!defaultList(triageResult.recommendedDepartments()).isEmpty()
                || !isBlank(triageResult.blockedReason())
                || !isBlank(triageResult.careAdvice())) {
            throw invalid("collecting result contains finalized fields");
        }
    }

    private void validateReady(String hospitalScope, AiTriageResult triageResult) {
        if (!"VIEW_TRIAGE_RESULT".equals(triageResult.nextAction())) {
            throw invalid("ready result nextAction is invalid");
        }
        if (isBlank(triageResult.riskLevel())) {
            throw invalid("ready result riskLevel is required");
        }
        if (isBlank(triageResult.catalogVersion())) {
            throw invalid("ready result catalogVersion is required");
        }
        List<AiTriageRecommendedDepartment> departments = defaultList(triageResult.recommendedDepartments());
        if (departments.isEmpty() || departments.size() > 3) {
            throw invalid("ready result recommendedDepartments is invalid");
        }
        TriageCatalog catalog = triageCatalogPublishPort
                .findCatalogByVersion(hospitalScope, parseCatalogVersion(triageResult.catalogVersion()))
                .orElseThrow(() -> invalid("triage catalog version not found: " + triageResult.catalogVersion()));
        for (AiTriageRecommendedDepartment department : departments) {
            if (department == null || department.departmentId() == null || isBlank(department.departmentName())) {
                throw invalid("ready result recommended department is incomplete");
            }
            try {
                TriageCatalogValidator.validate(catalog, department.departmentId(), department.departmentName());
            } catch (RuntimeException exception) {
                throw invalid("recommended department is not in published triage catalog", exception);
            }
        }
    }

    private void validateBlocked(AiTriageResult triageResult) {
        if (!"high".equals(triageResult.riskLevel())) {
            throw invalid("blocked result riskLevel must be high");
        }
        if (isBlank(triageResult.blockedReason())) {
            throw invalid("blocked result blockedReason is required");
        }
        if (!defaultList(triageResult.recommendedDepartments()).isEmpty()) {
            throw invalid("blocked result recommendedDepartments must be empty");
        }
    }

    private CatalogVersion parseCatalogVersion(String value) {
        try {
            return new CatalogVersion(value);
        } catch (IllegalArgumentException exception) {
            throw invalid("triage catalog version format is invalid: " + value, exception);
        }
    }

    private AiTriageResultSnapshot toSnapshot(String hospitalScope, AiTriageQueryResponse response) {
        AiTriageResult triageResult = response.triageResult();
        return new AiTriageResultSnapshot(
                response.requestId(),
                response.sessionId(),
                response.turnId(),
                response.queryRunId(),
                hospitalScope,
                triageResult.triageStage(),
                triageResult.triageCompletionReason(),
                triageResult.nextAction(),
                triageResult.riskLevel(),
                triageResult.chiefComplaintSummary(),
                triageResult.careAdvice(),
                triageResult.blockedReason(),
                triageResult.catalogVersion(),
                defaultList(triageResult.recommendedDepartments()),
                defaultList(triageResult.citations()));
    }

    private <T> List<T> defaultList(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private SysException invalid(String message) {
        return new SysException(ErrorCode.AI_RESPONSE_INVALID, message);
    }

    private SysException invalid(String message, Throwable cause) {
        return new SysException(ErrorCode.AI_RESPONSE_INVALID, message, cause);
    }
}
