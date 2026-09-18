GRANT ALL ON SCHEMA public TO :"app_user";

SELECT format('ALTER TABLE public.%I OWNER TO %I', tablename, :'app_user')
FROM pg_tables
WHERE schemaname = 'public' AND tableowner <> :'app_user'
\gexec

SELECT format('ALTER SEQUENCE public.%I OWNER TO %I', sequencename, :'app_user')
FROM pg_sequences
WHERE schemaname = 'public' AND sequenceowner <> :'app_user'
\gexec

SELECT format('ALTER VIEW public.%I OWNER TO %I', viewname, :'app_user')
FROM pg_views
WHERE schemaname = 'public' AND viewowner <> :'app_user'
\gexec
