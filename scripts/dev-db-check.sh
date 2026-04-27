#!/bin/sh

set -eu

echo "[mediask] checking PostgreSQL extension, schemas, tables, and seed data"

docker compose exec -T postgres psql -U mediask -d mediask_dev -v ON_ERROR_STOP=1 <<'SQL'
\echo '[mediask] installed extensions'
\dx

\echo '[mediask] available schemas'
\dn

\echo '[mediask] RAG tables'
\dt public.knowledge_*

\echo '[mediask] seed verification'
SELECT id, code, status
FROM knowledge_base
WHERE id = '90000000-0000-0000-0000-000000000001';

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'vector') THEN
        RAISE EXCEPTION 'vector extension is missing';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.schemata WHERE schema_name = 'audit') THEN
        RAISE EXCEPTION 'audit schema is missing';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.schemata WHERE schema_name = 'event') THEN
        RAISE EXCEPTION 'event schema is missing';
    END IF;
    IF to_regclass('public.knowledge_base') IS NULL THEN
        RAISE EXCEPTION 'knowledge_base table is missing';
    END IF;
    IF NOT EXISTS (
        SELECT 1
        FROM knowledge_base
        WHERE id = '90000000-0000-0000-0000-000000000001'
    ) THEN
        RAISE EXCEPTION 'default RAG seed data is missing';
    END IF;
END $$;
SQL
