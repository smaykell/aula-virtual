-- V16__add_grade_return.sql
--
-- Una nota nace en borrador (returned_at nulo) y el estudiante solo la ve
-- cuando el docente se la devuelve. Las notas que ya existian se veian desde
-- el momento de ponerlas, asi que se dan por devueltas entonces.

ALTER TABLE grades ADD COLUMN returned_at TIMESTAMPTZ;

UPDATE grades SET returned_at = graded_at;
