package me.jianwen.mediask.api.controller;

import me.jianwen.mediask.api.assembler.KnowledgeAdminAssembler;
import me.jianwen.mediask.api.dto.CreateKnowledgeBaseRequest;
import me.jianwen.mediask.api.dto.PublishKnowledgeReleaseRequest;
import me.jianwen.mediask.api.dto.UpdateKnowledgeBaseRequest;
import me.jianwen.mediask.api.security.AuthenticatedUserPrincipal;
import me.jianwen.mediask.application.authz.AuthorizeScenario;
import me.jianwen.mediask.application.authz.ScenarioCode;
import me.jianwen.mediask.application.knowledge.usecase.KnowledgeAdminGatewayUseCase;
import me.jianwen.mediask.common.result.Result;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin")
public class KnowledgeAdminController {

    private final KnowledgeAdminGatewayUseCase useCase;

    public KnowledgeAdminController(KnowledgeAdminGatewayUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/knowledge-bases")
    @AuthorizeScenario(ScenarioCode.ADMIN_KNOWLEDGE_BASE_LIST)
    public Result<Object> listKnowledgeBases(
            @RequestParam(required = false) String keyword,
            @RequestParam(name = "pageNum", required = false) Integer pageNum,
            @RequestParam(name = "pageSize", required = false) Integer pageSize,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return Result.ok(KnowledgeAdminAssembler.toFrontendPayload(useCase.listKnowledgeBases(
                KnowledgeAdminAssembler.toContext(principal),
                KnowledgeAdminAssembler.toKnowledgeBaseListQuery(keyword, pageNum, pageSize))));
    }

    @PostMapping("/knowledge-bases")
    @AuthorizeScenario(ScenarioCode.ADMIN_KNOWLEDGE_BASE_CREATE)
    public Result<Object> createKnowledgeBase(
            @RequestBody CreateKnowledgeBaseRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return Result.ok(KnowledgeAdminAssembler.toFrontendPayload(useCase.createKnowledgeBase(
                KnowledgeAdminAssembler.toContext(principal),
                KnowledgeAdminAssembler.toCreateKnowledgeBasePayload(request))));
    }

    @PatchMapping("/knowledge-bases/{knowledgeBaseId}")
    @AuthorizeScenario(ScenarioCode.ADMIN_KNOWLEDGE_BASE_UPDATE)
    public Result<Object> updateKnowledgeBase(
            @PathVariable String knowledgeBaseId,
            @RequestBody UpdateKnowledgeBaseRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return Result.ok(KnowledgeAdminAssembler.toFrontendPayload(useCase.updateKnowledgeBase(
                KnowledgeAdminAssembler.toContext(principal),
                knowledgeBaseId,
                KnowledgeAdminAssembler.toUpdateKnowledgeBasePayload(request))));
    }

    @DeleteMapping("/knowledge-bases/{knowledgeBaseId}")
    @AuthorizeScenario(ScenarioCode.ADMIN_KNOWLEDGE_BASE_DELETE)
    public Result<Void> deleteKnowledgeBase(
            @PathVariable String knowledgeBaseId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        useCase.deleteKnowledgeBase(KnowledgeAdminAssembler.toContext(principal), knowledgeBaseId);
        return Result.ok();
    }

    @PostMapping(value = "/knowledge-documents/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @AuthorizeScenario(ScenarioCode.ADMIN_KNOWLEDGE_DOCUMENT_IMPORT)
    public Result<Object> importKnowledgeDocument(
            @RequestParam("knowledgeBaseId") String knowledgeBaseId,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return Result.ok(KnowledgeAdminAssembler.toFrontendPayload(useCase.importKnowledgeDocument(
                KnowledgeAdminAssembler.toContext(principal),
                knowledgeBaseId,
                KnowledgeAdminAssembler.toKnowledgeAdminFile(file))));
    }

    @GetMapping("/knowledge-documents")
    @AuthorizeScenario(ScenarioCode.ADMIN_KNOWLEDGE_DOCUMENT_LIST)
    public Result<Object> listKnowledgeDocuments(
            @RequestParam("knowledgeBaseId") String knowledgeBaseId,
            @RequestParam(name = "pageNum", required = false) Integer pageNum,
            @RequestParam(name = "pageSize", required = false) Integer pageSize,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return Result.ok(KnowledgeAdminAssembler.toFrontendPayload(useCase.listKnowledgeDocuments(
                KnowledgeAdminAssembler.toContext(principal),
                KnowledgeAdminAssembler.toKnowledgeDocumentListQuery(knowledgeBaseId, pageNum, pageSize))));
    }

    @DeleteMapping("/knowledge-documents/{documentId}")
    @AuthorizeScenario(ScenarioCode.ADMIN_KNOWLEDGE_DOCUMENT_DELETE)
    public Result<Void> deleteKnowledgeDocument(
            @PathVariable String documentId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        useCase.deleteKnowledgeDocument(KnowledgeAdminAssembler.toContext(principal), documentId);
        return Result.ok();
    }

    @GetMapping("/ingest-jobs/{jobId}")
    @AuthorizeScenario(ScenarioCode.ADMIN_KNOWLEDGE_INGEST_JOB_VIEW)
    public Result<Object> getIngestJob(
            @PathVariable String jobId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return Result.ok(KnowledgeAdminAssembler.toFrontendPayload(useCase.getIngestJob(
                KnowledgeAdminAssembler.toContext(principal),
                jobId)));
    }

    @GetMapping("/knowledge-index-versions")
    @AuthorizeScenario(ScenarioCode.ADMIN_KNOWLEDGE_INDEX_VERSION_LIST)
    public Result<Object> listKnowledgeIndexVersions(
            @RequestParam("knowledgeBaseId") String knowledgeBaseId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return Result.ok(KnowledgeAdminAssembler.toFrontendPayload(useCase.listKnowledgeIndexVersions(
                KnowledgeAdminAssembler.toContext(principal),
                knowledgeBaseId)));
    }

    @GetMapping("/knowledge-releases")
    @AuthorizeScenario(ScenarioCode.ADMIN_KNOWLEDGE_RELEASE_LIST)
    public Result<Object> listKnowledgeReleases(
            @RequestParam("knowledgeBaseId") String knowledgeBaseId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return Result.ok(KnowledgeAdminAssembler.toFrontendPayload(useCase.listKnowledgeReleases(
                KnowledgeAdminAssembler.toContext(principal),
                knowledgeBaseId)));
    }

    @PostMapping("/knowledge-releases")
    @AuthorizeScenario(ScenarioCode.ADMIN_KNOWLEDGE_RELEASE_PUBLISH)
    public Result<Object> publishKnowledgeRelease(
            @RequestBody PublishKnowledgeReleaseRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return Result.ok(KnowledgeAdminAssembler.toFrontendPayload(useCase.publishKnowledgeRelease(
                KnowledgeAdminAssembler.toContext(principal),
                KnowledgeAdminAssembler.toPublishKnowledgeReleasePayload(request))));
    }
}
