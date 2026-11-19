-- =====================================================================
-- V9__audit_immutability_and_constraints.sql
-- Itens 1.1, 1.4, 1.10: Unicidade de ano letivo ativo, slot único na grade
-- e preservação do histórico de auditoria ao excluir notas/frequências.
-- =====================================================================

-- 1. Item 1.1: Garante no máximo um ano letivo ativo por vez no banco
CREATE UNIQUE INDEX IF NOT EXISTS uq_academic_years_single_active
    ON academic_years (active)
    WHERE active = TRUE;

-- 2. Item 1.10: Garante unicidade de horário por turma, dia e ordem do slot
ALTER TABLE timetable_slots
    DROP CONSTRAINT IF EXISTS uq_timetable_classroom_day_slot,
    ADD CONSTRAINT uq_timetable_classroom_day_slot
        UNIQUE (classroom_id, day_of_week, slot_order);

-- 3. Item 1.4: Torna FKs de auditoria anuláveis com ON DELETE SET NULL
ALTER TABLE grade_history
    ALTER COLUMN grade_id DROP NOT NULL,
    DROP CONSTRAINT IF EXISTS fk45ak73yeaf9nls14n95lxt152,
    ADD CONSTRAINT fk45ak73yeaf9nls14n95lxt152
        FOREIGN KEY (grade_id) REFERENCES grades(id) ON DELETE SET NULL;

ALTER TABLE attendance_history
    ALTER COLUMN attendance_id DROP NOT NULL,
    DROP CONSTRAINT IF EXISTS fki5389e1a64knk452xs0vslyjy,
    ADD CONSTRAINT fki5389e1a64knk452xs0vslyjy
        FOREIGN KEY (attendance_id) REFERENCES attendances(id) ON DELETE SET NULL;
