package me.jianwen.mediask.application.knowledge.usecase;

import me.jianwen.mediask.domain.ai.model.CreateKnowledgeBasePayload;
import me.jianwen.mediask.domain.ai.model.KnowledgeAdminFile;
import me.jianwen.mediask.domain.ai.model.KnowledgeAdminGatewayContext;
import me.jianwen.mediask.domain.ai.model.KnowledgeBaseListQuery;
import me.jianwen.mediask.domain.ai.model.KnowledgeDocumentListQuery;
import me.jianwen.mediask.domain.ai.model.PublishKnowledgeReleasePayload;
import me.jianwen.mediask.domain.ai.model.UpdateKnowledgeBasePayload;
import me.jianwen.mediask.domain.ai.port.KnowledgeAdminGatewayPort;

public class KnowledgeAdminGatewayUseCase {

    private final KnowledgeAdminGatewayPort gatewayPort;

    public KnowledgeAdminGatewayUseCase(KnowledgeAdminGatewayPort gatewayPort) {
        this.gatewayPort = gatewayPort;
    }

    public Object listKnowledgeBases(KnowledgeAdminGatewayContext context, KnowledgeBaseListQuery query) {
        return gatewayPort.listKnowledgeBases(context, query);
    }

    public Object createKnowledgeBase(KnowledgeAdminGatewayContext context, CreateKnowledgeBasePayload payload) {
        return gatewayPort.createKnowledgeBase(context, payload);
    }

    public Object updateKnowledgeBase(
            KnowledgeAdminGatewayContext context,
            String knowledgeBaseId,
            UpdateKnowledgeBasePayload payload) {
        return gatewayPort.updateKnowledgeBase(context, knowledgeBaseId, payload);
    }

    public void deleteKnowledgeBase(KnowledgeAdminGatewayContext context, String knowledgeBaseId) {
        gatewayPort.deleteKnowledgeBase(context, knowledgeBaseId);
    }

    public Object importKnowledgeDocument(
            KnowledgeAdminGatewayContext context,
            String knowledgeBaseId,
            KnowledgeAdminFile file) {
        return gatewayPort.importKnowledgeDocument(context, knowledgeBaseId, file);
    }

    public Object listKnowledgeDocuments(KnowledgeAdminGatewayContext context, KnowledgeDocumentListQuery query) {
        return gatewayPort.listKnowledgeDocuments(context, query);
    }

    public void deleteKnowledgeDocument(KnowledgeAdminGatewayContext context, String documentId) {
        gatewayPort.deleteKnowledgeDocument(context, documentId);
    }

    public Object getIngestJob(KnowledgeAdminGatewayContext context, String jobId) {
        return gatewayPort.getIngestJob(context, jobId);
    }

    public Object listKnowledgeIndexVersions(KnowledgeAdminGatewayContext context, String knowledgeBaseId) {
        return gatewayPort.listKnowledgeIndexVersions(context, knowledgeBaseId);
    }

    public Object listKnowledgeReleases(KnowledgeAdminGatewayContext context, String knowledgeBaseId) {
        return gatewayPort.listKnowledgeReleases(context, knowledgeBaseId);
    }

    public Object publishKnowledgeRelease(
            KnowledgeAdminGatewayContext context,
            PublishKnowledgeReleasePayload payload) {
        return gatewayPort.publishKnowledgeRelease(context, payload);
    }
}
