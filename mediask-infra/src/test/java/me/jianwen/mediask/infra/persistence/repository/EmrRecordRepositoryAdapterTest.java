package me.jianwen.mediask.infra.persistence.repository;

import me.jianwen.mediask.domain.ai.port.AiContentEncryptorPort;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.clinical.exception.ClinicalErrorCode;
import me.jianwen.mediask.domain.clinical.model.EmrRecordAccess;
import me.jianwen.mediask.domain.clinical.model.EmrDiagnosis;
import me.jianwen.mediask.domain.clinical.model.EmrRecord;
import me.jianwen.mediask.domain.clinical.model.EmrRecordListItem;
import me.jianwen.mediask.domain.clinical.model.EmrRecordStatus;
import me.jianwen.mediask.infra.persistence.dataobject.EmrDiagnosisDO;
import me.jianwen.mediask.infra.persistence.dataobject.EmrRecordDO;
import me.jianwen.mediask.infra.persistence.mapper.EmrRecordMapper;
import me.jianwen.mediask.infra.persistence.mapper.EmrRecordListRow;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        assertEquals("encrypted:Detailed medical examination findings...", handler.insertedRecord.getContentEncrypted());
        assertNotNull(handler.insertedRecord.getContentMasked());
        assertNotNull(handler.insertedRecord.getContentHash());
        assertEquals(64, handler.insertedRecord.getContentHash().length());

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
    void findByEncounterId_WhenRecordExists_ReturnsDecryptedRecord() {
        EmrMapperCapturingHandler handler = new EmrMapperCapturingHandler();
        handler.selectedRecord = new EmrRecordDO();
        handler.selectedRecord.setId(7101L);
        handler.selectedRecord.setRecordNo("EMR123456");
        handler.selectedRecord.setEncounterId(8101L);
        handler.selectedRecord.setPatientId(1001L);
        handler.selectedRecord.setDoctorId(2101L);
        handler.selectedRecord.setDepartmentId(3101L);
        handler.selectedRecord.setRecordStatus(EmrRecordStatus.DRAFT.name());
        handler.selectedRecord.setChiefComplaintSummary("Headache and congestion");
        handler.selectedRecord.setVersion(0);
        handler.selectedRecord.setCreatedAt(OffsetDateTime.parse("2026-04-19T10:00:00+08:00"));
        handler.selectedRecord.setUpdatedAt(OffsetDateTime.parse("2026-04-19T10:00:00+08:00"));
        handler.selectedRecord.setContentEncrypted("encrypted:Detailed medical examination findings...");

        EmrDiagnosisDO primaryDiagnosis = new EmrDiagnosisDO();
        primaryDiagnosis.setId(81001L);
        primaryDiagnosis.setRecordId(7101L);
        primaryDiagnosis.setDiagnosisType("PRIMARY");
        primaryDiagnosis.setDiagnosisCode("J01.90");
        primaryDiagnosis.setDiagnosisName("Acute sinusitis");
        primaryDiagnosis.setIsPrimary(true);
        primaryDiagnosis.setSortOrder(0);
        handler.selectedDiagnoses = List.of(primaryDiagnosis);

        EmrRecordRepositoryAdapter adapter = new EmrRecordRepositoryAdapter(
                new StubAiContentEncryptor(),
                proxy(EmrRecordMapper.class, Map.of(
                        "selectByEncounterId", handler::selectByEncounterId,
                        "selectDiagnosesByRecordId", handler::selectDiagnosesByRecordId
                ))
        );

        Optional<EmrRecord> result = adapter.findByEncounterId(8101L);

        assertTrue(result.isPresent());
        assertEquals(8101L, handler.selectedEncounterIdArg);
        assertEquals(7101L, handler.selectedDiagnosesRecordIdArg);
        assertEquals("Detailed medical examination findings...", result.get().content());
        assertEquals(1, result.get().diagnoses().size());
        assertEquals("Acute sinusitis", result.get().diagnoses().get(0).diagnosisName());
    }

    @Test
    void findRecordIdByEncounterId_WhenRecordExists_ReturnsRecordId() {
        EmrMapperCapturingHandler handler = new EmrMapperCapturingHandler();
        handler.selectedRecordId = 7101L;

        EmrRecordRepositoryAdapter adapter = new EmrRecordRepositoryAdapter(
                new StubAiContentEncryptor(),
                proxy(EmrRecordMapper.class, Map.of("selectRecordIdByEncounterId", handler::selectRecordIdByEncounterId))
        );

        Optional<Long> result = adapter.findRecordIdByEncounterId(8101L);

        assertEquals(8101L, handler.selectedEncounterIdArg);
        assertTrue(result.isPresent());
        assertEquals(7101L, result.get());
    }

    @Test
    void findAccessByRecordId_WhenRecordExists_ReturnsAccessContext() {
        EmrMapperCapturingHandler handler = new EmrMapperCapturingHandler();
        handler.selectedAccessRecord = new EmrRecordDO();
        handler.selectedAccessRecord.setId(7101L);
        handler.selectedAccessRecord.setPatientId(1001L);
        handler.selectedAccessRecord.setDepartmentId(3101L);

        EmrRecordRepositoryAdapter adapter = new EmrRecordRepositoryAdapter(
                new StubAiContentEncryptor(),
                proxy(EmrRecordMapper.class, Map.of("selectAccessByRecordId", handler::selectAccessByRecordId))
        );

        Optional<EmrRecordAccess> result = adapter.findAccessByRecordId(7101L);

        assertEquals(7101L, handler.selectedAccessRecordIdArg);
        assertTrue(result.isPresent());
        assertEquals(1001L, result.get().patientId());
        assertEquals(3101L, result.get().departmentId());
    }

    @Test
    void listByPatientUserId_WhenRowsExist_MapSummaryRows() {
        EmrMapperCapturingHandler handler = new EmrMapperCapturingHandler();
        EmrRecordListRow row = new EmrRecordListRow();
        row.setRecordId(7101L);
        row.setRecordNo("EMR20260504001");
        row.setEncounterId(8104L);
        row.setRecordStatus("DRAFT");
        row.setDepartmentId(3102L);
        row.setDepartmentName("全科门诊");
        row.setDoctorId(3203L);
        row.setDoctorName("李医生");
        row.setSessionDate(java.time.LocalDate.parse("2026-05-04"));
        row.setChiefComplaintSummary("发热缓解后复诊评估");
        row.setCreatedAt(OffsetDateTime.parse("2026-05-04T15:10:00+08:00"));
        handler.listRows = List.of(row);

        EmrRecordRepositoryAdapter adapter = new EmrRecordRepositoryAdapter(
                new StubAiContentEncryptor(),
                proxy(EmrRecordMapper.class, Map.of("selectListByPatientUserId", handler::selectListByPatientUserId))
        );

        List<EmrRecordListItem> result = adapter.listByPatientUserId(2003L, 8101L);

        assertEquals(2003L, handler.listPatientUserIdArg);
        assertEquals(8101L, handler.listExcludeEncounterIdArg);
        assertEquals(1, result.size());
        assertEquals(7101L, result.getFirst().recordId());
        assertEquals("李医生", result.getFirst().doctorName());
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
            return encryptedText.replaceFirst("^encrypted:", "");
        }
    }

    private static final class EmrMapperCapturingHandler {
        EmrRecordDO insertedRecord;
        List<EmrDiagnosisDO> insertedDiagnoses;
        Long existsByEncounterIdArg;
        Boolean existsByEncounterIdResult;
        Long selectedEncounterIdArg;
        Long selectedAccessRecordIdArg;
        Long selectedDiagnosesRecordIdArg;
        Long selectedRecordId;
        Long listPatientUserIdArg;
        Long listExcludeEncounterIdArg;
        EmrRecordDO selectedRecord;
        EmrRecordDO selectedAccessRecord;
        List<EmrDiagnosisDO> selectedDiagnoses;
        List<EmrRecordListRow> listRows;

        private Object insertRecord(Object[] arguments) {
            this.insertedRecord = (EmrRecordDO) arguments[0];
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

        private Object selectByEncounterId(Object[] arguments) {
            this.selectedEncounterIdArg = (Long) arguments[0];
            return Optional.ofNullable(selectedRecord);
        }

        private Object selectRecordIdByEncounterId(Object[] arguments) {
            this.selectedEncounterIdArg = (Long) arguments[0];
            return Optional.ofNullable(selectedRecordId);
        }

        private Object selectAccessByRecordId(Object[] arguments) {
            this.selectedAccessRecordIdArg = (Long) arguments[0];
            return Optional.ofNullable(selectedAccessRecord);
        }

        private Object selectDiagnosesByRecordId(Object[] arguments) {
            this.selectedDiagnosesRecordIdArg = (Long) arguments[0];
            return selectedDiagnoses == null ? List.of() : selectedDiagnoses;
        }

        private Object selectListByPatientUserId(Object[] arguments) {
            this.listPatientUserIdArg = (Long) arguments[0];
            this.listExcludeEncounterIdArg = (Long) arguments[1];
            return listRows == null ? List.of() : listRows;
        }
    }
}
