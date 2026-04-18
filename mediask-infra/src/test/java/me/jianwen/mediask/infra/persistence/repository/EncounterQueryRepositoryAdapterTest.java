package me.jianwen.mediask.infra.persistence.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import me.jianwen.mediask.domain.clinical.model.EncounterAiSummary;
import me.jianwen.mediask.domain.clinical.model.EncounterListItem;
import me.jianwen.mediask.domain.clinical.model.VisitEncounterStatus;
import me.jianwen.mediask.infra.persistence.mapper.AiRunCitationRow;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import me.jianwen.mediask.infra.persistence.mapper.VisitEncounterListRow;
import me.jianwen.mediask.infra.persistence.mapper.VisitEncounterAiSummaryRow;
import me.jianwen.mediask.infra.persistence.mapper.VisitEncounterMapper;
import org.junit.jupiter.api.Test;

class EncounterQueryRepositoryAdapterTest {

    @Test
    void listByDoctorId_WhenStatusProvided_MapRowsAndForwardFilter() {
        MapperHandler handler = new MapperHandler();
        EncounterQueryRepositoryAdapter adapter = new EncounterQueryRepositoryAdapter(
                proxy(VisitEncounterMapper.class, Map.of(
                        "selectDoctorEncounters", handler::selectDoctorEncounters,
                        "selectEncounterAiSummary", handler::selectEncounterAiSummary,
                        "selectRunCitations", handler::selectRunCitations)),
                beanFactory().getBeanProvider(ObjectMapper.class));

        List<EncounterListItem> result = adapter.listByDoctorId(2101L, VisitEncounterStatus.SCHEDULED);

        assertEquals(2101L, handler.lastDoctorId);
        assertEquals("SCHEDULED", handler.lastStatus);
        assertEquals(1, result.size());
        assertEquals(8101L, result.getFirst().encounterId());
        assertEquals(6101L, result.getFirst().registrationId());
        assertEquals("李患者", result.getFirst().patientName());
        assertEquals(VisitEncounterStatus.SCHEDULED, result.getFirst().encounterStatus());
    }

    @Test
    void findAiSummaryByEncounterId_WhenSummaryExists_MapFieldsAndSortCitations() {
        MapperHandler handler = new MapperHandler();
        handler.aiSummaryRow = aiSummaryRow();
        handler.citationRows = List.of(citationRow(7001L, 1), citationRow(7002L, 2));
        EncounterQueryRepositoryAdapter adapter = new EncounterQueryRepositoryAdapter(
                proxy(VisitEncounterMapper.class, Map.of(
                        "selectDoctorEncounters", handler::selectDoctorEncounters,
                        "selectEncounterAiSummary", handler::selectEncounterAiSummary,
                        "selectRunCitations", handler::selectRunCitations)),
                beanFactory().getBeanProvider(ObjectMapper.class));

        EncounterAiSummary result = adapter.findAiSummaryByEncounterId(8101L).orElseThrow();

        assertEquals(8101L, handler.lastAiSummaryEncounterId);
        assertEquals(9201L, handler.lastModelRunId);
        assertEquals(9001L, result.sessionId());
        assertEquals("患者自述头痛三天伴低热", result.structuredSummary());
        assertEquals("头痛三天", result.chiefComplaintSummary());
        assertEquals("神经内科", result.recommendedDepartments().getFirst().departmentName());
        assertEquals(2, result.latestCitations().size());
        assertEquals(7001L, result.latestCitations().getFirst().chunkId());
    }

    @Test
    void findAiSummaryByEncounterId_WhenSummaryMissing_ReturnEmpty() {
        MapperHandler handler = new MapperHandler();
        EncounterQueryRepositoryAdapter adapter = new EncounterQueryRepositoryAdapter(
                proxy(VisitEncounterMapper.class, Map.of(
                        "selectDoctorEncounters", handler::selectDoctorEncounters,
                        "selectEncounterAiSummary", handler::selectEncounterAiSummary,
                        "selectRunCitations", handler::selectRunCitations)),
                beanFactory().getBeanProvider(ObjectMapper.class));

        assertTrue(adapter.findAiSummaryByEncounterId(8101L).isEmpty());
    }

    private StaticListableBeanFactory beanFactory() {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("objectMapper", new ObjectMapper().findAndRegisterModules());
        return beanFactory;
    }

    private static <T> T proxy(Class<T> type, Map<String, Function<Object[], Object>> handlers) {
        Object proxyInstance = Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> type.getSimpleName() + "TestProxy";
                    default -> throw new UnsupportedOperationException(method.getName());
                };
            }
            Function<Object[], Object> handler = handlers.get(method.getName());
            if (handler == null) {
                throw new AssertionError("No test handler registered for " + type.getSimpleName() + "#" + method.getName());
            }
            return handler.apply(args == null ? new Object[0] : args);
        });
        return type.cast(proxyInstance);
    }

    private static final class MapperHandler {

        private Long lastDoctorId;
        private String lastStatus;
        private Long lastAiSummaryEncounterId;
        private Long lastModelRunId;
        private VisitEncounterAiSummaryRow aiSummaryRow;
        private List<AiRunCitationRow> citationRows = List.of();

        private Object selectDoctorEncounters(Object[] arguments) {
            this.lastDoctorId = (Long) arguments[0];
            this.lastStatus = (String) arguments[1];
            VisitEncounterListRow row = new VisitEncounterListRow();
            row.setEncounterId(8101L);
            row.setRegistrationId(6101L);
            row.setPatientUserId(2003L);
            row.setPatientName("李患者");
            row.setDepartmentId(3101L);
            row.setDepartmentName("心内科");
            row.setSessionDate(LocalDate.parse("2026-04-03"));
            row.setPeriodCode("MORNING");
            row.setEncounterStatus("SCHEDULED");
            row.setStartedAt(OffsetDateTime.parse("2026-04-03T09:00:00+08:00"));
            return List.of(row);
        }

        private Object selectEncounterAiSummary(Object[] arguments) {
            this.lastAiSummaryEncounterId = (Long) arguments[0];
            return aiSummaryRow;
        }

        private Object selectRunCitations(Object[] arguments) {
            this.lastModelRunId = (Long) arguments[0];
            return citationRows;
        }
    }

    private VisitEncounterAiSummaryRow aiSummaryRow() {
        VisitEncounterAiSummaryRow row = new VisitEncounterAiSummaryRow();
        row.setEncounterId(8101L);
        row.setSessionId(9001L);
        row.setStructuredSummary("患者自述头痛三天伴低热");
        row.setModelRunId(9201L);
        row.setRiskLevel("medium");
        row.setTriageSnapshotJson(
                """
                {"triageStage":"READY","chiefComplaintSummary":"头痛三天","recommendedDepartments":[{"departmentId":101,"departmentName":"神经内科","priority":1,"reason":"持续头痛"}]}
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
}
