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
    (4101, 3001, 3103, 3201, CURRENT_DATE + 1, 'MORNING', 'GENERAL', 'OPEN', 2, 2, 25.00, 'MANUAL'),
    (4102, 3001, 3103, 3201, CURRENT_DATE + 1, 'AFTERNOON', 'GENERAL', 'OPEN', 2, 1, 25.00, 'MANUAL'),
    (4103, 3001, 3101, 3202, CURRENT_DATE + 2, 'MORNING', 'SPECIALIST', 'OPEN', 2, 2, 40.00, 'MANUAL'),
    (4104, 3001, 3102, 3203, CURRENT_DATE + 2, 'AFTERNOON', 'GENERAL', 'OPEN', 1, 1, 30.00, 'MANUAL'),
    (4111, 3001, 3103, 3201, CURRENT_DATE - 5, 'MORNING', 'GENERAL', 'CLOSED', 1, 0, 25.00, 'MANUAL'),
    (4112, 3001, 3101, 3202, CURRENT_DATE, 'AFTERNOON', 'SPECIALIST', 'CLOSED', 1, 0, 40.00, 'MANUAL'),
    (4113, 3001, 3102, 3203, CURRENT_DATE - 1, 'AFTERNOON', 'GENERAL', 'CLOSED', 1, 0, 30.00, 'MANUAL')
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
    (5101, 4101, 1, ((CURRENT_DATE + 1)::timestamp + TIME '09:00') AT TIME ZONE 'Asia/Shanghai', ((CURRENT_DATE + 1)::timestamp + TIME '09:15') AT TIME ZONE 'Asia/Shanghai', 'AVAILABLE', 1, 1),
    (5102, 4101, 2, ((CURRENT_DATE + 1)::timestamp + TIME '09:15') AT TIME ZONE 'Asia/Shanghai', ((CURRENT_DATE + 1)::timestamp + TIME '09:30') AT TIME ZONE 'Asia/Shanghai', 'AVAILABLE', 1, 1),
    (5103, 4102, 1, ((CURRENT_DATE + 1)::timestamp + TIME '14:00') AT TIME ZONE 'Asia/Shanghai', ((CURRENT_DATE + 1)::timestamp + TIME '14:15') AT TIME ZONE 'Asia/Shanghai', 'BOOKED', 1, 0),
    (5104, 4102, 2, ((CURRENT_DATE + 1)::timestamp + TIME '14:15') AT TIME ZONE 'Asia/Shanghai', ((CURRENT_DATE + 1)::timestamp + TIME '14:30') AT TIME ZONE 'Asia/Shanghai', 'AVAILABLE', 1, 1),
    (5105, 4103, 1, ((CURRENT_DATE + 2)::timestamp + TIME '09:00') AT TIME ZONE 'Asia/Shanghai', ((CURRENT_DATE + 2)::timestamp + TIME '09:20') AT TIME ZONE 'Asia/Shanghai', 'AVAILABLE', 1, 1),
    (5106, 4103, 2, ((CURRENT_DATE + 2)::timestamp + TIME '09:20') AT TIME ZONE 'Asia/Shanghai', ((CURRENT_DATE + 2)::timestamp + TIME '09:40') AT TIME ZONE 'Asia/Shanghai', 'AVAILABLE', 1, 1),
    (5107, 4104, 1, ((CURRENT_DATE + 2)::timestamp + TIME '15:00') AT TIME ZONE 'Asia/Shanghai', ((CURRENT_DATE + 2)::timestamp + TIME '15:20') AT TIME ZONE 'Asia/Shanghai', 'AVAILABLE', 1, 1),
    (5111, 4111, 1, ((CURRENT_DATE - 5)::timestamp + TIME '09:00') AT TIME ZONE 'Asia/Shanghai', ((CURRENT_DATE - 5)::timestamp + TIME '09:15') AT TIME ZONE 'Asia/Shanghai', 'BOOKED', 1, 0),
    (5112, 4112, 1, (CURRENT_DATE::timestamp + TIME '14:00') AT TIME ZONE 'Asia/Shanghai', (CURRENT_DATE::timestamp + TIME '14:20') AT TIME ZONE 'Asia/Shanghai', 'BOOKED', 1, 0),
    (5113, 4113, 1, ((CURRENT_DATE - 1)::timestamp + TIME '15:00') AT TIME ZONE 'Asia/Shanghai', ((CURRENT_DATE - 1)::timestamp + TIME '15:20') AT TIME ZONE 'Asia/Shanghai', 'BOOKED', 1, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO registration_order (
    id,
    order_no,
    patient_id,
    doctor_id,
    department_id,
    session_id,
    slot_id,
    source_ai_session_id,
    order_status,
    fee,
    paid_at,
    created_at,
    updated_at
)
VALUES
    (
        6101,
        'REG202604190001',
        2003,
        3201,
        3103,
        4102,
        5103,
        NULL,
        'CONFIRMED',
        25.00,
        CURRENT_TIMESTAMP - INTERVAL '3 hour',
        CURRENT_TIMESTAMP - INTERVAL '3 hour',
        CURRENT_TIMESTAMP - INTERVAL '3 hour'
    ),
    (
        6102,
        'REG202604140001',
        2006,
        3201,
        3103,
        4111,
        5111,
        NULL,
        'COMPLETED',
        25.00,
        CURRENT_TIMESTAMP - INTERVAL '5 day' - INTERVAL '6 hour',
        CURRENT_TIMESTAMP - INTERVAL '5 day' - INTERVAL '6 hour',
        CURRENT_TIMESTAMP - INTERVAL '5 day' - INTERVAL '1 hour'
    ),
    (
        6103,
        'REG202604190002',
        2007,
        3202,
        3101,
        4112,
        5112,
        NULL,
        'CONFIRMED',
        40.00,
        CURRENT_TIMESTAMP - INTERVAL '2 hour',
        CURRENT_TIMESTAMP - INTERVAL '2 hour',
        CURRENT_TIMESTAMP - INTERVAL '30 minute'
    ),
    (
        6104,
        'REG202604180001',
        2003,
        3203,
        3102,
        4113,
        5113,
        NULL,
        'COMPLETED',
        30.00,
        CURRENT_TIMESTAMP - INTERVAL '1 day' - INTERVAL '5 hour',
        CURRENT_TIMESTAMP - INTERVAL '1 day' - INTERVAL '5 hour',
        CURRENT_TIMESTAMP - INTERVAL '1 day'
    )
ON CONFLICT (order_no) DO NOTHING;

INSERT INTO visit_encounter (
    id,
    order_id,
    patient_id,
    doctor_id,
    department_id,
    encounter_status,
    started_at,
    ended_at,
    summary,
    created_at,
    updated_at
)
VALUES
    (
        8101,
        6101,
        2003,
        3201,
        3103,
        'SCHEDULED',
        NULL,
        NULL,
        '已完成预问诊，待医生正式接诊并录入病历。',
        CURRENT_TIMESTAMP - INTERVAL '3 hour',
        CURRENT_TIMESTAMP - INTERVAL '3 hour'
    ),
    (
        8102,
        6102,
        2006,
        3201,
        3103,
        'COMPLETED',
        CURRENT_TIMESTAMP - INTERVAL '5 day' - INTERVAL '5 hour',
        CURRENT_TIMESTAMP - INTERVAL '5 day' - INTERVAL '4 hour',
        '发热伴咽痛，线下完成问诊和用药建议。',
        CURRENT_TIMESTAMP - INTERVAL '5 day' - INTERVAL '6 hour',
        CURRENT_TIMESTAMP - INTERVAL '5 day' - INTERVAL '4 hour'
    ),
    (
        8103,
        6103,
        2007,
        3202,
        3101,
        'IN_PROGRESS',
        CURRENT_TIMESTAMP - INTERVAL '20 minute',
        NULL,
        '患者已进入诊室，医生正在查看症状与既往史。',
        CURRENT_TIMESTAMP - INTERVAL '2 hour',
        CURRENT_TIMESTAMP - INTERVAL '20 minute'
    ),
    (
        8104,
        6104,
        2003,
        3203,
        3102,
        'COMPLETED',
        CURRENT_TIMESTAMP - INTERVAL '1 day' - INTERVAL '4 hour',
        CURRENT_TIMESTAMP - INTERVAL '1 day' - INTERVAL '3 hour' - INTERVAL '30 minute',
        '发热症状已完成线下处置，建议复诊时复核体温变化。',
        CURRENT_TIMESTAMP - INTERVAL '1 day' - INTERVAL '5 hour',
        CURRENT_TIMESTAMP - INTERVAL '1 day' - INTERVAL '3 hour' - INTERVAL '30 minute'
    )
ON CONFLICT (order_id) DO NOTHING;
