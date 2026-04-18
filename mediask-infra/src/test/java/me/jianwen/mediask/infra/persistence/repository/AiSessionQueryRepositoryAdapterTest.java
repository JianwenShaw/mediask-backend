package me.jianwen.mediask.infra.persistence.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Proxy;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import me.jianwen.mediask.infra.persistence.dataobject.AiSessionDO;
import me.jianwen.mediask.domain.ai.model.AiSessionDetail;
import me.jianwen.mediask.domain.ai.model.AiSessionListItem;
import me.jianwen.mediask.domain.ai.model.AiSessionTriageResultView;
import me.jianwen.mediask.domain.ai.model.AiTriageResultStatus;
import me.jianwen.mediask.domain.ai.model.AiTriageStage;
import me.jianwen.mediask.domain.ai.model.GuardrailAction;
import me.jianwen.mediask.domain.ai.model.RiskLevel;
import me.jianwen.mediask.infra.persistence.mapper.AiRunCitationRow;
import me.jianwen.mediask.infra.persistence.mapper.AiSessionDetailRow;
import me.jianwen.mediask.infra.persistence.mapper.AiSessionMapper;
import me.jianwen.mediask.infra.persistence.mapper.AiSessionMessageRow;
import me.jianwen.mediask.infra.persistence.mapper.AiSessionQueryMapper;
import me.jianwen.mediask.infra.persistence.mapper.AiSessionTriageResultRow;
import me.jianwen.mediask.infra.persistence.mapper.AiSessionTurnRow;

class AiSessionQueryRepositoryAdapterTest {

    @Test
    void listSessionsByPatientUserId_ShouldMapRowsInOrder() {
        AiSessionMapper aiSessionMapper = aiSessionMapper(List.of(
                sessionDo(9002L, "2026-04-13T09:30:00+08:00"), sessionDo(9001L, "2026-04-12T09:30:00+08:00")));
        StubAiSessionQueryMapper mapper = new StubAiSessionQueryMapper();
        AiSessionQueryRepositoryAdapter adapter =
                new AiSessionQueryRepositoryAdapter(aiSessionMapper, mapper, beanFactory().getBeanProvider(ObjectMapper.class));

        List<AiSessionListItem> sessions = adapter.listSessionsByPatientUserId(1001L);

        assertEquals(2, sessions.size());
        assertEquals(9002L, sessions.getFirst().sessionId());
        assertEquals("复诊头痛", sessions.getFirst().chiefComplaintSummary());
        assertEquals(9001L, sessions.get(1).sessionId());
    }

    @Test
    void findSessionDetailById_ShouldAssembleTurnsAndMessagesInOrder() {
        AiSessionMapper aiSessionMapper = aiSessionMapper(List.of());
        StubAiSessionQueryMapper mapper = new StubAiSessionQueryMapper();
        mapper.sessionDetailRow = sessionDetailRow();
        mapper.turnRows = List.of(turnRow(9101L, 1), turnRow(9102L, 2));
        mapper.messageRows = List.of(
                messageRow(9101L, "USER", "enc<头痛三天>", "2026-04-12T09:30:00+08:00"),
                messageRow(9101L, "ASSISTANT", "enc<建议挂神经内科>", "2026-04-12T09:31:00+08:00"),
                messageRow(9102L, "USER", "enc<仍然头痛>", "2026-04-12T09:32:00+08:00"));
        AiSessionQueryRepositoryAdapter adapter =
                new AiSessionQueryRepositoryAdapter(aiSessionMapper, mapper, beanFactory().getBeanProvider(ObjectMapper.class));

        AiSessionDetail detail = adapter.findSessionDetailById(9001L).orElseThrow();

        assertEquals(2, detail.turns().size());
        assertEquals(2, detail.turns().getFirst().messages().size());
        assertEquals("enc<头痛三天>", detail.turns().getFirst().messages().getFirst().encryptedContent());
        assertEquals("enc<仍然头痛>", detail.turns().get(1).messages().getFirst().encryptedContent());
    }

