package me.jianwen.mediask.infra.persistence.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AiSessionQueryMapper {

    AiSessionDetailRow selectSessionDetail(@Param("sessionId") Long sessionId);

    List<AiSessionTurnRow> selectSessionTurns(@Param("sessionId") Long sessionId);

    List<AiSessionMessageRow> selectSessionMessages(@Param("sessionId") Long sessionId);

    AiSessionTriageResultRow selectLatestTriageResult(@Param("sessionId") Long sessionId);

    List<AiRunCitationRow> selectRunCitations(@Param("modelRunId") Long modelRunId);
}
