package me.jianwen.mediask.infra.ai.adapter;

import java.util.List;
import me.jianwen.mediask.domain.ai.model.KnowledgeChunk;
import me.jianwen.mediask.domain.ai.model.KnowledgeDocument;
import me.jianwen.mediask.domain.ai.model.KnowledgePrepareInvocation;
import me.jianwen.mediask.domain.ai.model.PreparedKnowledgeChunk;
import me.jianwen.mediask.domain.ai.port.KnowledgeIndexPort;
import me.jianwen.mediask.domain.ai.port.KnowledgePreparePort;
import me.jianwen.mediask.infra.ai.client.PythonKnowledgeClient;
import me.jianwen.mediask.infra.ai.client.dto.PythonKnowledgeIndexRequest;
import me.jianwen.mediask.infra.ai.client.dto.PythonKnowledgePrepareRequest;

public final class PythonKnowledgePortAdapter implements KnowledgePreparePort, KnowledgeIndexPort {

    private final PythonKnowledgeClient pythonKnowledgeClient;

    public PythonKnowledgePortAdapter(PythonKnowledgeClient pythonKnowledgeClient) {
        this.pythonKnowledgeClient = pythonKnowledgeClient;
    }

    @Override
    public List<PreparedKnowledgeChunk> prepare(KnowledgePrepareInvocation invocation) {
        return pythonKnowledgeClient
                .prepare(new PythonKnowledgePrepareRequest(
                        invocation.documentId(),
                        invocation.documentUuid(),
                        invocation.knowledgeBaseId(),
                        invocation.title(),
                        invocation.sourceType().name(),
                        invocation.sourceUri(),
                        invocation.inlineContent()))
                .chunks()
                .stream()
                .map(chunk -> new PreparedKnowledgeChunk(
                        chunk.chunkIndex(),
                        chunk.content(),
                        chunk.sectionTitle(),
                        chunk.pageNo(),
                        chunk.charStart(),
                        chunk.charEnd(),
                        chunk.tokenCount(),
                        chunk.contentPreview(),
                        chunk.citationLabel()))
                .toList();
    }

    @Override
    public void index(KnowledgeDocument knowledgeDocument, List<KnowledgeChunk> knowledgeChunks) {
        pythonKnowledgeClient.index(new PythonKnowledgeIndexRequest(
                knowledgeDocument.id(),
                knowledgeDocument.knowledgeBaseId(),
                knowledgeChunks.stream()
                        .map(chunk ->
                                new PythonKnowledgeIndexRequest.PythonKnowledgeChunk(chunk.id(), chunk.chunkIndex(), chunk.content()))
                        .toList()));
    }
}
