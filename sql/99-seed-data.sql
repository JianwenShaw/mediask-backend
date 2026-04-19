\echo '[mediask] seed auth and permissions'
\ir 99-seed-01-auth.sql

\echo '[mediask] seed hospital org and staff'
\ir 99-seed-02-org.sql

\echo '[mediask] seed ai summary fixtures'
\ir 99-seed-03-ai.sql

\echo '[mediask] seed outpatient and encounter fixtures'
\ir 99-seed-04-outpatient.sql
