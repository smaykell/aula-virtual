-- V21__add_assignment_reminder.sql
--
-- Cuando se avisó a los estudiantes de que la tarea vence pronto. Nulo mientras no se
-- haya avisado; cambiar la fecha de vencimiento lo vuelve a dejar en nulo.

ALTER TABLE assignments ADD COLUMN reminded_at TIMESTAMPTZ;

CREATE INDEX idx_assignments_due_at_pending_reminder ON assignments (due_at)
    WHERE reminded_at IS NULL;
