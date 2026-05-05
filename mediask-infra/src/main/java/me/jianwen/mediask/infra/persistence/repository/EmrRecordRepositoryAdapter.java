package me.jianwen.mediask.infra.persistence.repository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.ai.port.AiContentEncryptorPort;
import me.jianwen.mediask.domain.clinical.exception.ClinicalErrorCode;
import me.jianwen.mediask.domain.clinical.model.EmrRecordAccess;
import me.jianwen.mediask.domain.clinical.model.EmrDiagnosis;
import me.jianwen.mediask.domain.clinical.model.EmrRecord;
import me.jianwen.mediask.domain.clinical.port.EmrRecordQueryRepository;
import me.jianwen.mediask.domain.clinical.port.EmrRecordRepository;
import me.jianwen.mediask.infra.persistence.dataobject.EmrDiagnosisDO;
import me.jianwen.mediask.infra.persistence.dataobject.EmrRecordDO;
import me.jianwen.mediask.infra.persistence.mapper.EmrRecordMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

@Component
public class EmrRecordRepositoryAdapter implements EmrRecordRepository, EmrRecordQueryRepository {

    private final AiContentEncryptorPort aiContentEncryptorPort;
    private final EmrRecordMapper emrRecordMapper;

    public EmrRecordRepositoryAdapter(
            AiContentEncryptorPort aiContentEncryptorPort,
            EmrRecordMapper emrRecordMapper) {
        this.aiContentEncryptorPort = aiContentEncryptorPort;
        this.emrRecordMapper = emrRecordMapper;
    }

    @Override
    public void save(EmrRecord emrRecord) {
        try {
            EmrRecordDO recordDO = new EmrRecordDO();
            recordDO.setId(emrRecord.recordId());
            recordDO.setRecordNo(emrRecord.recordNo());
            recordDO.setEncounterId(emrRecord.encounterId());
            recordDO.setPatientId(emrRecord.patientId());
            recordDO.setDoctorId(emrRecord.doctorId());
            recordDO.setDepartmentId(emrRecord.departmentId());
            recordDO.setRecordStatus(emrRecord.recordStatus().name());
            recordDO.setChiefComplaintSummary(emrRecord.chiefComplaintSummary());
            recordDO.setContentEncrypted(aiContentEncryptorPort.encrypt(emrRecord.content()));
            recordDO.setContentMasked(maskContent(emrRecord.content()));
            recordDO.setContentHash(computeContentHash(emrRecord.content()));
            recordDO.setVersion(emrRecord.version());
            recordDO.setCreatedAt(emrRecord.createdAt().atOffset(ZoneOffset.UTC));
            recordDO.setUpdatedAt(emrRecord.updatedAt().atOffset(ZoneOffset.UTC));
            emrRecordMapper.insert(recordDO);

            List<EmrDiagnosisDO> diagnosisDOs = emrRecord.diagnoses().stream()
                    .map(diagnosis -> {
                        EmrDiagnosisDO diagnosisDO = new EmrDiagnosisDO();
                        diagnosisDO.setId(me.jianwen.mediask.common.id.SnowflakeIdGenerator.nextId());
                        diagnosisDO.setRecordId(emrRecord.recordId());
                        diagnosisDO.setDiagnosisType(diagnosis.diagnosisType().name());
                        diagnosisDO.setDiagnosisCode(diagnosis.diagnosisCode());
                        diagnosisDO.setDiagnosisName(diagnosis.diagnosisName());
                        diagnosisDO.setIsPrimary(diagnosis.isPrimary());
                        diagnosisDO.setSortOrder(diagnosis.sortOrder());
                        diagnosisDO.setCreatedAt(emrRecord.createdAt().atOffset(ZoneOffset.UTC));
                        return diagnosisDO;
                    })
                    .toList();

            if (!diagnosisDOs.isEmpty()) {
                emrRecordMapper.insertDiagnoses(diagnosisDOs);
            }
        } catch (DuplicateKeyException exception) {
            throw mapDuplicateKeyOnSave(exception);
        }
    }

    @Override
    public boolean existsByEncounterId(Long encounterId) {
        return emrRecordMapper.existsByEncounterId(encounterId);
    }

    @Override
    public Optional<EmrRecord> findByEncounterId(Long encounterId) {
        return emrRecordMapper.selectByEncounterId(encounterId).map(recordDO -> {
            List<EmrDiagnosis> diagnoses = emrRecordMapper.selectDiagnosesByRecordId(recordDO.getId()).stream()
                    .map(diagnosisDO -> new EmrDiagnosis(
                            EmrDiagnosis.DiagnosisType.valueOf(diagnosisDO.getDiagnosisType()),
                            diagnosisDO.getDiagnosisCode(),
                            diagnosisDO.getDiagnosisName(),
                            Boolean.TRUE.equals(diagnosisDO.getIsPrimary()),
                            diagnosisDO.getSortOrder()))
                    .toList();
            return new EmrRecord(
                    recordDO.getId(),
                    recordDO.getRecordNo(),
                    recordDO.getEncounterId(),
                    recordDO.getPatientId(),
                    recordDO.getDoctorId(),
                    recordDO.getDepartmentId(),
                    me.jianwen.mediask.domain.clinical.model.EmrRecordStatus.valueOf(recordDO.getRecordStatus()),
                    recordDO.getChiefComplaintSummary(),
                    aiContentEncryptorPort.decrypt(recordDO.getContentEncrypted()),
                    diagnoses,
                    recordDO.getVersion(),
                    recordDO.getCreatedAt().toInstant(),
                    recordDO.getUpdatedAt().toInstant());
        });
    }

    @Override
    public Optional<Long> findRecordIdByEncounterId(Long encounterId) {
        return emrRecordMapper.selectRecordIdByEncounterId(encounterId);
    }

    @Override
    public Optional<EmrRecordAccess> findAccessByRecordId(Long recordId) {
        return emrRecordMapper.selectAccessByRecordId(recordId)
                .map(recordDO -> new EmrRecordAccess(recordDO.getId(), recordDO.getPatientId(), recordDO.getDepartmentId()));
    }

    private static final Pattern PHONE_PATTERN = Pattern.compile("(\\d{3})\\d{4}(\\d{4})");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("(\\d{4})\\d{10}(\\d{4})");
    private static final Pattern NAME_IN_CONTEXT_PATTERN = Pattern.compile("(姓名[：:][\\s]*)([\\u4e00-\\u9fa5]{2,4})");

    private String maskContent(String content) {
        if (content == null || content.isBlank()) {
            return content;
        }
        String masked = ID_CARD_PATTERN.matcher(content).replaceAll("$1**********$2");
        masked = PHONE_PATTERN.matcher(masked).replaceAll("$1****$2");
        masked = NAME_IN_CONTEXT_PATTERN.matcher(masked).replaceAll("$1**");
        return masked;
    }

    private String computeContentHash(String content) {
        if (content == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private BizException mapDuplicateKeyOnSave(DuplicateKeyException exception) {
        String message = exception.getMessage();
        if (message != null && message.contains("uk_emr_record_encounter")) {
            return new BizException(ClinicalErrorCode.EMR_RECORD_ALREADY_EXISTS);
        }
        throw exception;
    }
}
