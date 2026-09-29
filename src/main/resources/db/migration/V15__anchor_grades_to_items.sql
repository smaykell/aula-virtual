-- V15__anchor_grades_to_items.sql
--
-- La nota deja de colgar de la entrega y pasa a colgar de la tarea y del
-- estudiante: asi se califica tambien a quien no entrego. max_score se copia
-- al calificar para que cambiar el puntaje de la tarea no altere notas ya
-- puestas. La tarea guarda su curso para que el registro de notas la
-- encuentre sin pasar por las unidades.

ALTER TABLE assignments ADD COLUMN course_id UUID;

UPDATE assignments a
SET course_id = u.course_id
FROM units u
WHERE u.id = a.unit_id;

ALTER TABLE assignments ALTER COLUMN course_id SET NOT NULL;
ALTER TABLE assignments
    ADD CONSTRAINT fk_assignments_course_id FOREIGN KEY (course_id) REFERENCES courses (id);

CREATE INDEX idx_assignments_course_id ON assignments (course_id, due_at);

ALTER TABLE grades ADD COLUMN max_score NUMERIC(5, 2);

UPDATE grades g
SET source_id = s.assignment_id,
    max_score = a.max_score
FROM submissions s
JOIN assignments a ON a.id = s.assignment_id
WHERE g.source_type = 'ASSIGNMENT'
  AND g.source_id = s.id;

ALTER TABLE grades ALTER COLUMN max_score SET NOT NULL;
ALTER TABLE grades DROP CONSTRAINT uk_grades_source;
ALTER TABLE grades
    ADD CONSTRAINT uk_grades_source_student UNIQUE (source_type, source_id, student_id);
ALTER TABLE grades ADD CONSTRAINT ck_grades_max_score CHECK (max_score > 0);
ALTER TABLE grades ADD CONSTRAINT ck_grades_score_within_max CHECK (score <= max_score);
