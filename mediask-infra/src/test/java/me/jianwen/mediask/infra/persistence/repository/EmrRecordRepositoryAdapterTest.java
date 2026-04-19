package me.jianwen.mediask.infra.persistence.repository;

import me.jianwen.mediask.domain.ai.port.AiContentEncryptorPort;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.clinical.exception.ClinicalErrorCode;
import me.jianwen.mediask.domain.clinical.model.EmrDiagnosis;
import me.jianwen.mediask.domain.clinical.model.EmrRecord;
import me.jianwen.mediask.domain.clinical.model.EmrRecordStatus;
import me.jianwen.mediask.infra.persistence.dataobject.EmrDiagnosisDO;
import me.jianwen.mediask.infra.persistence.dataobject.EmrRecordContentDO;
import me.jianwen.mediask.infra.persistence.dataobject.EmrRecordDO;
import me.jianwen.mediask.infra.persistence.mapper.EmrRecordMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import org.springframework.dao.DuplicateKeyException;

class EmrRecordRepositoryAdapterTest {

    @Test
    void save_WhenCalled_InsertsRecordContentAndDiagnoses() {
        EmrMapperCapturingHandler handler = new EmrMapperCapturingHandler();
        EmrRecordRepositoryAdapter adapter = new EmrRecordRepositoryAdapter(
                new StubAiContentEncryptor(),
                proxy(EmrRecordMapper.class, Map.of(
                        "insert", handler::insertRecord,
                        "insertContent", handler::insertContent,
                        "insertDiagnoses", handler::insertDiagnoses
                ))
        );

        List<EmrDiagnosis> diagnoses = List.of(
                EmrDiagnosis.createPrimary("J01.90", "Acute sinusitis", 0),
                EmrDiagnosis.createSecondary("J06.9", "Acute upper respiratory infection", 1)
        );

        EmrRecord record = EmrRecord.createDraft(
                "EMR123456",
                1L,
                100L,
                200L,
                300L,
                "Headache and congestion",
                "Detailed medical examination findings...",
                diagnoses
        );

        adapter.save(record);

        // Verify record insert
        assertNotNull(handler.insertedRecord);
        assertEquals(record.recordId(), handler.insertedRecord.getId());
        assertEquals("EMR123456", handler.insertedRecord.getRecordNo());
        assertEquals(1L, handler.insertedRecord.getEncounterId());
        assertEquals(EmrRecordStatus.DRAFT.name(), handler.insertedRecord.getRecordStatus());
        assertEquals("Headache and congestion", handler.insertedRecord.getChiefComplaintSummary());

        // Verify content insert
        assertNotNull(handler.insertedContent);
        assertEquals(record.recordId(), handler.insertedContent.getRecordId());
        assertNotEquals("Detailed medical examination findings...", handler.insertedContent.getContentEncrypted());
        assertEquals("encrypted:Detailed medical examination findings...", handler.insertedContent.getContentEncrypted());
        assertNotNull(handler.insertedContent.getContentMasked());
        assertNotNull(handler.insertedContent.getContentHash());
        assertEquals(64, handler.insertedContent.getContentHash().length());

        // Verify diagnoses insert
        assertNotNull(handler.insertedDiagnoses);
        assertEquals(2, handler.insertedDiagnoses.size());
        assertEquals("PRIMARY", handler.insertedDiagnoses.get(0).getDiagnosisType());
        assertEquals("Acute sinusitis", handler.insertedDiagnoses.get(0).getDiagnosisName());
        assertTrue(handler.insertedDiagnoses.get(0).getIsPrimary());
        assertEquals("SECONDARY", handler.insertedDiagnoses.get(1).getDiagnosisType());
        assertEquals("Acute upper respiratory infection", handler.insertedDiagnoses.get(1).getDiagnosisName());
        assertFalse(handler.insertedDiagnoses.get(1).getIsPrimary());
    }

