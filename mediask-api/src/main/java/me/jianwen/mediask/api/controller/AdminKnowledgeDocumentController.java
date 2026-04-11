package me.jianwen.mediask.api.controller;

import me.jianwen.mediask.api.assembler.AiAssembler;
import me.jianwen.mediask.api.dto.KnowledgeDocumentListItemResponse;
import me.jianwen.mediask.application.ai.command.DeleteKnowledgeDocumentCommand;
import me.jianwen.mediask.application.ai.query.ListKnowledgeDocumentsQuery;
import me.jianwen.mediask.application.ai.usecase.DeleteKnowledgeDocumentUseCase;
import me.jianwen.mediask.application.ai.usecase.ListKnowledgeDocumentsUseCase;
import me.jianwen.mediask.application.authz.AuthorizeScenario;
import me.jianwen.mediask.application.authz.ScenarioCode;
import me.jianwen.mediask.common.pagination.PageData;
import me.jianwen.mediask.common.result.Result;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/knowledge-documents")
public class AdminKnowledgeDocumentController {

    private final ListKnowledgeDocumentsUseCase listKnowledgeDocumentsUseCase;
    private final DeleteKnowledgeDocumentUseCase deleteKnowledgeDocumentUseCase;

    public AdminKnowledgeDocumentController(
            ListKnowledgeDocumentsUseCase listKnowledgeDocumentsUseCase,
            DeleteKnowledgeDocumentUseCase deleteKnowledgeDocumentUseCase) {
        this.listKnowledgeDocumentsUseCase = listKnowledgeDocumentsUseCase;
        this.deleteKnowledgeDocumentUseCase = deleteKnowledgeDocumentUseCase;
    }

    @GetMapping
    @AuthorizeScenario(ScenarioCode.ADMIN_KNOWLEDGE_DOCUMENT_LIST)
    public Result<PageData<KnowledgeDocumentListItemResponse>> list(
            @RequestParam Long knowledgeBaseId,
            @RequestParam(required = false) Long pageNum,
            @RequestParam(required = false) Long pageSize) {
        return Result.ok(listKnowledgeDocumentsUseCase
                .handle(ListKnowledgeDocumentsQuery.page(knowledgeBaseId, pageNum, pageSize))
                .map(AiAssembler::toKnowledgeDocumentListItemResponse));
    }

    @DeleteMapping("/{documentId}")
    @AuthorizeScenario(ScenarioCode.ADMIN_KNOWLEDGE_DOCUMENT_DELETE)
    public Result<Void> delete(@PathVariable Long documentId) {
        deleteKnowledgeDocumentUseCase.handle(new DeleteKnowledgeDocumentCommand(documentId));
        return Result.ok();
    }
}