    @Test
    void findLatestTriageResultBySessionId_ShouldParseDetailJsonAndSortCitations() {
        AiSessionMapper aiSessionMapper = aiSessionMapper(List.of());
        StubAiSessionQueryMapper mapper = new StubAiSessionQueryMapper();
        mapper.triageResultRow = triageResultRow();
        mapper.citationRows = List.of(citationRow(7001L, 1), citationRow(7002L, 2));
        AiSessionQueryRepositoryAdapter adapter =
                new AiSessionQueryRepositoryAdapter(aiSessionMapper, mapper, beanFactory().getBeanProvider(ObjectMapper.class));

        AiSessionTriageResultView result = adapter.findLatestTriageResultBySessionId(9001L).orElseThrow();

        assertEquals(AiTriageResultStatus.UPDATING, result.resultStatus());
        assertEquals(AiTriageStage.READY, result.triageStage());
        assertEquals(RiskLevel.MEDIUM, result.riskLevel());
        assertEquals(GuardrailAction.CAUTION, result.guardrailAction());
        assertEquals("头痛三天", result.chiefComplaintSummary());
        assertEquals(1, result.recommendedDepartments().getFirst().priority());
        assertEquals(2, result.citations().size());
        assertEquals(1, result.citations().getFirst().retrievalRank());
        assertEquals(7001L, result.citations().getFirst().chunkId());
    }

    @Test
    void findSessionDetailById_WhenMapperReturnsNull_ShouldReturnEmpty() {
        AiSessionMapper aiSessionMapper = aiSessionMapper(List.of());
        AiSessionQueryRepositoryAdapter adapter =
                new AiSessionQueryRepositoryAdapter(aiSessionMapper, new StubAiSessionQueryMapper(), beanFactory().getBeanProvider(ObjectMapper.class));

        assertTrue(adapter.findSessionDetailById(9001L).isEmpty());
    }

    private StaticListableBeanFactory beanFactory() {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("objectMapper", new ObjectMapper().findAndRegisterModules());
        return beanFactory;
    }

    private AiSessionMapper aiSessionMapper(List<AiSessionDO> sessions) {
        return (AiSessionMapper) Proxy.newProxyInstance(
                AiSessionMapper.class.getClassLoader(),
                new Class<?>[] {AiSessionMapper.class},
                (proxy, method, args) -> {
                    if ("selectList".equals(method.getName())) {
                        return sessions;
                    }
                    if (method.getReturnType().equals(boolean.class)) {
                        return false;
                    }
                    if (method.getReturnType().equals(int.class)) {
                        return 0;
                    }
                    if (method.getReturnType().equals(long.class)) {
                        return 0L;
                    }
                    return null;
                });
    }

    private AiSessionDetailRow sessionDetailRow() {
        AiSessionDetailRow row = new AiSessionDetailRow();
        row.setSessionId(9001L);
        row.setPatientId(1001L);
        row.setDepartmentId(2001L);
        row.setSceneType("PRE_CONSULTATION");
        row.setSessionStatus("ACTIVE");
        row.setChiefComplaintSummary("头痛三天");
        row.setSummary("头痛三天伴低烧");
        row.setStartedAt(OffsetDateTime.parse("2026-04-12T09:30:00+08:00"));
        return row;
    }

    private AiSessionDO sessionDo(Long sessionId, String startedAt) {
        AiSessionDO row = new AiSessionDO();
        row.setId(sessionId);
        row.setDepartmentId(2001L);
        row.setSceneType("PRE_CONSULTATION");
        row.setSessionStatus(sessionId.equals(9002L) ? "CLOSED" : "ACTIVE");
        row.setChiefComplaintSummary(sessionId.equals(9002L) ? "复诊头痛" : "头痛三天");
        row.setSummary(sessionId.equals(9002L) ? "复诊头痛已缓解" : "头痛三天伴低烧");
        row.setStartedAt(OffsetDateTime.parse(startedAt));
        row.setEndedAt(sessionId.equals(9002L) ? OffsetDateTime.parse("2026-04-13T09:35:00+08:00") : null);
        return row;
    }

