package me.jianwen.mediask.api.controller;

import me.jianwen.mediask.api.assembler.AiAssembler;
import me.jianwen.mediask.api.dto.ImportKnowledgeDocumentRequest;
import me.jianwen.mediask.api.dto.ImportKnowledgeDocumentResponse;
import me.jianwen.mediask.application.ai.command.ImportKnowledgeDocumentCommand;
import me.jianwen.mediask.application.ai.usecase.ImportKnowledgeDocumentUseCase;
import me.jianwen.mediask.application.authz.AuthorizeScenario;
import me.jianwen.mediask.application.authz.ScenarioCode;
import me.jianwen.mediask.common.result.Result;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/knowledge-documents")
@ConditionalOnProperty(prefix = "mediask.ai.service", name = {"base-url", "api-key"})
public class AdminKnowledgeDocumentController {

    private final ImportKnowledgeDocumentUseCase importKnowledgeDocumentUseCase;

    public AdminKnowledgeDocumentController(ImportKnowledgeDocumentUseCase importKnowledgeDocumentUseCase) {
        this.importKnowledgeDocumentUseCase = importKnowledgeDocumentUseCase;
    }

    @PostMapping("/import")
    @AuthorizeScenario(ScenarioCode.ADMIN_KNOWLEDGE_DOCUMENT_IMPORT)
    public Result<ImportKnowledgeDocumentResponse> importDocument(@RequestBody ImportKnowledgeDocumentRequest request) {
        return Result.ok(AiAssembler.toImportKnowledgeDocumentResponse(importKnowledgeDocumentUseCase.handle(
                new ImportKnowledgeDocumentCommand(
                        request.knowledgeBaseId(),
                        request.title(),
                        request.sourceType(),
                        request.sourceUri(),
                        request.inlineContent()))));
    }
}