    @Test
    void existsByEncounterId_WhenExists_ReturnsTrue() {
        EmrMapperCapturingHandler handler = new EmrMapperCapturingHandler();
        handler.existsByEncounterIdResult = true;

        EmrRecordRepositoryAdapter adapter = new EmrRecordRepositoryAdapter(
                new StubAiContentEncryptor(),
                proxy(EmrRecordMapper.class, Map.of("existsByEncounterId", handler::existsByEncounterId))
        );

        boolean result = adapter.existsByEncounterId(1L);

        assertTrue(result);
        assertEquals(1L, handler.existsByEncounterIdArg);
    }

    @Test
    void existsByEncounterId_WhenNotExists_ReturnsFalse() {
        EmrMapperCapturingHandler handler = new EmrMapperCapturingHandler();
        handler.existsByEncounterIdResult = false;

        EmrRecordRepositoryAdapter adapter = new EmrRecordRepositoryAdapter(
                new StubAiContentEncryptor(),
                proxy(EmrRecordMapper.class, Map.of("existsByEncounterId", handler::existsByEncounterId))
        );

        boolean result = adapter.existsByEncounterId(1L);

        assertFalse(result);
        assertEquals(1L, handler.existsByEncounterIdArg);
    }

    @Test
    void save_WhenEncounterDuplicate_ThrowsConflictBizException() {
        EmrRecordRepositoryAdapter adapter = new EmrRecordRepositoryAdapter(
                new StubAiContentEncryptor(),
                proxy(EmrRecordMapper.class, Map.of(
                        "insert", arguments -> {
                            throw new DuplicateKeyException("duplicate key value violates unique constraint \"uk_emr_record_encounter\"");
                        }
                ))
        );

        BizException exception = assertThrows(BizException.class, () -> adapter.save(emrRecord()));

        assertEquals(ClinicalErrorCode.EMR_RECORD_ALREADY_EXISTS.getCode(), exception.getCode());
    }

    @Test
    void save_WhenOtherDuplicateKey_PropagatesOriginalException() {
        EmrRecordRepositoryAdapter adapter = new EmrRecordRepositoryAdapter(
                new StubAiContentEncryptor(),
                proxy(EmrRecordMapper.class, Map.of(
                        "insert", arguments -> {
                            throw new DuplicateKeyException("duplicate key value violates unique constraint \"uk_emr_record_no\"");
                        }
                ))
        );

        assertThrows(DuplicateKeyException.class, () -> adapter.save(emrRecord()));
    }

    private EmrRecord emrRecord() {
        return EmrRecord.createDraft(
                "EMR123456",
                1L,
                100L,
                200L,
                300L,
                "Headache and congestion",
                "Detailed medical examination findings...",
                List.of(EmrDiagnosis.createPrimary("J01.90", "Acute sinusitis", 0))
        );
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

    private static final class StubAiContentEncryptor implements AiContentEncryptorPort {
        @Override
        public String encrypt(String plainText) {
            return "encrypted:" + plainText;
        }

        @Override
        public String decrypt(String encryptedText) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class EmrMapperCapturingHandler {
        EmrRecordDO insertedRecord;
        EmrRecordContentDO insertedContent;
        List<EmrDiagnosisDO> insertedDiagnoses;
        Long existsByEncounterIdArg;
        Boolean existsByEncounterIdResult;

        private Object insertRecord(Object[] arguments) {
            this.insertedRecord = (EmrRecordDO) arguments[0];
            return 1;
        }

        private Object insertContent(Object[] arguments) {
            this.insertedContent = (EmrRecordContentDO) arguments[0];
            return 1;
        }

        private Object insertDiagnoses(Object[] arguments) {
            this.insertedDiagnoses = (List<EmrDiagnosisDO>) arguments[0];
            return arguments.length;
        }

        private Object existsByEncounterId(Object[] arguments) {
            this.existsByEncounterIdArg = (Long) arguments[0];
            return existsByEncounterIdResult;
        }
    }
}
