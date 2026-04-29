package me.jianwen.mediask.domain.ai.port;

import me.jianwen.mediask.domain.ai.model.CreateKnowledgeBasePayload;
import me.jianwen.mediask.domain.ai.model.KnowledgeAdminFile;
import me.jianwen.mediask.domain.ai.model.KnowledgeAdminGatewayContext;
import me.jianwen.mediask.domain.ai.model.KnowledgeBaseListQuery;
import me.jianwen.mediask.domain.ai.model.KnowledgeDocumentListQuery;
import me.jianwen.mediask.domain.ai.model.PublishKnowledgeReleasePayload;
import me.jianwen.mediask.domain.ai.model.UpdateKnowledgeBasePayload;

public interface KnowledgeAdminGatewayPort {

    Object listKnowledgeBases(KnowledgeAdminGatewayContext context, KnowledgeBaseListQuery query);

    Object createKnowledgeBase(KnowledgeAdminGatewayContext context, CreateKnowledgeBasePayload payload);

    Object updateKnowledgeBase(
            KnowledgeAdminGatewayContext context,
            String knowledgeBaseId,
            UpdateKnowledgeBasePayload payload);

    void deleteKnowledgeBase(KnowledgeAdminGatewayContext context, String knowledgeBaseId);

    Object importKnowledgeDocument(
            KnowledgeAdminGatewayContext context,
            String knowledgeBaseId,
            KnowledgeAdminFile file);

    Object listKnowledgeDocuments(KnowledgeAdminGatewayContext context, KnowledgeDocumentListQuery query);

    void deleteKnowledgeDocument(KnowledgeAdminGatewayContext context, String documentId);

    Object getIngestJob(KnowledgeAdminGatewayContext context, String jobId);

    Object listKnowledgeIndexVersions(KnowledgeAdminGatewayContext context, String knowledgeBaseId);

    Object listKnowledgeReleases(KnowledgeAdminGatewayContext context, String knowledgeBaseId);

    Object publishKnowledgeRelease(KnowledgeAdminGatewayContext context, PublishKnowledgeReleasePayload payload);
}
