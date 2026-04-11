package me.jianwen.mediask.infra.ai.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record PythonKnowledgePrepareResponse(List<PythonPreparedChunk> chunks) {

    public record PythonPreparedChunk(
            @JsonProperty("chunk_index") Integer chunkIndex,
            String content,
            @JsonProperty("section_title") String sectionTitle,
            @JsonProperty("page_no") Integer pageNo,
            @JsonProperty("char_start") Integer charStart,
            @JsonProperty("char_end") Integer charEnd,
            @JsonProperty("token_count") Integer tokenCount,
            @JsonProperty("content_preview") String contentPreview,
            @JsonProperty("citation_label") String citationLabel) {}
}