    private AiSessionTurnRow turnRow(Long turnId, Integer turnNo) {
        AiSessionTurnRow row = new AiSessionTurnRow();
        row.setTurnId(turnId);
        row.setTurnNo(turnNo);
        row.setTurnStatus("COMPLETED");
        row.setStartedAt(OffsetDateTime.parse("2026-04-12T09:30:00+08:00"));
        row.setCompletedAt(OffsetDateTime.parse("2026-04-12T09:31:00+08:00"));
        return row;
    }

    private AiSessionMessageRow messageRow(Long turnId, String role, String encryptedContent, String createdAt) {
        AiSessionMessageRow row = new AiSessionMessageRow();
        row.setTurnId(turnId);
        row.setContentRole(role);
        row.setContentEncrypted(encryptedContent);
        row.setCreatedAt(OffsetDateTime.parse(createdAt));
        return row;
    }

    private AiSessionTriageResultRow triageResultRow() {
        AiSessionTriageResultRow row = new AiSessionTriageResultRow();
        row.setSessionId(9001L);
        row.setPatientId(1001L);
        row.setModelRunId(9201L);
        row.setFinalizedTurnId(9101L);
        row.setFinalizedTurnNo(1);
        row.setFinalizedAt(OffsetDateTime.parse("2026-04-12T09:31:00+08:00"));
        row.setLatestTurnNo(3);
        row.setLatestTurnStatus("COMPLETED");
        row.setLatestRunStatus("SUCCEEDED");
        row.setLatestEventDetailJson(
                """
                {"triageStage":"COLLECTING","followUpQuestions":["是否伴随恶心？"]}
                """);
        row.setRiskLevel("medium");
        row.setGuardrailAction("caution");
        row.setTriageSnapshotJson(
                """
                {"triageStage":"READY","triageCompletionReason":"SUFFICIENT_INFO","chiefComplaintSummary":"头痛三天","recommendedDepartments":[{"departmentId":101,"departmentName":"神经内科","priority":1,"reason":"持续头痛"}],"careAdvice":"建议线下就诊"}
                """);
        return row;
    }

    private AiRunCitationRow citationRow(Long chunkId, Integer rank) {
        AiRunCitationRow row = new AiRunCitationRow();
        row.setChunkId(chunkId);
        row.setRetrievalRank(rank);
        row.setFusionScore(0.82D);
        row.setSnippet("引用片段-" + rank);
        return row;
    }

    private static final class StubAiSessionQueryMapper implements AiSessionQueryMapper {
        private AiSessionDetailRow sessionDetailRow;
        private List<AiSessionTurnRow> turnRows = List.of();
        private List<AiSessionMessageRow> messageRows = List.of();
        private AiSessionTriageResultRow triageResultRow;
        private List<AiRunCitationRow> citationRows = List.of();

        @Override
        public AiSessionDetailRow selectSessionDetail(Long sessionId) {
            return sessionDetailRow;
        }

        @Override
        public List<AiSessionTurnRow> selectSessionTurns(Long sessionId) {
            return turnRows;
        }

        @Override
        public List<AiSessionMessageRow> selectSessionMessages(Long sessionId) {
            return messageRows;
        }

        @Override
        public AiSessionTriageResultRow selectLatestTriageResult(Long sessionId) {
            return triageResultRow;
        }

        @Override
        public List<AiRunCitationRow> selectRunCitations(Long modelRunId) {
            return citationRows;
        }

        @Override
        public String selectLatestTriageEventDetail(Long sessionId) {
            return triageResultRow == null ? null : triageResultRow.getLatestEventDetailJson();
        }
    }
}
