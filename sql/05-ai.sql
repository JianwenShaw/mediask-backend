CREATE TABLE ai_session (
    id BIGINT PRIMARY KEY,
    session_uuid VARCHAR(64) NOT NULL,
    patient_id BIGINT NOT NULL,
    department_id BIGINT,
    scene_type VARCHAR(32) NOT NULL,
    entrypoint VARCHAR(32) NOT NULL,
    session_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    chief_complaint_summary TEXT,
    summary TEXT,
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMPTZ,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ DEFAULT NULL,
    CONSTRAINT uk_ai_session_uuid UNIQUE (session_uuid),
    CONSTRAINT fk_ai_session_patient FOREIGN KEY (patient_id) REFERENCES users (id),
    CONSTRAINT fk_ai_session_department FOREIGN KEY (department_id) REFERENCES departments (id),
    CONSTRAINT ck_ai_session_scene_type CHECK (scene_type IN ('PRE_CONSULTATION', 'HEALTH_CONSULTATION', 'FOLLOW_UP')),
    CONSTRAINT ck_ai_session_entrypoint CHECK (entrypoint IN ('PATIENT_APP', 'DOCTOR_CONSOLE', 'ADMIN_TOOL')),
    CONSTRAINT ck_ai_session_status CHECK (session_status IN ('ACTIVE', 'CLOSED', 'FAILED'))
);

