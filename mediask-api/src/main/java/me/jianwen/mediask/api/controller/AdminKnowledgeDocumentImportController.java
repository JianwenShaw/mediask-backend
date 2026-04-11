package me.jianwen.mediask.api.controller;

import me.jianwen.mediask.api.assembler.AiAssembler;
import me.jianwen.mediask.api.dto.ImportKnowledgeDocumentResponse;
import me.jianwen.mediask.application.ai.command.ImportKnowledgeDocumentCommand;
import me.jianwen.mediask.application.ai.usecase.ImportKnowledgeDocumentUseCase;
import me.jianwen.mediask.application.authz.AuthorizeScenario;
import me.jianwen.mediask.application.authz.ScenarioCode;
import me.jianwen.mediask.common.result.Result;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/knowledge-documents")
@ConditionalOnProperty(prefix = "mediask.ai.service", name = {"base-url", "api-key"})
@ConditionalOnBean(ImportKnowledgeDocumentUseCase.class)
public class AdminKnowledgeDocumentImportController {

    private final ImportKnowledgeDocumentUseCase importKnowledgeDocumentUseCase;

    public AdminKnowledgeDocumentImportController(ImportKnowledgeDocumentUseCase importKnowledgeDocumentUseCase) {
        this.importKnowledgeDocumentUseCase = importKnowledgeDocumentUseCase;
    }

    @PostMapping(path = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @AuthorizeScenario(ScenarioCode.ADMIN_KNOWLEDGE_DOCUMENT_IMPORT)
    public Result<ImportKnowledgeDocumentResponse> importDocument(
            @RequestParam("knowledgeBaseId") Long knowledgeBaseId, @RequestParam("file") MultipartFile file)
            throws java.io.IOException {
        return Result.ok(AiAssembler.toImportKnowledgeDocumentResponse(importKnowledgeDocumentUseCase.handle(
                new ImportKnowledgeDocumentCommand(
                        knowledgeBaseId, file.getOriginalFilename(), file.getContentType(), file.getBytes()))));
    }
}
