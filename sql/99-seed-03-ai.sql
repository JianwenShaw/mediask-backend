INSERT INTO knowledge_base (id, kb_code, name, owner_type, visibility, status)
VALUES
    (4001, 'KB_SYSTEM_TRIAGE', '系统导诊知识库', 'SYSTEM', 'PUBLIC', 'ENABLED')
ON CONFLICT (kb_code) DO NOTHING;