CREATE TABLE ai_turn (
    id BIGINT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    turn_no INT NOT NULL,
    turn_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    input_hash VARCHAR(128),
    output_hash VARCHAR(128),
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    error_code INT,
    error_message VARCHAR(255),
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ DEFAULT NULL,
    CONSTRAINT uk_ai_turn_session_no UNIQUE (session_id, turn_no),
    CONSTRAINT fk_ai_turn_session FOREIGN KEY (session_id) REFERENCES ai_session (id),
    CONSTRAINT ck_ai_turn_status CHECK (turn_status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'))
);

CREATE TABLE ai_turn_content (
    id BIGINT PRIMARY KEY,
    turn_id BIGINT NOT NULL,
    content_role VARCHAR(16) NOT NULL,
    content_encrypted TEXT NOT NULL,
    content_masked TEXT,
    content_hash VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_turn_content_turn FOREIGN KEY (turn_id) REFERENCES ai_turn (id),
    CONSTRAINT ck_ai_turn_content_role CHECK (content_role IN ('USER', 'ASSISTANT', 'SYSTEM'))
);

CREATE TABLE ai_model_run (
    id BIGINT PRIMARY KEY,
    turn_id BIGINT NOT NULL,
    provider_name VARCHAR(32) NOT NULL DEFAULT 'PYTHON_AI',
    provider_run_id VARCHAR(128),
    model_name VARCHAR(64),
    request_id VARCHAR(64) NOT NULL,
    rag_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    retrieval_provider VARCHAR(32),
    run_status VARCHAR(16) NOT NULL DEFAULT 'RUNNING',
    is_degraded BOOLEAN NOT NULL DEFAULT FALSE,
    tokens_input INT,
    tokens_output INT,
    latency_ms INT,
    triage_snapshot_json JSONB,
    request_payload_hash VARCHAR(128),
    response_payload_hash VARCHAR(128),
    error_code INT,
    error_message VARCHAR(255),
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ DEFAULT NULL,
    CONSTRAINT fk_ai_model_run_turn FOREIGN KEY (turn_id) REFERENCES ai_turn (id),
    CONSTRAINT ck_ai_model_run_status CHECK (run_status IN ('RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELLED'))
);

CREATE TABLE ai_guardrail_event (
    id BIGINT PRIMARY KEY,
    run_id BIGINT NOT NULL,
    risk_level VARCHAR(16) NOT NULL,
    action_taken VARCHAR(16) NOT NULL,
    matched_rule_codes JSONB NOT NULL DEFAULT '[]'::jsonb,
    input_hash VARCHAR(128),
    output_hash VARCHAR(128),
    event_detail_json JSONB,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_guardrail_event_run FOREIGN KEY (run_id) REFERENCES ai_model_run (id),
    CONSTRAINT ck_ai_guardrail_risk_level CHECK (risk_level IN ('low', 'medium', 'high')),
    CONSTRAINT ck_ai_guardrail_action CHECK (action_taken IN ('allow', 'caution', 'refuse'))
);

CREATE TABLE knowledge_base (
    id BIGINT PRIMARY KEY,
    kb_code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    owner_type VARCHAR(32) NOT NULL,
    owner_dept_id BIGINT,
    visibility VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
    embedding_model VARCHAR(64) NOT NULL DEFAULT 'text-embedding-v4',
    embedding_dim INT NOT NULL DEFAULT 1536,
    chunk_strategy_json JSONB,
    retrieval_strategy_json JSONB,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ DEFAULT NULL,
    CONSTRAINT uk_knowledge_base_code UNIQUE (kb_code),
    CONSTRAINT fk_knowledge_base_owner_dept FOREIGN KEY (owner_dept_id) REFERENCES departments (id),
    CONSTRAINT ck_knowledge_base_owner_type CHECK (owner_type IN ('SYSTEM', 'DEPARTMENT', 'TOPIC')),
    CONSTRAINT ck_knowledge_base_visibility CHECK (visibility IN ('PUBLIC', 'DEPT', 'PRIVATE')),
    CONSTRAINT ck_knowledge_base_status CHECK (status IN ('ENABLED', 'DISABLED', 'ARCHIVED')),
    CONSTRAINT ck_knowledge_base_embedding_dim CHECK (embedding_dim = 1536)
);

CREATE TABLE knowledge_document (
    id BIGINT PRIMARY KEY,
    knowledge_base_id BIGINT NOT NULL,
    document_uuid UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    source_uri TEXT,
    content_hash VARCHAR(128),
    language_code VARCHAR(16) NOT NULL DEFAULT 'zh-CN',
    version_no INT NOT NULL DEFAULT 1,
    document_status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    ingested_by_service VARCHAR(32) NOT NULL DEFAULT 'JAVA',
    published_at TIMESTAMPTZ,
    doc_metadata JSONB,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ DEFAULT NULL,
    CONSTRAINT uk_knowledge_document_uuid UNIQUE (document_uuid),
    CONSTRAINT fk_knowledge_document_base FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_base (id),
    CONSTRAINT ck_knowledge_document_source_type CHECK (source_type IN ('MARKDOWN', 'DOCX', 'PDF', 'MANUAL', 'WEB')),
    CONSTRAINT ck_knowledge_document_status CHECK (document_status IN ('DRAFT', 'UPLOADED', 'PARSING', 'CHUNKED', 'INDEXING', 'ACTIVE', 'FAILED', 'ARCHIVED'))
);

CREATE UNIQUE INDEX uk_knowledge_document_base_hash_active
    ON knowledge_document (knowledge_base_id, content_hash)
    WHERE deleted_at IS NULL AND content_hash IS NOT NULL;

CREATE TABLE knowledge_chunk (
    id BIGINT PRIMARY KEY,
    knowledge_base_id BIGINT NOT NULL,
    document_id BIGINT NOT NULL,
    chunk_index INT NOT NULL,
    section_title VARCHAR(255),
    page_no INT,
    char_start INT,
    char_end INT,
    token_count INT,
    content TEXT NOT NULL,
    content_preview TEXT,
    citation_label VARCHAR(255),
    chunk_metadata JSONB,
    chunk_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ DEFAULT NULL,
    CONSTRAINT uk_knowledge_chunk_doc_idx UNIQUE (document_id, chunk_index),
    CONSTRAINT fk_knowledge_chunk_base FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_base (id),
    CONSTRAINT fk_knowledge_chunk_document FOREIGN KEY (document_id) REFERENCES knowledge_document (id),
    CONSTRAINT ck_knowledge_chunk_status CHECK (chunk_status IN ('ACTIVE', 'INACTIVE', 'DELETED'))
);

CREATE TABLE knowledge_chunk_index (
    chunk_id BIGINT PRIMARY KEY,
    knowledge_base_id BIGINT NOT NULL,
    document_id BIGINT NOT NULL,
    embedding VECTOR(1536) NOT NULL,
    embedding_model VARCHAR(64) NOT NULL DEFAULT 'text-embedding-v4',
    embedding_version INT NOT NULL DEFAULT 1,
    search_text TEXT NOT NULL,
    search_lexemes TEXT NOT NULL,
    search_tsv TSVECTOR GENERATED ALWAYS AS (
        to_tsvector('simple', COALESCE(search_lexemes, ''))
    ) STORED,
    authority_score NUMERIC(6,3) NOT NULL DEFAULT 1.000,
    freshness_score NUMERIC(6,3) NOT NULL DEFAULT 1.000,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    indexed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_knowledge_chunk_index_chunk FOREIGN KEY (chunk_id) REFERENCES knowledge_chunk (id) ON DELETE CASCADE,
    CONSTRAINT fk_knowledge_chunk_index_base FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_base (id),
    CONSTRAINT fk_knowledge_chunk_index_document FOREIGN KEY (document_id) REFERENCES knowledge_document (id)
);

CREATE TABLE ai_run_citation (
    id BIGINT PRIMARY KEY,
    model_run_id BIGINT NOT NULL,
    chunk_id BIGINT NOT NULL,
    retrieval_rank INT NOT NULL,
    vector_score DOUBLE PRECISION,
    keyword_score DOUBLE PRECISION,
    fusion_score DOUBLE PRECISION,
    rerank_score DOUBLE PRECISION,
    used_in_answer BOOLEAN NOT NULL DEFAULT TRUE,
    snippet TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_ai_run_citation UNIQUE (model_run_id, chunk_id),
    CONSTRAINT fk_ai_run_citation_run FOREIGN KEY (model_run_id) REFERENCES ai_model_run (id),
    CONSTRAINT fk_ai_run_citation_chunk FOREIGN KEY (chunk_id) REFERENCES knowledge_chunk (id)
);

CREATE INDEX idx_ai_session_patient_created ON ai_session (patient_id, created_at);
CREATE INDEX idx_ai_turn_session_created ON ai_turn (session_id, created_at);
CREATE INDEX idx_ai_model_run_request ON ai_model_run (request_id);
CREATE INDEX idx_ai_model_run_turn_status ON ai_model_run (turn_id, run_status);
CREATE INDEX idx_ai_guardrail_run ON ai_guardrail_event (run_id, occurred_at);
CREATE INDEX idx_knowledge_document_base_status ON knowledge_document (knowledge_base_id, document_status);
CREATE INDEX idx_knowledge_chunk_base_status ON knowledge_chunk (knowledge_base_id, chunk_status);
CREATE INDEX idx_ai_run_citation_run_rank ON ai_run_citation (model_run_id, retrieval_rank);
CREATE INDEX idx_kci_vector_hnsw
    ON knowledge_chunk_index
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 128)
    WHERE is_active = TRUE;
CREATE INDEX idx_kci_search_tsv ON knowledge_chunk_index USING gin (search_tsv);
CREATE INDEX idx_kci_kb_active ON knowledge_chunk_index (knowledge_base_id, is_active);
CREATE INDEX idx_kci_doc_active ON knowledge_chunk_index (document_id, is_active);
