package me.jianwen.mediask.application.ai.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import me.jianwen.mediask.application.ai.query.GetAiSessionDetailQuery;
import me.jianwen.mediask.application.ai.query.GetAiSessionTriageResultQuery;
import me.jianwen.mediask.application.ai.query.ListAiSessionsQuery;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.ai.exception.AiErrorCode;
import me.jianwen.mediask.domain.ai.model.AiCitation;
import me.jianwen.mediask.domain.ai.model.AiContentRole;
import me.jianwen.mediask.domain.ai.model.AiSceneType;
import me.jianwen.mediask.domain.ai.model.AiSessionDetail;
import me.jianwen.mediask.domain.ai.model.AiSessionListItem;
import me.jianwen.mediask.domain.ai.model.AiSessionMessage;
import me.jianwen.mediask.domain.ai.model.AiSessionStatus;
import me.jianwen.mediask.domain.ai.model.AiSessionTriageResultView;
import me.jianwen.mediask.domain.ai.model.AiSessionTurnDetail;
import me.jianwen.mediask.domain.ai.model.AiTurnStatus;
import me.jianwen.mediask.domain.ai.model.GuardrailAction;
import me.jianwen.mediask.domain.ai.model.RecommendedDepartment;
import me.jianwen.mediask.domain.ai.model.RiskLevel;
import me.jianwen.mediask.domain.ai.port.AiContentEncryptorPort;
import me.jianwen.mediask.domain.ai.port.AiSessionQueryRepository;
import org.junit.jupiter.api.Test;

class GetAiSessionReadUseCaseTest {

    @Test
    void listAiSessions_ShouldReturnRepositoryResults() {
        ListAiSessionsUseCase useCase = new ListAiSessionsUseCase(new StubQueryRepository());

        List<AiSessionListItem> sessions = useCase.handle(new ListAiSessionsQuery(1001L));

        assertEquals(2, sessions.size());
        assertEquals(9002L, sessions.getFirst().sessionId());
        assertEquals(AiSessionStatus.CLOSED, sessions.getFirst().status());
    }

    @Test
    void getSessionDetail_WhenOwnedByPatient_ShouldDecryptMessages() {
        GetAiSessionDetailUseCase useCase = new GetAiSessionDetailUseCase(new StubQueryRepository(), encryptor());

        AiSessionDetail detail = useCase.handle(new GetAiSessionDetailQuery(1001L, 9001L));

        assertEquals(1, detail.turns().size());
        assertEquals("头痛三天", detail.turns().getFirst().messages().getFirst().encryptedContent());
        assertEquals("建议挂神经内科", detail.turns().getFirst().messages().get(1).encryptedContent());
    }

    @Test
    void getSessionDetail_WhenSessionOwnedByAnotherPatient_ShouldReject() {
        GetAiSessionDetailUseCase useCase = new GetAiSessionDetailUseCase(new StubQueryRepository(), encryptor());

        BizException exception =
                assertThrows(BizException.class, () -> useCase.handle(new GetAiSessionDetailQuery(9999L, 9001L)));

        assertEquals(AiErrorCode.AI_SESSION_ACCESS_DENIED.getCode(), exception.getCode());
    }

    @Test
    void getSessionTriageResult_WhenMissing_ShouldReject() {
        GetAiSessionTriageResultUseCase useCase = new GetAiSessionTriageResultUseCase(new AiSessionQueryRepository() {
            @Override
            public List<AiSessionListItem> listSessionsByPatientUserId(Long patientUserId) {
                return List.of();
            }

            @Override
            public Optional<AiSessionDetail> findSessionDetailById(Long sessionId) {
                return Optional.empty();
            }

            @Override
            public Optional<AiSessionTriageResultView> findLatestTriageResultBySessionId(Long sessionId) {
                return Optional.empty();
            }
        });

        BizException exception = assertThrows(
                BizException.class, () -> useCase.handle(new GetAiSessionTriageResultQuery(1001L, 9001L)));

        assertEquals(AiErrorCode.AI_SESSION_TRIAGE_RESULT_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void getSessionTriageResult_WhenOwnedByPatient_ShouldReturnResult() {
        GetAiSessionTriageResultUseCase useCase = new GetAiSessionTriageResultUseCase(new StubQueryRepository());

        AiSessionTriageResultView result = useCase.handle(new GetAiSessionTriageResultQuery(1001L, 9001L));

        assertEquals(RiskLevel.MEDIUM, result.riskLevel());
        assertEquals(1, result.citations().size());
        assertEquals("持续头痛建议线下评估", result.citations().getFirst().snippet());
    }

    private AiContentEncryptorPort encryptor() {
        return new AiContentEncryptorPort() {
            @Override
            public String encrypt(String plainText) {
                return "enc<" + plainText + ">";
            }

            @Override
            public String decrypt(String encryptedText) {
                return encryptedText.substring(4, encryptedText.length() - 1);
            }
        };
    }

    private static final class StubQueryRepository implements AiSessionQueryRepository {
        @Override
        public List<AiSessionListItem> listSessionsByPatientUserId(Long patientUserId) {
            return List.of(
                    new AiSessionListItem(
                            9002L,
                            2001L,
                            AiSceneType.PRE_CONSULTATION,
                            AiSessionStatus.CLOSED,
                            "复诊头痛",
                            "复诊头痛已缓解",
                            OffsetDateTime.parse("2026-04-13T09:30:00+08:00"),
                            OffsetDateTime.parse("2026-04-13T09:35:00+08:00")),
                    new AiSessionListItem(
                            9001L,
                            2001L,
                            AiSceneType.PRE_CONSULTATION,
                            AiSessionStatus.ACTIVE,
                            "头痛三天",
                            "头痛三天伴低烧",
                            OffsetDateTime.parse("2026-04-12T09:30:00+08:00"),
                            null));
        }

        @Override
        public Optional<AiSessionDetail> findSessionDetailById(Long sessionId) {
            return Optional.of(new AiSessionDetail(
                    9001L,
                    1001L,
                    2001L,
                    AiSceneType.PRE_CONSULTATION,
                    AiSessionStatus.ACTIVE,
                    "头痛三天",
                    "头痛三天伴低烧",
                    OffsetDateTime.parse("2026-04-12T09:30:00+08:00"),
                    null,
                    List.of(new AiSessionTurnDetail(
                            9101L,
                            1,
                            AiTurnStatus.COMPLETED,
                            OffsetDateTime.parse("2026-04-12T09:30:00+08:00"),
                            OffsetDateTime.parse("2026-04-12T09:31:00+08:00"),
                            null,
                            null,
                            List.of(
                                    new AiSessionMessage(
                                            AiContentRole.USER,
                                            "enc<头痛三天>",
                                            OffsetDateTime.parse("2026-04-12T09:30:00+08:00")),
                                    new AiSessionMessage(
                                            AiContentRole.ASSISTANT,
                                            "enc<建议挂神经内科>",
                                            OffsetDateTime.parse("2026-04-12T09:31:00+08:00")))))));
        }

        @Override
        public Optional<AiSessionTriageResultView> findLatestTriageResultBySessionId(Long sessionId) {
            return Optional.of(new AiSessionTriageResultView(
                    9001L,
                    1001L,
                    "头痛三天",
                    RiskLevel.MEDIUM,
                    GuardrailAction.CAUTION,
                    List.of(new RecommendedDepartment(101L, "神经内科", 1, "持续头痛")),
                    "建议线下就诊",
                    List.of(new AiCitation(7001L, 1, 0.82D, "持续头痛建议线下评估"))));
        }
    }
}
