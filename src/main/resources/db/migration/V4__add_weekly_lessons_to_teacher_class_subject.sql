-- =====================================================================
-- V4__add_weekly_lessons_to_teacher_class_subject.sql
-- Adiciona a quantidade de aulas semanais (weekly_lessons) em teacher_class_subject
-- =====================================================================

ALTER TABLE teacher_class_subject
    ADD COLUMN weekly_lessons integer NOT NULL DEFAULT 2;

ALTER TABLE teacher_class_subject
    ADD CONSTRAINT teacher_class_subject_weekly_lessons_check
    CHECK (weekly_lessons >= 1 AND weekly_lessons <= 4);
