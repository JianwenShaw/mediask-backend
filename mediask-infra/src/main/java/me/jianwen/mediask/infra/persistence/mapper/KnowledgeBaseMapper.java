package me.jianwen.mediask.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import me.jianwen.mediask.infra.persistence.dataobject.KnowledgeBaseDO;
import me.jianwen.mediask.infra.persistence.row.KnowledgeBaseListRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBaseDO> {

    @Select("""
            <script>
            SELECT
                kb.id,
                kb.kb_code,
                kb.name,
                kb.owner_type,
                kb.owner_dept_id,
                kb.visibility,
                kb.status,
                COUNT(doc.id) AS doc_count
            FROM knowledge_base kb
            LEFT JOIN knowledge_document doc
                ON doc.knowledge_base_id = kb.id
               AND doc.deleted_at IS NULL
            WHERE kb.deleted_at IS NULL
            <if test="keyword != null and keyword != ''">
              AND (kb.name ILIKE CONCAT('%', #{keyword}, '%')
                OR kb.kb_code ILIKE CONCAT('%', #{keyword}, '%'))
            </if>
            GROUP BY kb.id, kb.kb_code, kb.name, kb.owner_type, kb.owner_dept_id, kb.visibility, kb.status
            ORDER BY kb.created_at DESC, kb.id DESC
            </script>
            """)
    IPage<KnowledgeBaseListRow> selectKnowledgeBasePage(
            Page<KnowledgeBaseListRow> page, @Param("keyword") String keyword);
}
