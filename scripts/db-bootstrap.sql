\set ON_ERROR_STOP on

SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'app_user', :'app_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'app_user')
\gexec

SELECT format('ALTER ROLE %I WITH LOGIN PASSWORD %L', :'app_user', :'app_password')
\gexec

SELECT format('CREATE DATABASE %I OWNER %I', t.name, :'app_user')
FROM (VALUES (:'app_db'), (:'test_db')) AS t(name)
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = t.name)
\gexec

SELECT format('ALTER DATABASE %I OWNER TO %I', t.name, :'app_user')
FROM (VALUES (:'app_db'), (:'test_db')) AS t(name)
\gexec

\connect :app_db
\ir db-adopt-objects.sql

\connect :test_db
\ir db-adopt-objects.sql

SELECT datname AS base, pg_get_userbyid(datdba) AS propietario
FROM pg_database
WHERE datname IN (:'app_db', :'test_db')
ORDER BY datname;
