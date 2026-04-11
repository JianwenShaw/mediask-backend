package me.jianwen.mediask.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import me.jianwen.mediask.infra.persistence.dataobject.KnowledgeDocumentDO;
import me.jianwen.mediask.infra.persistence.row.KnowledgeDocumentListRow;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface KnowledgeDocumentMapper extends BaseMapper<KnowledgeDocumentDO> {

    @Insert("""
            INSERT INTO knowledge_document (
                id,
                knowledge_base_id,
                document_uuid,
                title,
                source_type,
                source_uri,
                content_hash,
                language_code,
                version_no,
                document_status,
                ingested_by_service,
                version,
                created_at,
                updated_at
            ) VALUES (
                #{document.id},
                #{document.knowledgeBaseId},
                CAST(#{document.documentUuid} AS UUID),
                #{document.title},
                #{document.sourceType},
                #{document.sourceUri},
                #{document.contentHash},
                #{document.languageCode},
                #{document.versionNo},
                #{document.documentStatus},
                #{document.ingestedByService},
                #{document.version},
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP
            )
            """)
    int insertKnowledgeDocument(@Param("document") KnowledgeDocumentDO document);

    @Select("""
            SELECT
                doc.id,
                doc.document_uuid,
                doc.title,
                doc.source_type,
                doc.document_status,
                COUNT(chunk.id) AS chunk_count
            FROM knowledge_document doc
            LEFT JOIN knowledge_chunk chunk
                ON chunk.document_id = doc.id
               AND chunk.deleted_at IS NULL
            WHERE doc.deleted_at IS NULL
              AND doc.knowledge_base_id = #{knowledgeBaseId}
            GROUP BY doc.id, doc.document_uuid, doc.title, doc.source_type, doc.document_status
            ORDER BY doc.created_at DESC, doc.id DESC
            """)
    IPage<KnowledgeDocumentListRow> selectKnowledgeDocumentPage(
            Page<KnowledgeDocumentListRow> page, @Param("knowledgeBaseId") Long knowledgeBaseId);
}
