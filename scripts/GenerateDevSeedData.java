import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class GenerateDevSeedData {

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Shanghai");
    private static final ZoneOffset ZONE_OFFSET = ZoneOffset.ofHours(8);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter OFFSET_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssXXX");
    private static final Path OUTPATIENT_SQL = Path.of("docs/sql/99-seed-04-outpatient.sql");
    private static final Path MEDICAL_SQL = Path.of("docs/sql/99-seed-05-medical.sql");

    public static void main(String[] args) throws Exception {
        byte[] encryptionKey = readEncryptionKey();
        LocalDate today = LocalDate.now(ZONE_ID);

        List<SessionSeed> sessions = new ArrayList<>();
        List<SlotSeed> slots = new ArrayList<>();
        buildFutureSessions(today, sessions, slots);
        buildHistoricalSessions(today, sessions, slots);

        List<RegistrationSeed> registrations = buildRegistrations(today);
        List<EncounterSeed> encounters = buildEncounters(today);
        List<EmrSeed> emrs = buildEmrs(today, encryptionKey);
        List<PrescriptionSeed> prescriptions = buildPrescriptions(today);

        Files.writeString(OUTPATIENT_SQL, renderOutpatientSql(today, sessions, slots, registrations, encounters), StandardCharsets.UTF_8);
        Files.writeString(MEDICAL_SQL, renderMedicalSql(today, emrs, prescriptions), StandardCharsets.UTF_8);
    }

    private static byte[] readEncryptionKey() {
        String encodedKey = System.getenv("MEDIASK_ENCRYPTION_KEY");
        if (encodedKey == null || encodedKey.isBlank()) {
            throw new IllegalStateException("MEDIASK_ENCRYPTION_KEY is required to generate encrypted EMR seed data");
        }
        byte[] decoded = Base64.getDecoder().decode(encodedKey);
        if (decoded.length != 16 && decoded.length != 24 && decoded.length != 32) {
            throw new IllegalStateException("MEDIASK_ENCRYPTION_KEY must decode to a valid AES key length");
        }
        return decoded;
    }

    private static void buildFutureSessions(LocalDate today, List<SessionSeed> sessions, List<SlotSeed> slots) {
        DoctorTemplate[] templates = new DoctorTemplate[] {
            new DoctorTemplate(3201L, 3103L, "GENERAL", "MORNING", 25.00, LocalTime.of(9, 0), 15),
            new DoctorTemplate(3202L, 3101L, "SPECIALIST", "AFTERNOON", 40.00, LocalTime.of(14, 0), 20),
            new DoctorTemplate(3203L, 3102L, "GENERAL", "MORNING", 30.00, LocalTime.of(10, 0), 20)
        };

        long sessionId = 4101L;
        long slotId = 5101L;
        for (int dayOffset = 1; dayOffset <= 7; dayOffset++) {
            LocalDate sessionDate = today.plusDays(dayOffset);
            for (DoctorTemplate template : templates) {
                long currentSessionId = sessionId++;
                int remaining = currentSessionId == 4101L ? 3 : 4;
                sessions.add(new SessionSeed(
                        currentSessionId,
                        3001L,
                        template.departmentId(),
                        template.doctorId(),
                        sessionDate,
                        template.periodCode(),
                        template.clinicType(),
                        "OPEN",
                        4,
                        remaining,
                        template.fee(),
                        "MANUAL"));
                for (int slotSeq = 1; slotSeq <= 4; slotSeq++) {
                    LocalTime slotStart = template.startTime().plusMinutes((long) (slotSeq - 1) * template.slotDurationMinutes());
                    LocalTime slotEnd = slotStart.plusMinutes(template.slotDurationMinutes());
                    boolean booked = currentSessionId == 4101L && slotSeq == 1;
                    slots.add(new SlotSeed(
                            slotId++,
                            currentSessionId,
                            slotSeq,
                            sessionDate.atTime(slotStart).atOffset(ZONE_OFFSET),
                            sessionDate.atTime(slotEnd).atOffset(ZONE_OFFSET),
                            booked ? "BOOKED" : "AVAILABLE",
                            1,
                            booked ? 0 : 1));
                }
            }
        }
    }

    private static void buildHistoricalSessions(LocalDate today, List<SessionSeed> sessions, List<SlotSeed> slots) {
        sessions.add(new SessionSeed(4201L, 3001L, 3103L, 3201L, today.minusDays(5), "MORNING", "GENERAL", "CLOSED", 1, 0, 25.00, "MANUAL"));
        sessions.add(new SessionSeed(4202L, 3001L, 3101L, 3202L, today, "AFTERNOON", "SPECIALIST", "CLOSED", 1, 0, 40.00, "MANUAL"));
        sessions.add(new SessionSeed(4203L, 3001L, 3102L, 3203L, today.minusDays(1), "AFTERNOON", "GENERAL", "CLOSED", 1, 0, 30.00, "MANUAL"));

        slots.add(new SlotSeed(
                5201L,
                4201L,
                1,
                today.minusDays(5).atTime(9, 0).atOffset(ZONE_OFFSET),
                today.minusDays(5).atTime(9, 15).atOffset(ZONE_OFFSET),
                "BOOKED",
                1,
                0));
        slots.add(new SlotSeed(
                5202L,
                4202L,
                1,
                today.atTime(14, 0).atOffset(ZONE_OFFSET),
                today.atTime(14, 20).atOffset(ZONE_OFFSET),
                "BOOKED",
                1,
                0));
        slots.add(new SlotSeed(
                5203L,
                4203L,
                1,
                today.minusDays(1).atTime(15, 0).atOffset(ZONE_OFFSET),
                today.minusDays(1).atTime(15, 20).atOffset(ZONE_OFFSET),
                "BOOKED",
                1,
                0));
    }

    private static List<RegistrationSeed> buildRegistrations(LocalDate today) {
        return List.of(
                new RegistrationSeed(
                        6101L,
                        "REG" + today.plusDays(1).format(DateTimeFormatter.BASIC_ISO_DATE) + "001",
                        2003L,
                        3201L,
                        3103L,
                        4101L,
                        5101L,
                        "CONFIRMED",
                        25.00,
                        today.atTime(8, 30).atOffset(ZONE_OFFSET),
                        today.atTime(8, 30).atOffset(ZONE_OFFSET),
                        today.atTime(8, 45).atOffset(ZONE_OFFSET)),
                new RegistrationSeed(
                        6102L,
                        "REG" + today.minusDays(5).format(DateTimeFormatter.BASIC_ISO_DATE) + "001",
                        2006L,
                        3201L,
                        3103L,
                        4201L,
                        5201L,
                        "COMPLETED",
                        25.00,
                        today.minusDays(5).atTime(8, 30).atOffset(ZONE_OFFSET),
                        today.minusDays(5).atTime(8, 30).atOffset(ZONE_OFFSET),
                        today.minusDays(5).atTime(10, 30).atOffset(ZONE_OFFSET)),
                new RegistrationSeed(
                        6103L,
                        "REG" + today.format(DateTimeFormatter.BASIC_ISO_DATE) + "001",
                        2007L,
                        3202L,
                        3101L,
                        4202L,
                        5202L,
                        "CONFIRMED",
                        40.00,
                        today.atTime(11, 30).atOffset(ZONE_OFFSET),
                        today.atTime(11, 30).atOffset(ZONE_OFFSET),
                        today.atTime(13, 40).atOffset(ZONE_OFFSET)),
                new RegistrationSeed(
                        6104L,
                        "REG" + today.minusDays(1).format(DateTimeFormatter.BASIC_ISO_DATE) + "001",
                        2003L,
                        3203L,
                        3102L,
                        4203L,
                        5203L,
                        "COMPLETED",
                        30.00,
                        today.minusDays(1).atTime(13, 30).atOffset(ZONE_OFFSET),
                        today.minusDays(1).atTime(13, 30).atOffset(ZONE_OFFSET),
                        today.minusDays(1).atTime(16, 0).atOffset(ZONE_OFFSET)));
    }

    private static List<EncounterSeed> buildEncounters(LocalDate today) {
        return List.of(
                new EncounterSeed(
                        8101L,
                        6101L,
                        2003L,
                        3201L,
                        3103L,
                        "SCHEDULED",
                        null,
                        null,
                        "已完成预问诊，待医生正式接诊并录入病历。",
                        today.atTime(8, 30).atOffset(ZONE_OFFSET),
                        today.atTime(8, 45).atOffset(ZONE_OFFSET)),
                new EncounterSeed(
                        8102L,
                        6102L,
                        2006L,
                        3201L,
                        3103L,
                        "COMPLETED",
                        today.minusDays(5).atTime(9, 0).atOffset(ZONE_OFFSET),
                        today.minusDays(5).atTime(9, 40).atOffset(ZONE_OFFSET),
                        "发热伴咽痛已完成线下面诊，医生完成病历记录并开具口服药物。",
                        today.minusDays(5).atTime(8, 30).atOffset(ZONE_OFFSET),
                        today.minusDays(5).atTime(9, 50).atOffset(ZONE_OFFSET)),
                new EncounterSeed(
                        8103L,
                        6103L,
                        2007L,
                        3202L,
                        3101L,
                        "IN_PROGRESS",
                        today.atTime(14, 0).minusMinutes(20).atOffset(ZONE_OFFSET),
                        null,
                        "患者已进入诊室，医生正在结合症状摘要进行接诊。",
                        today.atTime(11, 30).atOffset(ZONE_OFFSET),
                        today.atTime(13, 40).atOffset(ZONE_OFFSET)),
                new EncounterSeed(
                        8104L,
                        6104L,
                        2003L,
                        3203L,
                        3102L,
                        "COMPLETED",
                        today.minusDays(1).atTime(15, 0).atOffset(ZONE_OFFSET),
                        today.minusDays(1).atTime(15, 25).atOffset(ZONE_OFFSET),
                        "发热症状已完成线下面诊，医生建议休息补液并观察体温变化。",
                        today.minusDays(1).atTime(13, 30).atOffset(ZONE_OFFSET),
                        today.minusDays(1).atTime(15, 30).atOffset(ZONE_OFFSET)));
    }

    private static List<EmrSeed> buildEmrs(LocalDate today, byte[] encryptionKey) throws Exception {
        String emr8102Content = "患者主诉发热伴咽痛 2 天，无明显呼吸困难。查体提示咽部充血，考虑急性上呼吸道感染，建议多饮水、规律休息并口服对症药物。";
        String emr8104Content = "患者发热症状较前明显缓解，当前无寒战和胸闷。查体生命体征平稳，考虑病毒感染恢复期，建议居家休息、补液并监测体温。";
        return List.of(
                new EmrSeed(
                        9101L,
                        "EMR" + today.minusDays(5).format(DateTimeFormatter.BASIC_ISO_DATE) + "001",
                        8102L,
                        2006L,
                        3201L,
                        3103L,
                        "DRAFT",
                        "发热伴咽痛 2 天",
                        encrypt("record-9101", emr8102Content, encryptionKey),
                        emr8102Content,
                        sha256(emr8102Content),
                        today.minusDays(5).atTime(9, 15).atOffset(ZONE_OFFSET),
                        today.minusDays(5).atTime(9, 50).atOffset(ZONE_OFFSET),
                        List.of(
                                new DiagnosisSeed(9201L, "PRIMARY", "J06.9", "急性上呼吸道感染", true, 0, today.minusDays(5).atTime(9, 20).atOffset(ZONE_OFFSET)),
                                new DiagnosisSeed(9202L, "SECONDARY", "R50.9", "发热", false, 1, today.minusDays(5).atTime(9, 20).atOffset(ZONE_OFFSET)))),
                new EmrSeed(
                        9102L,
                        "EMR" + today.minusDays(1).format(DateTimeFormatter.BASIC_ISO_DATE) + "001",
                        8104L,
                        2003L,
                        3203L,
                        3102L,
                        "DRAFT",
                        "发热缓解后复诊评估",
                        encrypt("record-9102", emr8104Content, encryptionKey),
                        emr8104Content,
                        sha256(emr8104Content),
                        today.minusDays(1).atTime(15, 10).atOffset(ZONE_OFFSET),
                        today.minusDays(1).atTime(15, 30).atOffset(ZONE_OFFSET),
                        List.of(
                                new DiagnosisSeed(9203L, "PRIMARY", "B34.9", "病毒感染恢复期", true, 0, today.minusDays(1).atTime(15, 15).atOffset(ZONE_OFFSET))))
        );
    }

    private static List<PrescriptionSeed> buildPrescriptions(LocalDate today) {
        return List.of(new PrescriptionSeed(
                9301L,
                "RX" + today.minusDays(5).format(DateTimeFormatter.BASIC_ISO_DATE) + "001",
                9101L,
                8102L,
                2006L,
                3201L,
                "DRAFT",
                today.minusDays(5).atTime(9, 30).atOffset(ZONE_OFFSET),
                today.minusDays(5).atTime(9, 50).atOffset(ZONE_OFFSET),
                List.of(
                        new PrescriptionItemSeed(9401L, 0, "阿莫西林胶囊", "0.25g*24粒", "每次2粒", "每日3次", "5天", "30.00", "粒", "口服", today.minusDays(5).atTime(9, 30).atOffset(ZONE_OFFSET)),
                        new PrescriptionItemSeed(9402L, 1, "对乙酰氨基酚片", "0.5g*12片", "发热时每次1片", "必要时", "3天", "6.00", "片", "口服", today.minusDays(5).atTime(9, 30).atOffset(ZONE_OFFSET)))));
    }

    private static String renderOutpatientSql(
            LocalDate today,
            List<SessionSeed> sessions,
            List<SlotSeed> slots,
            List<RegistrationSeed> registrations,
            List<EncounterSeed> encounters) {
        StringBuilder sql = new StringBuilder();
        sql.append("-- Auto-generated by scripts/GenerateDevSeedData.java on ")
                .append(today)
                .append('\n')
                .append("-- Regenerate with: java scripts/GenerateDevSeedData.java\n\n");
        sql.append("INSERT INTO clinic_session (\n")
                .append("    id,\n")
                .append("    hospital_id,\n")
                .append("    department_id,\n")
                .append("    doctor_id,\n")
                .append("    session_date,\n")
                .append("    period_code,\n")
                .append("    clinic_type,\n")
                .append("    session_status,\n")
                .append("    capacity,\n")
                .append("    remaining_count,\n")
                .append("    fee,\n")
                .append("    source_type\n")
                .append(")\nVALUES\n");
        appendRows(sql, sessions, GenerateDevSeedData::renderSessionRow);
        sql.append("ON CONFLICT (id) DO NOTHING;\n\n");

        sql.append("INSERT INTO clinic_slot (\n")
                .append("    id,\n")
                .append("    session_id,\n")
                .append("    slot_seq,\n")
                .append("    slot_start_time,\n")
                .append("    slot_end_time,\n")
                .append("    slot_status,\n")
                .append("    capacity,\n")
                .append("    remaining_count\n")
                .append(")\nVALUES\n");
        appendRows(sql, slots, GenerateDevSeedData::renderSlotRow);
        sql.append("ON CONFLICT (id) DO NOTHING;\n\n");

        sql.append("INSERT INTO registration_order (\n")
                .append("    id,\n")
                .append("    order_no,\n")
                .append("    patient_id,\n")
                .append("    doctor_id,\n")
                .append("    department_id,\n")
                .append("    session_id,\n")
                .append("    slot_id,\n")
                .append("    source_ai_session_id,\n")
                .append("    order_status,\n")
                .append("    fee,\n")
                .append("    paid_at,\n")
                .append("    created_at,\n")
                .append("    updated_at\n")
                .append(")\nVALUES\n");
        appendRows(sql, registrations, GenerateDevSeedData::renderRegistrationRow);
        sql.append("ON CONFLICT (order_no) DO NOTHING;\n\n");

        sql.append("INSERT INTO visit_encounter (\n")
                .append("    id,\n")
                .append("    order_id,\n")
                .append("    patient_id,\n")
                .append("    doctor_id,\n")
                .append("    department_id,\n")
                .append("    encounter_status,\n")
                .append("    started_at,\n")
                .append("    ended_at,\n")
                .append("    summary,\n")
                .append("    created_at,\n")
                .append("    updated_at\n")
                .append(")\nVALUES\n");
        appendRows(sql, encounters, GenerateDevSeedData::renderEncounterRow);
        sql.append("ON CONFLICT (order_id) DO NOTHING;\n");
        return sql.toString();
    }

    private static String renderMedicalSql(LocalDate today, List<EmrSeed> emrs, List<PrescriptionSeed> prescriptions) {
        StringBuilder sql = new StringBuilder();
        sql.append("-- Auto-generated by scripts/GenerateDevSeedData.java on ")
                .append(today)
                .append('\n')
                .append("-- Regenerate with: java scripts/GenerateDevSeedData.java\n\n");

        sql.append("INSERT INTO emr_record (\n")
                .append("    id,\n")
                .append("    record_no,\n")
                .append("    encounter_id,\n")
                .append("    patient_id,\n")
                .append("    doctor_id,\n")
                .append("    department_id,\n")
                .append("    record_status,\n")
                .append("    chief_complaint_summary,\n")
                .append("    created_at,\n")
                .append("    updated_at\n")
                .append(")\nVALUES\n");
        appendRows(sql, emrs, GenerateDevSeedData::renderEmrRecordRow);
        sql.append("ON CONFLICT (id) DO NOTHING;\n\n");

        sql.append("INSERT INTO emr_record_content (\n")
                .append("    record_id,\n")
                .append("    content_encrypted,\n")
                .append("    content_masked,\n")
                .append("    content_hash,\n")
                .append("    created_at,\n")
                .append("    updated_at\n")
                .append(")\nVALUES\n");
        appendRows(sql, emrs, GenerateDevSeedData::renderEmrContentRow);
        sql.append("ON CONFLICT (record_id) DO NOTHING;\n\n");

        List<DiagnosisSeed> diagnoses = emrs.stream().flatMap(emr -> emr.diagnoses().stream()).toList();
        sql.append("INSERT INTO emr_diagnosis (\n")
                .append("    id,\n")
                .append("    record_id,\n")
                .append("    diagnosis_type,\n")
                .append("    diagnosis_code,\n")
                .append("    diagnosis_name,\n")
                .append("    is_primary,\n")
                .append("    sort_order,\n")
                .append("    created_at\n")
                .append(")\nVALUES\n");
        appendRows(sql, diagnoses, GenerateDevSeedData::renderDiagnosisRow);
        sql.append("ON CONFLICT (id) DO NOTHING;\n\n");

        sql.append("INSERT INTO prescription_order (\n")
                .append("    id,\n")
                .append("    prescription_no,\n")
                .append("    record_id,\n")
                .append("    encounter_id,\n")
                .append("    patient_id,\n")
                .append("    doctor_id,\n")
                .append("    prescription_status,\n")
                .append("    created_at,\n")
                .append("    updated_at\n")
                .append(")\nVALUES\n");
        appendRows(sql, prescriptions, GenerateDevSeedData::renderPrescriptionRow);
        sql.append("ON CONFLICT (id) DO NOTHING;\n\n");

        List<PrescriptionItemSeed> items = prescriptions.stream().flatMap(seed -> seed.items().stream()).toList();
        sql.append("INSERT INTO prescription_item (\n")
                .append("    id,\n")
                .append("    prescription_id,\n")
                .append("    sort_order,\n")
                .append("    drug_name,\n")
                .append("    drug_specification,\n")
                .append("    dosage_text,\n")
                .append("    frequency_text,\n")
                .append("    duration_text,\n")
                .append("    quantity,\n")
                .append("    unit,\n")
                .append("    route,\n")
                .append("    created_at\n")
                .append(")\nVALUES\n");
        appendRows(sql, items, GenerateDevSeedData::renderPrescriptionItemRow);
        sql.append("ON CONFLICT (id) DO NOTHING;\n");
        return sql.toString();
    }

    private static <T> void appendRows(StringBuilder builder, List<T> rows, RowRenderer<T> renderer) {
        for (int i = 0; i < rows.size(); i++) {
            builder.append(renderer.render(rows.get(i)));
            builder.append(i == rows.size() - 1 ? '\n' : ",\n");
        }
    }

    private static String renderSessionRow(SessionSeed session) {
        return "    (" + session.id() + ", 3001, " + session.departmentId() + ", " + session.doctorId()
                + ", DATE '" + session.sessionDate().format(DATE_FORMATTER) + "', '" + session.periodCode() + "', '"
                + session.clinicType() + "', '" + session.sessionStatus() + "', " + session.capacity() + ", "
                + session.remainingCount() + ", " + decimal(session.fee()) + ", '" + session.sourceType() + "')";
    }

    private static String renderSlotRow(SlotSeed slot) {
        return "    (" + slot.id() + ", " + slot.sessionId() + ", " + slot.slotSeq()
                + ", TIMESTAMPTZ '" + timestamp(slot.slotStartTime()) + "', TIMESTAMPTZ '"
                + timestamp(slot.slotEndTime()) + "', '" + slot.slotStatus() + "', " + slot.capacity() + ", "
                + slot.remainingCount() + ")";
    }

    private static String renderRegistrationRow(RegistrationSeed registration) {
        return "    (" + registration.id() + ", '" + registration.orderNo() + "', " + registration.patientId() + ", "
                + registration.doctorId() + ", " + registration.departmentId() + ", " + registration.sessionId() + ", "
                + registration.slotId() + ", NULL, '" + registration.orderStatus() + "', " + decimal(registration.fee())
                + ", TIMESTAMPTZ '" + timestamp(registration.paidAt()) + "', TIMESTAMPTZ '"
                + timestamp(registration.createdAt()) + "', TIMESTAMPTZ '" + timestamp(registration.updatedAt()) + "')";
    }

    private static String renderEncounterRow(EncounterSeed encounter) {
        return "    (" + encounter.id() + ", " + encounter.orderId() + ", " + encounter.patientId() + ", "
                + encounter.doctorId() + ", " + encounter.departmentId() + ", '" + encounter.encounterStatus() + "', "
                + nullableTimestamp(encounter.startedAt()) + ", " + nullableTimestamp(encounter.endedAt()) + ", "
                + sqlString(encounter.summary()) + ", TIMESTAMPTZ '" + timestamp(encounter.createdAt())
                + "', TIMESTAMPTZ '" + timestamp(encounter.updatedAt()) + "')";
    }

    private static String renderEmrRecordRow(EmrSeed emr) {
        return "    (" + emr.id() + ", '" + emr.recordNo() + "', " + emr.encounterId() + ", " + emr.patientId() + ", "
                + emr.doctorId() + ", " + emr.departmentId() + ", '" + emr.recordStatus() + "', "
                + sqlString(emr.chiefComplaintSummary()) + ", TIMESTAMPTZ '" + timestamp(emr.createdAt())
                + "', TIMESTAMPTZ '" + timestamp(emr.updatedAt()) + "')";
    }

    private static String renderEmrContentRow(EmrSeed emr) {
        return "    (" + emr.id() + ", " + sqlString(emr.contentEncrypted()) + ", " + sqlString(emr.contentMasked())
                + ", '" + emr.contentHash() + "', TIMESTAMPTZ '" + timestamp(emr.createdAt()) + "', TIMESTAMPTZ '"
                + timestamp(emr.updatedAt()) + "')";
    }

    private static String renderDiagnosisRow(DiagnosisSeed diagnosis) {
        return "    (" + diagnosis.id() + ", " + diagnosis.recordId() + ", '" + diagnosis.diagnosisType() + "', "
                + nullableString(diagnosis.diagnosisCode()) + ", " + sqlString(diagnosis.diagnosisName()) + ", "
                + diagnosis.isPrimary() + ", " + diagnosis.sortOrder() + ", TIMESTAMPTZ '"
                + timestamp(diagnosis.createdAt()) + "')";
    }

    private static String renderPrescriptionRow(PrescriptionSeed prescription) {
        return "    (" + prescription.id() + ", '" + prescription.prescriptionNo() + "', " + prescription.recordId()
                + ", " + prescription.encounterId() + ", " + prescription.patientId() + ", " + prescription.doctorId()
                + ", '" + prescription.prescriptionStatus() + "', TIMESTAMPTZ '" + timestamp(prescription.createdAt())
                + "', TIMESTAMPTZ '" + timestamp(prescription.updatedAt()) + "')";
    }

    private static String renderPrescriptionItemRow(PrescriptionItemSeed item) {
        return "    (" + item.id() + ", " + item.prescriptionId() + ", " + item.sortOrder() + ", "
                + sqlString(item.drugName()) + ", " + nullableString(item.drugSpecification()) + ", "
                + nullableString(item.dosageText()) + ", " + nullableString(item.frequencyText()) + ", "
                + nullableString(item.durationText()) + ", " + item.quantity() + ", " + nullableString(item.unit())
                + ", " + nullableString(item.route()) + ", TIMESTAMPTZ '" + timestamp(item.createdAt()) + "')";
    }

    private static String decimal(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private static String timestamp(OffsetDateTime value) {
        return value.format(OFFSET_FORMATTER);
    }

    private static String nullableTimestamp(OffsetDateTime value) {
        return value == null ? "NULL" : "TIMESTAMPTZ '" + timestamp(value) + "'";
    }

    private static String sqlString(String value) {
        return "'" + value.replace("'", "''") + "'";
    }

    private static String nullableString(String value) {
        return value == null ? "NULL" : sqlString(value);
    }

    private static String encrypt(String seedLabel, String plainText, byte[] key) throws GeneralSecurityException {
        byte[] iv = deterministicIv(seedLabel, plainText);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
        byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        byte[] payload = new byte[iv.length + cipherText.length];
        System.arraycopy(iv, 0, payload, 0, iv.length);
        System.arraycopy(cipherText, 0, payload, iv.length, cipherText.length);
        return Base64.getEncoder().encodeToString(payload);
    }

    private static byte[] deterministicIv(String seedLabel, String plainText) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((seedLabel + "|" + plainText).getBytes(StandardCharsets.UTF_8));
            byte[] iv = new byte[12];
            System.arraycopy(hash, 0, iv, 0, iv.length);
            return iv;
        } catch (Exception exception) {
            throw new IllegalStateException("failed to derive deterministic iv", exception);
        }
    }

    private static String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("failed to compute sha256", exception);
        }
    }

    private record DoctorTemplate(
            long doctorId,
            long departmentId,
            String clinicType,
            String periodCode,
            double fee,
            LocalTime startTime,
            int slotDurationMinutes) {
    }

    private record SessionSeed(
            long id,
            long hospitalId,
            long departmentId,
            long doctorId,
            LocalDate sessionDate,
            String periodCode,
            String clinicType,
            String sessionStatus,
            int capacity,
            int remainingCount,
            double fee,
            String sourceType) {
    }

    private record SlotSeed(
            long id,
            long sessionId,
            int slotSeq,
            OffsetDateTime slotStartTime,
            OffsetDateTime slotEndTime,
            String slotStatus,
            int capacity,
            int remainingCount) {
    }

    private record RegistrationSeed(
            long id,
            String orderNo,
            long patientId,
            long doctorId,
            long departmentId,
            long sessionId,
            long slotId,
            String orderStatus,
            double fee,
            OffsetDateTime paidAt,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
    }

    private record EncounterSeed(
            long id,
            long orderId,
            long patientId,
            long doctorId,
            long departmentId,
            String encounterStatus,
            OffsetDateTime startedAt,
            OffsetDateTime endedAt,
            String summary,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
    }

    private record EmrSeed(
            long id,
            String recordNo,
            long encounterId,
            long patientId,
            long doctorId,
            long departmentId,
            String recordStatus,
            String chiefComplaintSummary,
            String contentEncrypted,
            String contentMasked,
            String contentHash,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            List<DiagnosisSeed> diagnoses) {
    }

    private record DiagnosisSeed(
            long id,
            String diagnosisType,
            String diagnosisCode,
            String diagnosisName,
            boolean isPrimary,
            int sortOrder,
            OffsetDateTime createdAt) {
        private long recordId() {
            return id == 9203L ? 9102L : 9101L;
        }
    }

    private record PrescriptionSeed(
            long id,
            String prescriptionNo,
            long recordId,
            long encounterId,
            long patientId,
            long doctorId,
            String prescriptionStatus,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            List<PrescriptionItemSeed> items) {
    }

    private record PrescriptionItemSeed(
            long id,
            int sortOrder,
            String drugName,
            String drugSpecification,
            String dosageText,
            String frequencyText,
            String durationText,
            String quantity,
            String unit,
            String route,
            OffsetDateTime createdAt) {
        private long prescriptionId() {
            return 9301L;
        }
    }

    @FunctionalInterface
    private interface RowRenderer<T> {
        String render(T value);
    }
}
