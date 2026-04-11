package me.jianwen.mediask.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import me.jianwen.mediask.infra.persistence.dataobject.KnowledgeDocumentDO;
import me.jianwen.mediask.infra.persistence.row.KnowledgeDocumentListRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface KnowledgeDocumentMapper extends BaseMapper<KnowledgeDocumentDO> {

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
