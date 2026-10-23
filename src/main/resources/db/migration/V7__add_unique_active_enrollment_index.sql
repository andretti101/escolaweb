-- Garante que um aluno possua no máximo uma matrícula ativa por vez no banco de dados
CREATE UNIQUE INDEX IF NOT EXISTS uq_enrollments_active_student
    ON enrollments (student_id)
    WHERE active = true;
