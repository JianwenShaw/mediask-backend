INSERT INTO roles (id, role_code, role_name, role_type, status, sort_order)
VALUES
    (1001, 'PATIENT', '患者', 'BUSINESS', 'ACTIVE', 10),
    (1002, 'DOCTOR', '医生', 'BUSINESS', 'ACTIVE', 20),
    (1003, 'ADMIN', '管理员', 'SYSTEM', 'ACTIVE', 30)
ON CONFLICT (role_code) DO NOTHING;

INSERT INTO permissions (id, permission_code, permission_name, permission_type, status, sort_order)
VALUES
    (1101, 'ai:chat', 'AI问诊', 'API', 'ACTIVE', 10),
    (1102, 'registration:create', '创建挂号', 'API', 'ACTIVE', 20),
    (1103, 'encounter:view', '查看接诊', 'API', 'ACTIVE', 30),
    (1104, 'emr:view', '查看病历', 'API', 'ACTIVE', 40),
    (1105, 'audit:view', '查看审计', 'API', 'ACTIVE', 50),
    (1106, 'auth:refresh', '刷新令牌', 'API', 'ACTIVE', 60),
    (1108, 'patient:profile:view:self', '查看本人患者资料', 'API', 'ACTIVE', 80),
    (1109, 'doctor:profile:view:self', '查看本人医生资料', 'API', 'ACTIVE', 90),
    (1110, 'patient:profile:update:self', '更新本人患者资料', 'API', 'ACTIVE', 100),
    (1111, 'doctor:profile:update:self', '更新本人医生资料', 'API', 'ACTIVE', 110),
    (1112, 'admin:patient:list', '后台患者列表', 'API', 'ACTIVE', 120),
    (1113, 'admin:patient:view', '后台患者详情', 'API', 'ACTIVE', 130),
    (1114, 'admin:patient:create', '后台新增患者', 'API', 'ACTIVE', 140),
    (1115, 'admin:patient:update', '后台修改患者', 'API', 'ACTIVE', 150),
    (1116, 'admin:patient:delete', '后台删除患者', 'API', 'ACTIVE', 160),
    (1117, 'admin:knowledge:document:import', '后台导入知识文档', 'API', 'ACTIVE', 170),
    (1118, 'admin:knowledge:base:list', '后台知识库列表', 'API', 'ACTIVE', 180),
    (1119, 'admin:knowledge:base:create', '后台新增知识库', 'API', 'ACTIVE', 190),
    (1120, 'admin:knowledge:base:update', '后台修改知识库', 'API', 'ACTIVE', 200),
    (1121, 'admin:knowledge:base:delete', '后台删除知识库', 'API', 'ACTIVE', 210),
    (1122, 'admin:knowledge:document:list', '后台知识文档列表', 'API', 'ACTIVE', 220),
    (1123, 'admin:knowledge:document:delete', '后台删除知识文档', 'API', 'ACTIVE', 230)
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO role_permissions (id, role_id, permission_id)
VALUES
    (1201, 1001, 1101),
    (1202, 1001, 1102),
    (1203, 1002, 1103),
    (1204, 1002, 1104),
    (1205, 1003, 1105),
    (1206, 1001, 1106),
    (1207, 1002, 1106),
    (1208, 1003, 1106),
    (1212, 1001, 1108),
    (1213, 1002, 1109),
    (1214, 1001, 1110),
    (1215, 1002, 1111),
    (1216, 1003, 1112),
    (1217, 1003, 1113),
    (1218, 1003, 1114),
    (1219, 1003, 1115),
    (1220, 1003, 1116),
    (1221, 1003, 1117),
    (1222, 1003, 1118),
    (1223, 1003, 1119),
    (1224, 1003, 1120),
    (1225, 1003, 1121),
    (1226, 1003, 1122),
    (1227, 1003, 1123)
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- admin / admin123
-- doctor_zhang / doctor123
-- patient_li / patient123
INSERT INTO users (id, username, password_hash, display_name, mobile_masked, user_type, account_status)
VALUES
    (2001, 'admin', '$2y$10$XMYeGErQIJ3Wg5Bzlj9B5.lhFhj3JBe1.YZ4j1OxihiNsHL4rIh8e', '系统管理员', '138****0000', 'ADMIN', 'ACTIVE'),
    (2002, 'doctor_zhang', '$2y$10$/0Am2Gpefs87WmCYex4Jr.RNGSOKaVsaLz4qmiB8zKOhKBgz.KUUy', '张医生', '139****0001', 'DOCTOR', 'ACTIVE'),
    (2003, 'patient_li', '$2y$10$xm4nTR2YtFBj9ZWiHbazOu3yq0JedJArylIn5HmdaP7FlLegb8WQe', '李患者', '137****0002', 'PATIENT', 'ACTIVE')
