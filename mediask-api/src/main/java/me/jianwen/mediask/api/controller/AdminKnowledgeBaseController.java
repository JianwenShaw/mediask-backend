package me.jianwen.mediask.api.controller;

import me.jianwen.mediask.api.assembler.AiAssembler;
import me.jianwen.mediask.api.dto.CreateKnowledgeBaseRequest;
import me.jianwen.mediask.api.dto.KnowledgeBaseResponse;
import me.jianwen.mediask.api.dto.UpdateKnowledgeBaseRequest;
import me.jianwen.mediask.application.ai.command.CreateKnowledgeBaseCommand;
import me.jianwen.mediask.application.ai.command.DeleteKnowledgeBaseCommand;
import me.jianwen.mediask.application.ai.command.UpdateKnowledgeBaseCommand;
import me.jianwen.mediask.application.ai.query.ListKnowledgeBasesQuery;
import me.jianwen.mediask.application.ai.usecase.CreateKnowledgeBaseUseCase;
import me.jianwen.mediask.application.ai.usecase.DeleteKnowledgeBaseUseCase;
import me.jianwen.mediask.application.ai.usecase.ListKnowledgeBasesUseCase;
import me.jianwen.mediask.application.ai.usecase.UpdateKnowledgeBaseUseCase;
import me.jianwen.mediask.application.authz.AuthorizeScenario;
import me.jianwen.mediask.application.authz.ScenarioCode;
import me.jianwen.mediask.common.pagination.PageData;
import me.jianwen.mediask.common.result.Result;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/knowledge-bases")
public class AdminKnowledgeBaseController {

    private final ListKnowledgeBasesUseCase listKnowledgeBasesUseCase;
    private final CreateKnowledgeBaseUseCase createKnowledgeBaseUseCase;
    private final UpdateKnowledgeBaseUseCase updateKnowledgeBaseUseCase;
    private final DeleteKnowledgeBaseUseCase deleteKnowledgeBaseUseCase;

    public AdminKnowledgeBaseController(
            ListKnowledgeBasesUseCase listKnowledgeBasesUseCase,
            CreateKnowledgeBaseUseCase createKnowledgeBaseUseCase,
            UpdateKnowledgeBaseUseCase updateKnowledgeBaseUseCase,
            DeleteKnowledgeBaseUseCase deleteKnowledgeBaseUseCase) {
        this.listKnowledgeBasesUseCase = listKnowledgeBasesUseCase;
        this.createKnowledgeBaseUseCase = createKnowledgeBaseUseCase;
        this.updateKnowledgeBaseUseCase = updateKnowledgeBaseUseCase;
        this.deleteKnowledgeBaseUseCase = deleteKnowledgeBaseUseCase;
    }

    @GetMapping
    @AuthorizeScenario(ScenarioCode.ADMIN_KNOWLEDGE_BASE_LIST)
    public Result<PageData<KnowledgeBaseResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long pageNum,
            @RequestParam(required = false) Long pageSize) {
        return Result.ok(listKnowledgeBasesUseCase
                .handle(ListKnowledgeBasesQuery.page(keyword, pageNum, pageSize))
                .map(AiAssembler::toKnowledgeBaseResponse));
    }

    @PostMapping
    @AuthorizeScenario(ScenarioCode.ADMIN_KNOWLEDGE_BASE_CREATE)
    public Result<KnowledgeBaseResponse> create(@RequestBody CreateKnowledgeBaseRequest request) {
        return Result.ok(AiAssembler.toKnowledgeBaseResponse(createKnowledgeBaseUseCase.handle(
                new CreateKnowledgeBaseCommand(
                        request.name(),
                        request.kbCode(),
                        request.ownerType(),
                        request.ownerDeptId(),
                        request.visibility()))));
    }

    @PatchMapping("/{knowledgeBaseId}")
    @AuthorizeScenario(ScenarioCode.ADMIN_KNOWLEDGE_BASE_UPDATE)
    public Result<KnowledgeBaseResponse> update(
            @PathVariable Long knowledgeBaseId, @RequestBody UpdateKnowledgeBaseRequest request) {
        return Result.ok(AiAssembler.toKnowledgeBaseResponse(updateKnowledgeBaseUseCase.handle(
                new UpdateKnowledgeBaseCommand(
                        knowledgeBaseId,
                        request.name(),
                        request.ownerType(),
                        request.ownerDeptId(),
                        request.visibility(),
                        request.status()))));
    }

    @DeleteMapping("/{knowledgeBaseId}")
    @AuthorizeScenario(ScenarioCode.ADMIN_KNOWLEDGE_BASE_DELETE)
    public Result<Void> delete(@PathVariable Long knowledgeBaseId) {
        deleteKnowledgeBaseUseCase.handle(new DeleteKnowledgeBaseCommand(knowledgeBaseId));
        return Result.ok();
    }
}