ON CONFLICT (username) DO NOTHING;

INSERT INTO user_roles (id, user_id, role_id, granted_by)
VALUES
    (2101, 2001, 1003, 2001),
    (2102, 2002, 1002, 2001),
    (2103, 2003, 1001, 2001)
ON CONFLICT (user_id, role_id) DO NOTHING;

INSERT INTO user_pii_profile (user_id, real_name_encrypted, phone_encrypted, id_no_encrypted, email_encrypted)
VALUES
    (2001, 'enc_admin_name', 'enc_admin_phone', 'enc_admin_id', 'enc_admin_mail'),
    (2002, 'enc_doctor_name', 'enc_doctor_phone', 'enc_doctor_id', 'enc_doctor_mail'),
    (2003, 'enc_patient_name', 'enc_patient_phone', 'enc_patient_id', 'enc_patient_mail')
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO patient_profile (id, user_id, patient_no, gender)
VALUES
    (2201, 2003, 'P20260001', 'FEMALE')
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO hospitals (id, hospital_code, name, hospital_level, status)
VALUES
    (3001, 'HOSP_MAIN', 'MediAsk Teaching Hospital', '3A', 'ACTIVE')
ON CONFLICT (hospital_code) DO NOTHING;

INSERT INTO departments (id, hospital_id, dept_code, name, dept_type, status, sort_order)
VALUES
    (3101, 3001, 'NEURO', '神经内科', 'CLINICAL', 'ACTIVE', 10),
    (3102, 3001, 'FEVER', '发热门诊', 'CLINICAL', 'ACTIVE', 20),
    (3103, 3001, 'GENMED', '普通内科', 'CLINICAL', 'ACTIVE', 30)
ON CONFLICT (hospital_id, dept_code) DO NOTHING;

INSERT INTO doctors (id, user_id, hospital_id, doctor_code, professional_title, introduction_masked, status)
VALUES
    (3201, 2002, 3001, 'DOC_ZHANG', 'ATTENDING', '擅长常见内科问题接诊', 'ACTIVE')
ON CONFLICT (doctor_code) DO NOTHING;

INSERT INTO doctor_department_rel (id, doctor_id, department_id, is_primary, relation_status)
VALUES
    (3301, 3201, 3103, TRUE, 'ACTIVE')
ON CONFLICT (doctor_id, department_id) DO NOTHING;

INSERT INTO data_scope_rules (id, role_id, resource_type, scope_type, status)
VALUES
    (3401, 1001, 'EMR_RECORD', 'SELF', 'ACTIVE'),
    (3402, 1002, 'EMR_RECORD', 'DEPARTMENT', 'ACTIVE'),
    (3403, 1003, 'AUDIT_EVENT', 'ALL', 'ACTIVE')
ON CONFLICT DO NOTHING;

INSERT INTO knowledge_base (id, kb_code, name, owner_type, visibility, status)
VALUES
    (4001, 'KB_SYSTEM_TRIAGE', '系统导诊知识库', 'SYSTEM', 'PUBLIC', 'ENABLED')
ON CONFLICT (kb_code) DO NOTHING;

INSERT INTO clinic_session (
    id,
    hospital_id,
    department_id,
    doctor_id,
    session_date,
    period_code,
    clinic_type,
    session_status,
    capacity,
    remaining_count,
    fee,
    source_type
)
VALUES
    (4101, 3001, 3103, 3201, DATE '2026-05-20', 'MORNING', 'GENERAL', 'OPEN', 2, 2, 25.00, 'MANUAL'),
    (4102, 3001, 3103, 3201, DATE '2026-05-20', 'AFTERNOON', 'GENERAL', 'PUBLISHED', 1, 1, 25.00, 'MANUAL')
ON CONFLICT (id) DO NOTHING;

INSERT INTO clinic_slot (
    id,
    session_id,
    slot_seq,
    slot_start_time,
    slot_end_time,
    slot_status,
    capacity,
    remaining_count
)
VALUES
    (5101, 4101, 1, TIMESTAMPTZ '2026-05-20 09:00:00+08', TIMESTAMPTZ '2026-05-20 09:15:00+08', 'AVAILABLE', 1, 1),
    (5102, 4101, 2, TIMESTAMPTZ '2026-05-20 09:15:00+08', TIMESTAMPTZ '2026-05-20 09:30:00+08', 'AVAILABLE', 1, 1),
    (5103, 4102, 1, TIMESTAMPTZ '2026-05-20 14:00:00+08', TIMESTAMPTZ '2026-05-20 14:15:00+08', 'AVAILABLE', 1, 1)
ON CONFLICT (id) DO NOTHING;
