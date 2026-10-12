-- =====================================================================
-- EscolaWeb - SCRIPT DE SEED (dados de demonstração / desenvolvimento)
-- Banco: PostgreSQL 16
-- =====================================================================
-- OBJETIVO
--   Popular o banco com um cenário completo e coerente para testes e
--   demonstração do sistema, cobrindo todas as entidades principais:
--   usuários, calendário letivo, turmas, matrículas, aulas, presenças,
--   avaliações, notas, avisos e chat.
--
-- CENÁRIO
--   * 3 anos letivos: 2024 e 2025 ENCERRADOS (active = false e todos os
--     períodos closed = true) e 2026 ATIVO (active = true).
--   * Período letivo: TRIMESTRE (3 períodos por ano).
--   * Em 2026 o 1º e o 2º trimestres estão encerrados e o 3º está em
--     andamento. A data de referência do seed é 06/10/2026: não existem
--     aulas nem notas posteriores a essa data.
--   * 30 alunos divididos em 3 coortes de 10. Cada coorte avança uma série
--     por ano (ex.: coorte A cursa 8º ano em 2024, 9º ano em 2025 e
--     1º ano do Ensino Médio em 2026).
--   * 8 professores (um por disciplina), 1 diretora e 1 secretário.
--
-- ACESSO
--   Senha de TODOS os usuários: senha123
--   (armazenada como hash BCrypt, prefixo $2a$, custo 10, compatível com
--    o BCryptPasswordEncoder do Spring Security).
--   Logins de exemplo:
--     Diretora   : direcao@example.com
--     Secretário : secretaria@example.com
--     Professora : mariana.ribeiro@example.com  (Língua Portuguesa)
--     Aluno      : lucas.almeida@example.com
--
-- OBSERVAÇÃO
--   O script pode ser executado várias vezes: a seção 0 apaga todos os
--   dados existentes e reinicia os contadores de ID antes de inserir.
--   ATENÇÃO: isso remove TODOS os dados das tabelas do sistema.
-- =====================================================================

SET client_encoding = 'UTF8';

BEGIN; -- tudo é executado em uma única transação: ou entra tudo, ou nada.

-- =====================================================================
-- 0. LIMPEZA DOS DADOS
-- =====================================================================
-- TRUNCATE ... RESTART IDENTITY zera as tabelas e reinicia as sequences de
-- ID em 1, garantindo que o seed gere sempre os mesmos IDs. A ordem de
-- listagem não importa, pois todas as tabelas relacionadas são limpas na
-- mesma instrução (o CASCADE cobre as chaves estrangeiras entre elas).
TRUNCATE TABLE
    -- Histórico e dados acadêmicos (tabelas "folha", que dependem de outras)
    grade_history, grades, attendance_history, attendances,
    lessons, assessments, teacher_class_subject, enrollments,
    -- Avisos e chat
    announcement_classrooms, chat_hidden_messages, chat_read_receipts,
    chat_messages, chat_message_history, announcements,
    -- Estrutura escolar
    class_rooms, subjects, academic_periods, academic_years, school_settings,
    -- Perfis de usuário (herança JOINED: cada perfil tem sua tabela)
    students, teachers, secretaries, principals,
    -- Tokens de autenticação
    refresh_tokens, password_reset_tokens,
    -- Tabela base de usuários
    users
RESTART IDENTITY CASCADE;

-- =====================================================================
-- 1. CONFIGURAÇÕES DA ESCOLA
-- =====================================================================
-- Registro único com os parâmetros globais: frequência mínima exigida (%),
-- média mínima para aprovação e o tipo de período letivo (TRIMESTER).
INSERT INTO school_settings (id, address, created_at, email, minimum_attendance, minimum_average, period_type, phone, school_name)
VALUES (1, 'Rua das Acácias, 120 - Centro - Criciúma/SC', '2024-01-15 09:00:00', 'contato@escolamodelo.example.com',
        75.00, 6.0, 'TRIMESTER', '(48) 3433-0000', 'Escola Modelo EscolaWeb');

-- =====================================================================
-- 2. CALENDÁRIO LETIVO (anos e períodos)
-- =====================================================================
-- Cada ano letivo guarda suas próprias regras (frequência mínima, nota
-- mínima e tipo de período). O campo "active" indica o ano em vigor:
-- apenas 2026 está ativo; 2024 e 2025 estão encerrados (active = false).
INSERT INTO academic_years (id, active, created_at, end_date, start_date, year, minimum_attendance, minimum_grade, period_type) VALUES
 (1, false, '2024-01-15 09:00:00', '2024-12-13', '2024-02-05', 2024, 75.00, 6.00, 'TRIMESTER'),
 (2, false, '2025-01-15 09:00:00', '2025-12-12', '2025-02-10', 2025, 75.00, 6.00, 'TRIMESTER'),
 (3, true,  '2026-01-15 09:00:00', '2026-12-11', '2026-02-09', 2026, 75.00, 6.00, 'TRIMESTER');

-- Períodos (trimestres). "closed = true" significa período encerrado
-- (notas e frequências fechadas). Nos anos de 2024 e 2025 todos os
-- trimestres estão fechados. Em 2026: 1º e 2º fechados, 3º aberto
-- (em andamento na data de referência, 06/10/2026).
INSERT INTO academic_periods (id, closed, created_at, end_date, name, start_date, academic_year_id) VALUES
 (1, true, '2024-01-15 09:00:00', '2024-05-10', '1º Trimestre', '2024-02-05', 1),
 (2, true, '2024-01-15 09:00:00', '2024-08-30', '2º Trimestre', '2024-05-13', 1),
 (3, true, '2024-01-15 09:00:00', '2024-12-13', '3º Trimestre', '2024-09-02', 1),
 (4, true, '2025-01-15 09:00:00', '2025-05-09', '1º Trimestre', '2025-02-10', 2),
 (5, true, '2025-01-15 09:00:00', '2025-08-29', '2º Trimestre', '2025-05-12', 2),
 (6, true, '2025-01-15 09:00:00', '2025-12-12', '3º Trimestre', '2025-09-01', 2),
 (7, true, '2026-01-15 09:00:00', '2026-05-08', '1º Trimestre', '2026-02-09', 3),
 (8, true, '2026-01-15 09:00:00', '2026-08-28', '2º Trimestre', '2026-05-11', 3),
 (9, false, '2026-01-15 09:00:00', '2026-12-11', '3º Trimestre', '2026-08-31', 3);

-- =====================================================================
-- 3. USUÁRIOS E PERFIS
-- =====================================================================
-- O sistema usa herança JOINED: a tabela "users" guarda os dados comuns
-- (e-mail, nome, senha, perfil) e cada tipo de usuário tem sua própria
-- tabela com o mesmo ID (principals, secretaries, teachers, students).
-- IDs: 1 = diretora | 2 = secretário | 3 a 10 = professores | 11 a 40 = alunos.
-- A senha de todos é 'senha123' (hash BCrypt).
-- Tabela base (last_read_announcements = última vez que o usuário abriu os avisos; NULL = nunca leu).
INSERT INTO users (id, role, active, created_at, email, last_read_announcements, name, password, updated_at) VALUES
 (1, 'PRINCIPAL', true, '2024-01-10 08:00:00', 'direcao@example.com', '2026-10-05 17:00:00', 'Helena Cristina Vieira', '$2a$10$F9WLGJ8qCxZh0chJ4agUt.HIGokXts.Q6i3orhM124txzuSZb8/ES', '2024-01-10 08:00:00'),
 (2, 'SECRETARY', true, '2024-01-10 08:05:00', 'secretaria@example.com', '2026-10-05 17:30:00', 'Ricardo Nogueira Santos', '$2a$10$F9WLGJ8qCxZh0chJ4agUt.HIGokXts.Q6i3orhM124txzuSZb8/ES', '2024-01-10 08:05:00'),
 (3, 'TEACHER', true, '2024-01-10 08:10:00', 'mariana.ribeiro@example.com', '2026-10-02 09:00:00', 'Mariana Costa Ribeiro', '$2a$10$F9WLGJ8qCxZh0chJ4agUt.HIGokXts.Q6i3orhM124txzuSZb8/ES', '2024-01-10 08:10:00'),
 (4, 'TEACHER', true, '2024-01-10 08:10:00', 'carlos.menezes@example.com', '2026-09-30 14:00:00', 'Carlos Eduardo Menezes', '$2a$10$F9WLGJ8qCxZh0chJ4agUt.HIGokXts.Q6i3orhM124txzuSZb8/ES', '2024-01-10 08:10:00'),
 (5, 'TEACHER', true, '2024-01-10 08:10:00', 'patricia.andrade@example.com', NULL, 'Patrícia Lima Andrade', '$2a$10$F9WLGJ8qCxZh0chJ4agUt.HIGokXts.Q6i3orhM124txzuSZb8/ES', '2024-01-10 08:10:00'),
 (6, 'TEACHER', true, '2024-01-10 08:10:00', 'roberto.teixeira@example.com', NULL, 'Roberto Alves Teixeira', '$2a$10$F9WLGJ8qCxZh0chJ4agUt.HIGokXts.Q6i3orhM124txzuSZb8/ES', '2024-01-10 08:10:00'),
 (7, 'TEACHER', true, '2024-01-10 08:10:00', 'fernanda.martins@example.com', NULL, 'Fernanda Souza Martins', '$2a$10$F9WLGJ8qCxZh0chJ4agUt.HIGokXts.Q6i3orhM124txzuSZb8/ES', '2024-01-10 08:10:00'),
 (8, 'TEACHER', true, '2024-01-10 08:10:00', 'juliana.duarte@example.com', NULL, 'Juliana Pereira Duarte', '$2a$10$F9WLGJ8qCxZh0chJ4agUt.HIGokXts.Q6i3orhM124txzuSZb8/ES', '2024-01-10 08:10:00'),
 (9, 'TEACHER', true, '2024-01-10 08:10:00', 'marcos.rocha@example.com', NULL, 'Marcos Vinícius Rocha', '$2a$10$F9WLGJ8qCxZh0chJ4agUt.HIGokXts.Q6i3orhM124txzuSZb8/ES', '2024-01-10 08:10:00'),
 (10, 'TEACHER', true, '2024-01-10 08:10:00', 'luciana.nunes@example.com', NULL, 'Luciana Barbosa Nunes', '$2a$10$F9WLGJ8qCxZh0chJ4agUt.HIGokXts.Q6i3orhM124txzuSZb8/ES', '2024-01-10 08:10:00'),
 (11, 'STUDENT', true, '2024-01-20 10:00:00', 'lucas.almeida@example.com', '2026-10-01 08:00:00', 'Lucas Henrique Almeida', '$2a$10$F9WLGJ8qCxZh0chJ4agUt.HIGokXts.Q6i3orhM124txzuSZb8/ES', '2024-01-20 10:00:00'),
 (12, 'STUDENT', true, '2024-01-20 10:00:00', 'beatriz.lopes@example.com', NULL, 'Beatriz Fernandes Lopes', '$2a$10$F9WLGJ8qCxZh0chJ4agUt.HIGokXts.Q6i3orhM124txzuSZb8/ES', '2024-01-20 10:00:00'),
 (13, 'STUDENT', true, '2024-01-20 10:00:00', 'gabriel.oliveira@example.com', NULL, 'Gabriel Santos Oliveira', '$2a$10$F9WLGJ8qCxZh0chJ4agUt.HIGokXts.Q6i3orhM124txzuSZb8/ES', '2024-01-20 10:00:00'),
 (14, 'STUDENT', true, '2024-01-20 10:00:00', 'larissa.cardoso@example.com', NULL, 'Larissa Moreira Cardoso', '$2a$10$F9WLGJ8qCxZh0chJ4agUt.HIGokXts.Q6i3orhM124txzuSZb8/ES', '2024-01-20 10:00:00'),
 (15, 'STUDENT', true, '2024-01-20 10:00:00', 'pedro.gomes@example.com', NULL, 'Pedro Henrique Gomes', '$2a$10$F9WLGJ8qCxZh0chJ4agUt.HIGokXts.Q6i3orhM124txzuSZb8/ES', '2024-01-20 10:00:00'),
 (16, 'STUDENT', true, '2024-01-20 10:00:00', 'isabela.ferreira@example.com', NULL, 'Isabela Ramos Ferreira', '$2a$10$F9WLGJ8qCxZh0chJ4agUt.HIGokXts.Q6i3orhM124txzuSZb8/ES', '2024-01-20 10:00:00'),
 (17, 'STUDENT', true, '2024-01-20 10:00:00', 'matheus.dias@example.com', NULL, 'Matheus Carvalho Dias', '$2a$10$F9WLGJ8qCxZh0chJ4agUt.HIGokXts.Q6i3orhM124txzuSZb8/ES', '2024-01-20 10:00:00'),
 (18, 'STUDENT', true, '2024-01-20 10:00:00', 'camila.barros@example.com', NULL, 'Camila Araújo Barros', '$2a$10$F9WLGJ8qCxZh0chJ4agUt.HIGokXts.Q6i3orhM124txzuSZb8/ES', '2024-01-20 10:00:00'),
 (19, 'STUDENT', true, '2024-01-20 10:00:00', 'rafael.pinto@example.com', NULL, 'Rafael Mendes Pinto', '$2a$10$F9WLGJ8qCxZh0chJ4agUt.HIGokXts.Q6i3orhM124txzuSZb8/ES', '2024-01-20 10:00:00'),
 (20, 'STUDENT', true, '2024-01-20 10:00:00', 'leticia.machado@example.com', NULL, 'Letícia Correia Machado', '$2a$10$F9WLGJ8qCxZh0chJ4agUt.HIGokXts.Q6i3orhM124txzuSZb8/ES', '2024-01-20 10:00:00'),
 (21, 'STUDENT', true, '2024-01-20 10:00:00', 'guilherme.freitas@example.com', '2026-09-29 08:00:00', 'Guilherme Batista Freitas', '$2a$10$F9WLGJ8qCxZh0chJ4agUt.HIGokXts.Q6i3orhM124txzuSZb8/ES', '2024-01-20 10:00:00'),
 (22, 'STUDENT', true, '2024-01-20 10:00:00', 'amanda.castro@example.com', NULL, 'Amanda Rodrigues Castro', '$2a$10$F9WLGJ8qCxZh0chJ4agUt.HIGokXts.Q6i3orhM124txzuSZb8/ES', '2024-01-20 10:00:00'),
 (23, 'STUDENT', true, '2024-01-20 10:00:00', 'felipe.lima@example.com', NULL, 'Felipe Monteiro Lima', '$2a$10$F9WLGJ8qCxZh0chJ4agUt.HIGokXts.Q6i3orhM124txzuSZb8/ES', '2024-01-20 10:00:00'),
 (24, 'STUDENT', true, '2024-01-20 10:00:00', 'julia.neves@example.com', NULL, 'Júlia Cavalcanti Neves', '$2a$10$F9WLGJ8qCxZh0chJ4agUt.HIGokXts.Q6i3orhM124txzuSZb8/ES', '2024-01-20 10:00:00'),
 (25, 'STUDENT', true, '2024-01-20 10:00:00', 'vinicius.azevedo@example.com', NULL, 'Vinícius Ribeiro Azevedo', '$2a$10$F9WLGJ8qCxZh0chJ4agUt.HIGokXts.Q6i3orhM124txzuSZb8/ES', '2024-01-20 10:00:00'),
 (26, 'STUDENT', true, '2024-01-20 10:00:00', 'mariana.borges@example.com', NULL, 'Mariana Teixeira Borges', '$2a$10$F9WLGJ8qCxZh0chJ4agUt.HIGokXts.Q6i3orhM124txzuSZb8/ES', '2024-01-20 10:00:00'),
 (27, 'STUDENT', true, '2024-01-20 10:00:00', 'thiago.prado@example.com', NULL, 'Thiago Nascimento Prado', '$2a$10$F9WLGJ8qCxZh0chJ4agUt.HIGokXts.Q6i3orhM124txzuSZb8/ES', '2024-01-20 10:00:00'),
 (28, 'STUDENT', true, '2024-01-20 10:00:00', 'bianca.rocha@example.com', NULL, 'Bianca Vasconcelos Rocha', '$2a$10$F9WLGJ8qCxZh0chJ4agUt.HIGokXts.Q6i3orhM124txzuSZb8/ES', '2024-01-20 10:00:00'),
 (29, 'STUDENT', true, '2024-01-20 10:00:00', 'leonardo.salles@example.com', NULL, 'Leonardo Duarte Salles', '$2a$10$F9WLGJ8qCxZh0chJ4agUt.HIGokXts.Q6i3orhM124txzuSZb8/ES', '2024-01-20 10:00:00'),
 (30, 'STUDENT', true, '2024-01-20 10:00:00', 'natalia.furtado@example.com', NULL, 'Natália Pires Furtado', '$2a$10$F9WLGJ8qCxZh0chJ4agUt.HIGokXts.Q6i3orhM124txzuSZb8/ES', '2024-01-20 10:00:00'),
 (31, 'STUDENT', true, '2024-01-20 10:00:00', 'bruno.matos@example.com', NULL, 'Bruno Siqueira Matos', '$2a$10$F9WLGJ8qCxZh0chJ4agUt.HIGokXts.Q6i3orhM124txzuSZb8/ES', '2024-01-20 10:00:00'),
 (32, 'STUDENT', true, '2024-01-20 10:00:00', 'carolina.pacheco@example.com', NULL, 'Carolina Medeiros Pacheco', '$2a$10$F9WLGJ8qCxZh0chJ4agUt.HIGokXts.Q6i3orhM124txzuSZb8/ES', '2024-01-20 10:00:00'),
 (33, 'STUDENT', true, '2024-01-20 10:00:00', 'eduardo.fontes@example.com', NULL, 'Eduardo Lacerda Fontes', '$2a$10$F9WLGJ8qCxZh0chJ4agUt.HIGokXts.Q6i3orhM124txzuSZb8/ES', '2024-01-20 10:00:00'),
 (34, 'STUDENT', true, '2024-01-20 10:00:00', 'gabriela.viana@example.com', NULL, 'Gabriela Antunes Viana', '$2a$10$F9WLGJ8qCxZh0chJ4agUt.HIGokXts.Q6i3orhM124txzuSZb8/ES', '2024-01-20 10:00:00'),
 (35, 'STUDENT', true, '2024-01-20 10:00:00', 'joao.campos@example.com', NULL, 'João Pedro Campos', '$2a$10$F9WLGJ8qCxZh0chJ4agUt.HIGokXts.Q6i3orhM124txzuSZb8/ES', '2024-01-20 10:00:00'),
 (36, 'STUDENT', true, '2024-01-20 10:00:00', 'sofia.reis@example.com', NULL, 'Sofia Albuquerque Reis', '$2a$10$F9WLGJ8qCxZh0chJ4agUt.HIGokXts.Q6i3orhM124txzuSZb8/ES', '2024-01-20 10:00:00'),
 (37, 'STUDENT', true, '2024-01-20 10:00:00', 'henrique.moura@example.com', NULL, 'Henrique Tavares Moura', '$2a$10$F9WLGJ8qCxZh0chJ4agUt.HIGokXts.Q6i3orhM124txzuSZb8/ES', '2024-01-20 10:00:00'),
 (38, 'STUDENT', true, '2024-01-20 10:00:00', 'manuela.quintana@example.com', NULL, 'Manuela Bastos Quintana', '$2a$10$F9WLGJ8qCxZh0chJ4agUt.HIGokXts.Q6i3orhM124txzuSZb8/ES', '2024-01-20 10:00:00'),
 (39, 'STUDENT', true, '2024-01-20 10:00:00', 'daniel.cunha@example.com', NULL, 'Daniel Figueiredo Cunha', '$2a$10$F9WLGJ8qCxZh0chJ4agUt.HIGokXts.Q6i3orhM124txzuSZb8/ES', '2024-01-20 10:00:00'),
 (40, 'STUDENT', true, '2024-01-20 10:00:00', 'livia.torres@example.com', NULL, 'Lívia Magalhães Torres', '$2a$10$F9WLGJ8qCxZh0chJ4agUt.HIGokXts.Q6i3orhM124txzuSZb8/ES', '2024-01-20 10:00:00'),
 (41, 'STUDENT', true, '2026-01-20 10:00:00', 'andre.santana@example.com', NULL, 'André Luiz Santana', '$2a$10$F9WLGJ8qCxZh0chJ4agUt.HIGokXts.Q6i3orhM124txzuSZb8/ES', '2026-01-20 10:00:00'),
 (42, 'STUDENT', true, '2026-01-20 10:00:00', 'bruna.melo@example.com', NULL, 'Bruna Rafaela Melo', '$2a$10$F9WLGJ8qCxZh0chJ4agUt.HIGokXts.Q6i3orhM124txzuSZb8/ES', '2026-01-20 10:00:00'),
 (43, 'STUDENT', true, '2026-01-20 10:00:00', 'caio.peixoto@example.com', NULL, 'Caio Henrique Peixoto', '$2a$10$F9WLGJ8qCxZh0chJ4agUt.HIGokXts.Q6i3orhM124txzuSZb8/ES', '2026-01-20 10:00:00'),
 (44, 'STUDENT', true, '2026-01-20 10:00:00', 'daniela.moraes@example.com', NULL, 'Daniela Cristina Moraes', '$2a$10$F9WLGJ8qCxZh0chJ4agUt.HIGokXts.Q6i3orhM124txzuSZb8/ES', '2026-01-20 10:00:00'),
 (45, 'STUDENT', true, '2026-01-20 10:00:00', 'enzo.rezende@example.com', NULL, 'Enzo Gabriel Rezende', '$2a$10$F9WLGJ8qCxZh0chJ4agUt.HIGokXts.Q6i3orhM124txzuSZb8/ES', '2026-01-20 10:00:00');

-- Perfis específicos (mesmo ID da tabela users).
INSERT INTO principals (id) VALUES (1);
INSERT INTO secretaries (id) VALUES (2);
INSERT INTO teachers (id) VALUES
 (3),
 (4),
 (5),
 (6),
 (7),
 (8),
 (9),
 (10);

-- Alunos: data de nascimento, telefone do responsável, telefone próprio
-- (opcional; NULL na coorte A, mais nova), bloqueio de chat e número de
-- matrícula (ano de ingresso + sequencial, único por aluno).
INSERT INTO students (id, birth_date, guardian_phone, is_chat_blocked, own_phone, enrollment) VALUES
 (11, '2011-01-01', '(48) 99507-2001', false, NULL, '2024001'),
 (12, '2011-06-08', '(48) 99544-2092', false, NULL, '2024002'),
 (13, '2011-11-15', '(48) 99581-2183', false, NULL, '2024003'),
 (14, '2011-04-22', '(48) 99618-2274', false, NULL, '2024004'),
 (15, '2011-09-02', '(48) 99655-2365', false, NULL, '2024005'),
 (16, '2011-02-09', '(48) 99692-2456', false, NULL, '2024006'),
 (17, '2011-07-16', '(48) 99729-2547', false, NULL, '2024007'),
 (18, '2011-12-23', '(48) 99766-2638', false, NULL, '2024008'),
 (19, '2011-05-03', '(48) 99803-2729', false, NULL, '2024009'),
 (20, '2011-10-10', '(48) 99840-2820', false, NULL, '2024010'),
 (21, '2010-03-17', '(48) 99877-2911', false, '(48) 98313-1357', '2024011'),
 (22, '2010-08-24', '(48) 99914-3002', false, '(48) 98366-1374', '2024012'),
 (23, '2010-01-04', '(48) 99951-3093', false, '(48) 98419-1391', '2024013'),
 (24, '2010-06-11', '(48) 99988-3184', false, '(48) 98472-1408', '2024014'),
 (25, '2010-11-18', '(48) 99125-3275', false, '(48) 98525-1425', '2024015'),
 (26, '2010-04-25', '(48) 99162-3366', false, '(48) 98578-1442', '2024016'),
 (27, '2010-09-05', '(48) 99199-3457', false, '(48) 98631-1459', '2024017'),
 (28, '2010-02-12', '(48) 99236-3548', false, '(48) 98684-1476', '2024018'),
 (29, '2010-07-19', '(48) 99273-3639', false, '(48) 98737-1493', '2024019'),
 (30, '2010-12-26', '(48) 99310-3730', false, '(48) 98790-1510', '2024020'),
 (31, '2009-05-06', '(48) 99347-3821', false, '(48) 98843-1527', '2024021'),
 (32, '2009-10-13', '(48) 99384-3912', false, '(48) 98896-1544', '2024022'),
 (33, '2009-03-20', '(48) 99421-4003', false, '(48) 98949-1561', '2024023'),
 (34, '2009-08-27', '(48) 99458-4094', false, '(48) 98102-1578', '2024024'),
 (35, '2009-01-07', '(48) 99495-4185', false, '(48) 98155-1595', '2024025'),
 (36, '2009-06-14', '(48) 99532-4276', false, '(48) 98208-1612', '2024026'),
 (37, '2009-11-21', '(48) 99569-4367', false, '(48) 98261-1629', '2024027'),
 (38, '2009-04-01', '(48) 99606-4458', false, '(48) 98314-1646', '2024028'),
 (39, '2009-09-08', '(48) 99643-4549', false, '(48) 98367-1663', '2024029'),
 (40, '2009-02-15', '(48) 99680-4640', false, '(48) 98420-1680', '2024030'),
 (41, '2010-03-12', '(48) 99701-4701', false, '(48) 98421-1701', '2026031'),
 (42, '2010-07-19', '(48) 99702-4702', false, '(48) 98422-1702', '2026032'),
 (43, '2010-11-05', '(48) 99703-4703', false, '(48) 98423-1703', '2026033'),
 (44, '2009-04-25', '(48) 99704-4704', false, '(48) 98424-1704', '2026034'),
 (45, '2009-09-14', '(48) 99705-4705', false, '(48) 98425-1705', '2026035');

-- =====================================================================
-- 4. DISCIPLINAS
-- =====================================================================
-- 8 disciplinas, cada uma lecionada por um professor (professor = ID da
-- disciplina + 2, ex.: Língua Portuguesa -> professora de ID 3).
INSERT INTO subjects (id, active, created_at, description, name, updated_at) VALUES
 (1, true, '2024-01-12 09:00:00', 'Leitura, interpretação, gramática e produção textual', 'Língua Portuguesa', '2024-01-12 09:00:00'),
 (2, true, '2024-01-12 09:00:00', 'Aritmética, álgebra, geometria e raciocínio lógico', 'Matemática', '2024-01-12 09:00:00'),
 (3, true, '2024-01-12 09:00:00', 'História do Brasil e geral', 'História', '2024-01-12 09:00:00'),
 (4, true, '2024-01-12 09:00:00', 'Geografia física, humana e econômica', 'Geografia', '2024-01-12 09:00:00'),
 (5, true, '2024-01-12 09:00:00', 'Ciências naturais, biologia, física e química', 'Ciências', '2024-01-12 09:00:00'),
 (6, true, '2024-01-12 09:00:00', 'Língua inglesa: leitura, escrita e conversação', 'Inglês', '2024-01-12 09:00:00'),
 (7, true, '2024-01-12 09:00:00', 'Práticas corporais, esportes e saúde', 'Educação Física', '2024-01-12 09:00:00'),
 (8, true, '2024-01-12 09:00:00', 'Artes visuais, música e expressão cultural', 'Artes', '2024-01-12 09:00:00');

-- =====================================================================
-- 5. TURMAS
-- =====================================================================
-- 3 turmas por ano letivo (9 no total). Fórmula do ID: (ano-2024)*3 + coorte + 1
--   IDs 1 a 3 = turmas de 2024 | 4 a 6 = turmas de 2025 | 7 a 9 = turmas de 2026.
-- Cada coorte avança uma série por ano. A coorte C estuda à tarde.
-- Turmas de anos encerrados ficam com active = false.
INSERT INTO class_rooms (id, active, created_at, number, school_grade, shift, updated_at, academic_year_id) VALUES
 (1, false, '2024-01-20 09:00:00', 'F8-A', 'FUNDAMENTAL_8', 'MORNING', '2024-01-20 09:00:00', 1),
 (2, false, '2024-01-20 09:00:00', 'F9-A', 'FUNDAMENTAL_9', 'MORNING', '2024-01-20 09:00:00', 1),
 (3, false, '2024-01-20 09:00:00', 'M1-A', 'MEDIO_1', 'AFTERNOON', '2024-01-20 09:00:00', 1),
 (4, false, '2025-01-20 09:00:00', 'F9-A', 'FUNDAMENTAL_9', 'MORNING', '2025-01-20 09:00:00', 2),
 (5, false, '2025-01-20 09:00:00', 'M1-A', 'MEDIO_1', 'MORNING', '2025-01-20 09:00:00', 2),
 (6, false, '2025-01-20 09:00:00', 'M2-A', 'MEDIO_2', 'AFTERNOON', '2025-01-20 09:00:00', 2),
 (7, true, '2026-01-20 09:00:00', 'M1-A', 'MEDIO_1', 'MORNING', '2026-01-20 09:00:00', 3),
 (8, true, '2026-01-20 09:00:00', 'M2-A', 'MEDIO_2', 'MORNING', '2026-01-20 09:00:00', 3),
 (9, true, '2026-01-20 09:00:00', 'M3-A', 'MEDIO_3', 'AFTERNOON', '2026-01-20 09:00:00', 3);

-- =====================================================================
-- 6. MATRÍCULAS
-- =====================================================================
-- Cada aluno (IDs 11 a 40) é matriculado, em cada ano, na turma da sua coorte:
--   coorte = (id_do_aluno - 11) / 10   (divisão inteira: 0, 1 ou 2)
--   id_da_turma = (índice do ano) * 3 + coorte + 1
-- Os alunos 41 a 45 permanecem sem matrícula ativa para testes de matrícula.
-- Situação geral: APPROVED nos anos encerrados e PENDING em 2026 (o ano
-- ainda não terminou). Matrículas de anos encerrados ficam inativas.
INSERT INTO enrollments (active, created_at, enrollment_date, general_situation, class_room_id, student_id)
SELECT ay.active,
       ay.start_date::timestamp - interval '15 days',   -- criada 15 dias antes do início do ano
       ay.start_date,
       CASE WHEN ay.active THEN 'PENDING' ELSE 'APPROVED' END,
       c.id,
       s.id
FROM students s
CROSS JOIN generate_series(0, 2) AS y(i)                 -- y.i = índice do ano (0=2024, 1=2025, 2=2026)
JOIN class_rooms c ON c.id = y.i * 3 + ((s.id - 11) / 10) + 1
JOIN academic_years ay ON ay.id = c.academic_year_id
WHERE s.id <= 40
ORDER BY ay.year, s.id;

-- =====================================================================
-- 7. ATRIBUIÇÃO PROFESSOR x TURMA x DISCIPLINA
-- =====================================================================
-- Liga cada turma às 8 disciplinas e ao professor responsável
-- (72 registros = 9 turmas x 8 disciplinas). Define também os limites de
-- avaliações por período: mínimo 2 e máximo 3.
INSERT INTO teacher_class_subject (created_at, max_assessments_per_period, min_assessments_per_period, class_room_id, subject_id, teacher_id, active)
SELECT ay.start_date::timestamp - interval '10 days', 3, 2, c.id, sb.id,
       sb.id + 2,                                        -- professor da disciplina (IDs 3 a 10)
       ay.active
FROM class_rooms c
JOIN academic_years ay ON ay.id = c.academic_year_id
CROSS JOIN subjects sb
ORDER BY c.id, sb.id;

-- =====================================================================
-- 8. AULAS
-- =====================================================================
-- Para cada vínculo professor/turma/disciplina e para cada trimestre do ano
-- são geradas até 6 aulas com planos pedagógicos realistas por disciplina.
-- O deslocamento ((subject_id-1) % 5) espalha as disciplinas de
-- segunda a sexta. Só entram aulas até 06/10/2026 (data de referência) e
-- dentro do período; por isso o 3º trimestre de 2026 tem menos aulas.
-- lesson_count (1 ou 2) = quantidade de aulas geminadas no dia.
INSERT INTO lessons (content, created_at, lesson_count, lesson_date, notes, updated_at, teacher_class_subject_id, active)
SELECT 
    CASE sb.id
        -- Língua Portuguesa
        WHEN 1 THEN (ARRAY[
            'Gêneros textuais contemporâneos: crônica jornalística e análise crítica',
            'Sintaxe do período composto por coordenação e subordinação',
            'Semântica e figuras de linguagem na interpretação de textos literários',
            'Produção textual dissertativo-argumentativa: introdução e tese',
            'Oficina de redação: coesão referencial e conectivos',
            'Revisão geral de gramática e estrutura textual para avaliação trimestral'
        ])[n.i + 1]
        -- Matemática
        WHEN 2 THEN (ARRAY[
            'Introdução às funções afins e quadráticas: estudo dos coeficientes',
            'Resolução de problemas contextualizados com equações e inequações',
            'Geometria plana e espacial: cálculo de perímetros, áreas e volumes',
            'Trigonometria no triângulo retângulo e razões fundamentais',
            'Análise de gráficos estatísticos, média ponderada e desvio padrão',
            'Plantão de resolução de exercícios e revisão dos conteúdos da prova'
        ])[n.i + 1]
        -- História
        WHEN 3 THEN (ARRAY[
            'A Era Vargas e as transformações trabalhistas e sociais no Brasil',
            'Crise de 1929 e a ascensão dos regimes totalitários na Europa',
            'Segunda Guerra Mundial: antecedentes, desdobramentos e ordem bipolar',
            'Guerra Fria: impactos geopolíticos na América Latina',
            'Ditadura Civil-Militar no Brasil: censura, resistência e movimentos sociais',
            'Síntese histórica, debate orientado e revisão preparatória do trimestre'
        ])[n.i + 1]
        -- Geografia
        WHEN 4 THEN (ARRAY[
            'Globalização e as novas redes de fluxos de capitais e mercadorias',
            'Geopolítica mundial: blocos econômicos e conflitos territoriais',
            'Urbanização brasileira e segregação socioespacial nas metrópoles',
            'Impactos socioambientais: desmatamento, transição energética e clima',
            'Cartografia temática e leitura de mapas de densidade demográfica',
            'Revisão interdisciplinar de geografia física e econômica'
        ])[n.i + 1]
        -- Ciências
        WHEN 5 THEN (ARRAY[
            'Genética mendeliana e hereditariedade: noções de DNA e cromossomos',
            'Evolução e seleção natural: teorias de Lamarck e Darwin',
            'Química fundamental: modelos atômicos e ligações químicas covalentes',
            'Termologia e leis fundamentais da conservação da energia',
            'Prática no laboratório de ciências: observação microscópica e reações',
            'Revisão dos eixos temáticos e discussão dos relatórios científicos'
        ])[n.i + 1]
        -- Inglês
        WHEN 6 THEN (ARRAY[
            'Reading Comprehension: Scanning, skimming and critical text analysis',
            'Grammar Focus: Present Perfect vs Simple Past in authentic contexts',
            'Listening & Speaking: Global environmental issues and debates',
            'Vocabulary building: Academic discourse and formal register',
            'Collaborative writing: Opinion essays and peer review',
            'Final trimestrial review: Interactive mock test and feedback'
        ])[n.i + 1]
        -- Educação Física
        WHEN 7 THEN (ARRAY[
            'Fundamentos táticos dos esportes coletivos: voleibol e futsal',
            'Capacidades físicas: resistência aeróbica, velocidade e flexibilidade',
            'História e dimensões culturais dos Jogos Olímpicos modernos',
            'Práticas corporais de aventura e expressão rítmica integrada',
            'Saúde, ergonomia e prevenção do sedentarismo na adolescência',
            'Vivência prática integrativa e autoavaliação do desempenho corporal'
        ])[n.i + 1]
        -- Artes
        WHEN 8 THEN (ARRAY[
            'Vanguardas europeias no século XX: Cubismo, Surrealismo e Futurismo',
            'Modernismo no Brasil e a Semana de Arte Moderna de 1922',
            'Linguagens visuais contemporâneas: fotografia, design e intervenção',
            'Música popular brasileira como patrimônio imaterial e resistência',
            'Oficina prática de criação: releitura visual e composição artística',
            'Apresentação do portfólio artístico e reflexão crítica do período'
        ])[n.i + 1]
        ELSE sb.name || ': Aula temática ' || (n.i + 1)
    END,
    x.d::timestamp + interval '8 hours',
    1 + ((t.id + n.i) % 2),
    x.d,
    CASE 
        WHEN n.i = 4 THEN 'Atividade avaliativa formativa / entrega de relatório preliminar'
        WHEN n.i = 5 THEN 'Encontro de consolidação de dúvidas antes da avaliação trimestral'
        ELSE NULL
    END,
    x.d::timestamp + interval '8 hours',
    t.id,
    true
FROM teacher_class_subject t
JOIN class_rooms c ON c.id = t.class_room_id
JOIN subjects sb ON sb.id = t.subject_id
JOIN academic_periods p ON p.academic_year_id = c.academic_year_id
CROSS JOIN generate_series(0, 5) AS n(i)                 -- n.i = número da aula no trimestre (0 a 5)
CROSS JOIN LATERAL (SELECT p.start_date + (n.i * 14) + ((t.subject_id - 1) % 5) AS d) x  -- x.d = data da aula
WHERE x.d <= p.end_date AND x.d <= DATE '2026-10-06'
ORDER BY t.id, p.start_date, n.i;

-- =====================================================================
-- 9. PRESENÇAS
-- =====================================================================
-- Para cada aula, uma presença para cada aluno matriculado na turma.
-- O status é definido de forma determinística:
--   - Alunos com reprovação por falta (19, 29, 40) em 2026: ~42% ABSENT (frequência ~58% < 75%)
--   - Demais alunos: ~91% PRESENT, ~5% ABSENT, ~4% JUSTIFIED_ABSENCE (frequência > 90%)
INSERT INTO attendances (created_at, status, updated_at, lesson_id, student_id, active)
SELECT l.created_at,
       CASE 
         WHEN e.student_id IN (19, 29, 40) AND l.lesson_date >= DATE '2026-01-01' THEN
           CASE WHEN h.v < 42 THEN 'ABSENT' WHEN h.v < 92 THEN 'PRESENT' ELSE 'JUSTIFIED_ABSENCE' END
         WHEN h.v < 91 THEN 'PRESENT'
         WHEN h.v < 96 THEN 'ABSENT'
         ELSE 'JUSTIFIED_ABSENCE'
       END,
       l.created_at,
       l.id,
       e.student_id,
       true
FROM lessons l
JOIN teacher_class_subject t ON t.id = l.teacher_class_subject_id
JOIN enrollments e ON e.class_room_id = t.class_room_id
CROSS JOIN LATERAL (SELECT (l.id * 7 + e.student_id * 13) % 100 AS v) h
ORDER BY l.id, e.student_id;

-- Histórico de presença: toda falta justificada começou como ABSENT e foi
-- alterada 2 dias depois para JUSTIFIED_ABSENCE (ex.: aluno entregou atestado).
-- Registrado pelo professor responsável da turma/disciplina.
INSERT INTO attendance_history (changed_at, new_status, old_status, attendance_id, changed_by)
SELECT a.created_at + interval '2 days', 'JUSTIFIED_ABSENCE', 'ABSENT', a.id, tcs.teacher_id
FROM attendances a
JOIN lessons l ON l.id = a.lesson_id
JOIN teacher_class_subject tcs ON tcs.id = l.teacher_class_subject_id
WHERE a.status = 'JUSTIFIED_ABSENCE'
ORDER BY a.id;

-- Mantém updated_at coerente com a data da alteração registrada no histórico.
UPDATE attendances SET updated_at = created_at + interval '2 days' WHERE status = 'JUSTIFIED_ABSENCE';

-- =====================================================================
-- 10. AVALIAÇÕES
-- =====================================================================
-- 2 avaliações por vínculo professor/turma/disciplina em cada trimestre,
-- respeitando a nomenclatura pedagógica contextualizada de cada disciplina:
--   * 1ª avaliação: PROVA (EXAM) com títulos autênticos por disciplina e trimestre;
--   * 2ª avaliação: TRABALHO (ASSIGNMENT) no 1º e 3º trimestres ou
--     VISTO DE CADERNO / ATIVIDADE PRÁTICA (NOTEBOOK) no 2º trimestre.
-- Em 2026, a 2ª avaliação do 3º trimestre ainda está no futuro (novembro).
INSERT INTO assessments (assessment_type, created_at, assessment_date, description, title, updated_at, period_id, teacher_class_subject_id, active)
SELECT k.typ,
       p.start_date::timestamp + interval '7 days',
       p.start_date + k.off + ((t.subject_id - 1) % 5),
       CASE k.n
           WHEN 1 THEN 
               CASE sb.id
                   WHEN 1 THEN 'Avaliação formal de interpretação textual, coesão e análise sintática'
                   WHEN 2 THEN 'Prova individual de raciocínio lógico, cálculo algébrico e geometria'
                   WHEN 3 THEN 'Avaliação discursiva de contextualização histórica e interpretação de fontes'
                   WHEN 4 THEN 'Prova de dinâmica territorial, geopolítica e análise cartográfica'
                   WHEN 5 THEN 'Avaliação teórico-prática de ciências naturais e biologia celular'
                   WHEN 6 THEN 'Reading comprehension test, lexical analysis and grammar accuracy'
                   WHEN 7 THEN 'Avaliação teórica de regras desportivas, fisiologia e hábitos saudáveis'
                   WHEN 8 THEN 'Análise estética, história da arte e manifestações culturais brasileiras'
                   ELSE 'Avaliação somativa formal do trimestre'
               END
           ELSE 
               CASE 
                   WHEN k.typ = 'ASSIGNMENT' THEN 
                       CASE sb.id
                           WHEN 1 THEN 'Produção de redação dissertativa com debate temático em grupo'
                           WHEN 2 THEN 'Trabalho de modelagem matemática com dados do cotidiano escolar'
                           WHEN 3 THEN 'Pesquisa bibliográfica e seminário temático de memória histórica'
                           WHEN 4 THEN 'Elaboração de mapa temático e relatório socioambiental'
                           WHEN 5 THEN 'Relatório experimental investigativo de laboratório'
                           WHEN 6 THEN 'Oral presentation and video storytelling in English'
                           WHEN 7 THEN 'Circuito motor e planejamento de torneio interclasses'
                           WHEN 8 THEN 'Portfólio de expressão artística e releitura contemporânea'
                           ELSE 'Trabalho bimestral de pesquisa aplicada'
                       END
                   ELSE 'Visto do caderno pedagógico, anotações de aula e resolução de listas semanais'
               END
       END,
       CASE k.n
           WHEN 1 THEN 
               CASE sb.id
                   WHEN 1 THEN 'Prova Bimestral de Língua Portuguesa (' || ((p.id - 1) % 3 + 1) || 'º Tri)'
                   WHEN 2 THEN 'Avaliação de Matemática: Álgebra e Geometria (' || ((p.id - 1) % 3 + 1) || 'º Tri)'
                   WHEN 3 THEN 'Prova de História: Sociedade e Transformações (' || ((p.id - 1) % 3 + 1) || 'º Tri)'
                   WHEN 4 THEN 'Avaliação de Geografia: Espaço e Dinâmica (' || ((p.id - 1) % 3 + 1) || 'º Tri)'
                   WHEN 5 THEN 'Prova Trimestral de Ciências Naturais (' || ((p.id - 1) % 3 + 1) || 'º Tri)'
                   WHEN 6 THEN 'English Written Assessment: Unit Progress (' || ((p.id - 1) % 3 + 1) || 'º Tri)'
                   WHEN 7 THEN 'Avaliação Teórica de Educação Física (' || ((p.id - 1) % 3 + 1) || 'º Tri)'
                   WHEN 8 THEN 'Avaliação de História da Arte e Expressão (' || ((p.id - 1) % 3 + 1) || 'º Tri)'
                   ELSE 'Prova Escrita do ' || ((p.id - 1) % 3 + 1) || 'º Trimestre'
               END
           ELSE 
               CASE 
                   WHEN k.typ = 'ASSIGNMENT' THEN 
                       CASE sb.id
                           WHEN 1 THEN 'Redação e Seminário de Literatura (' || ((p.id - 1) % 3 + 1) || 'º Tri)'
                           WHEN 2 THEN 'Projeto Prático de Estatística e Resolução de Problemas (' || ((p.id - 1) % 3 + 1) || 'º Tri)'
                           WHEN 3 THEN 'Pesquisa Documental e Apresentação em Grupo (' || ((p.id - 1) % 3 + 1) || 'º Tri)'
                           WHEN 4 THEN 'Estudo de Caso Socioambiental (' || ((p.id - 1) % 3 + 1) || 'º Tri)'
                           WHEN 5 THEN 'Relatório Investigativo de Laboratório (' || ((p.id - 1) % 3 + 1) || 'º Tri)'
                           WHEN 6 THEN 'Speaking Project & Cultural Essay (' || ((p.id - 1) % 3 + 1) || 'º Tri)'
                           WHEN 7 THEN 'Avaliação Prática de Habilidades Esportivas (' || ((p.id - 1) % 3 + 1) || 'º Tri)'
                           WHEN 8 THEN 'Painel Artístico e Portfólio de Criação (' || ((p.id - 1) % 3 + 1) || 'º Tri)'
                           ELSE 'Trabalho Prático do ' || ((p.id - 1) % 3 + 1) || 'º Trimestre'
                       END
                   ELSE 'Acompanhamento de Caderno e Listas (' || ((p.id - 1) % 3 + 1) || 'º Tri)'
               END
       END,
       p.start_date::timestamp + interval '7 days',
       p.id,
       t.id,
       true
FROM teacher_class_subject t
JOIN class_rooms c ON c.id = t.class_room_id
JOIN subjects sb ON sb.id = t.subject_id
JOIN academic_periods p ON p.academic_year_id = c.academic_year_id
CROSS JOIN LATERAL (
    VALUES (1, 'EXAM', 28),
           (2, CASE WHEN ((p.id - 1) % 3 + 1) % 2 = 1 THEN 'ASSIGNMENT' ELSE 'NOTEBOOK' END, 63)
) AS k(n, typ, off)
ORDER BY t.id, p.id, k.n;

-- =====================================================================
-- 11. NOTAS
-- =====================================================================
-- Distribuição diversificada e coerente de notas:
-- Em 2024 e 2025: todos aprovados (notas 6,0 a 9,5) para manter histórico escolar.
-- Em 2026 (ano ativo com 1º e 2º trimestres fechados e 3º trimestre em andamento):
--   - Alunos 11, 12, 13, 21, 22, 23, 31, 32, 33 (9 alunos):
--       Matematicamente APROVADOS no 2º Trimestre (notas ~9.0 a 10.0; soma Tri1 + Tri2 >= 18.0).
--   - Alunos 17, 27, 37, 38 (4 alunos):
--       Matematicamente REPROVADOS no 2º Trimestre por NOTA (notas ~2.5 a 3.5; soma Tri1 + Tri2 < 8.0, impossível atingir 18 mesmo com 10 no 3º tri).
--   - Alunos 18, 28, 39 (3 alunos):
--       REPROVADOS no 3º Trimestre por NOTA (notas ~5.0 nos 2 primeiros trimestres e ~3.5 no 3º trimestre).
--   - Alunos 19, 29, 40 (3 alunos):
--       REPROVADOS no 3º Trimestre por FALTA (notas boas ~7.5, mas frequência abaixo de 75%).
--       Representam 30% do total de reprovados (3 de 10).
--   - Demais alunos 14, 15, 16, 20, 24, 25, 26, 30, 34, 35, 36 (11 alunos):
--       APROVADOS no 3º Trimestre (médias ~6.5 a 7.5 no Tri1/Tri2 e ~8.0 no Tri3).
INSERT INTO grades (created_at, student_situation, updated_at, score, assessment_id, student_id, active)
SELECT (a.assessment_date + 1)::timestamp + interval '10 hours',
       CASE WHEN s.sc >= ay.minimum_grade THEN 'APPROVED' ELSE 'FAILED' END,
       (a.assessment_date + 1)::timestamp + interval '10 hours',
       s.sc,
       a.id,
       e.student_id,
       true
FROM assessments a
JOIN teacher_class_subject t ON t.id = a.teacher_class_subject_id
JOIN class_rooms c ON c.id = t.class_room_id
JOIN academic_years ay ON ay.id = c.academic_year_id
JOIN enrollments e ON e.class_room_id = c.id
CROSS JOIN LATERAL (
    SELECT round(
      CASE 
        -- Anos anteriores (2024 e 2025): todos aprovados com boas notas
        WHEN ay.year < 2026 THEN
          6.0 + ((e.student_id * 31 + a.id * 17) % 35) / 10.0

        -- 2026: Ano ativo em andamento
        -- 1. Matematicamente Aprovados no 2º Trimestre (Soma Tri1 + Tri2 >= 18.0)
        WHEN e.student_id IN (11, 12, 13, 21, 22, 23, 31, 32, 33) THEN
          9.0 + ((e.student_id * 13 + a.id * 7) % 11) / 10.0

        -- 2. Matematicamente Reprovados no 2º Trimestre por NOTA (Soma Tri1 + Tri2 < 8.0)
        WHEN e.student_id IN (17, 27, 37, 38) THEN
          2.5 + ((e.student_id * 11 + a.id * 5) % 11) / 10.0

        -- 3. Reprovados no 3º Trimestre por NOTA (Tri1/Tri2 medianos ~5.0, Tri3 baixo ~3.5)
        WHEN e.student_id IN (18, 28, 39) THEN
          CASE 
            WHEN ((a.period_id - 1) % 3 + 1) IN (1, 2) THEN
              4.8 + ((e.student_id * 7 + a.id * 3) % 9) / 10.0
            ELSE
              3.0 + ((e.student_id * 7 + a.id * 3) % 11) / 10.0
          END

        -- 4. Reprovados no 3º Trimestre por FALTA (Notas boas ~7.5, frequência insuficiente)
        WHEN e.student_id IN (19, 29, 40) THEN
          7.0 + ((e.student_id * 17 + a.id * 11) % 15) / 10.0

        -- 5. Aprovados no 3º Trimestre (Tri1/Tri2 regulares ~6.8, Tri3 garantindo aprovação ~8.0)
        ELSE
          CASE 
            WHEN ((a.period_id - 1) % 3 + 1) IN (1, 2) THEN
              6.4 + ((e.student_id * 23 + a.id * 13) % 12) / 10.0
            ELSE
              8.0 + ((e.student_id * 23 + a.id * 13) % 15) / 10.0
          END
      END, 1)::numeric(5,2) AS sc
) s
WHERE a.assessment_date + 1 <= DATE '2026-10-06'
ORDER BY a.id, e.student_id;

-- Histórico de notas: 1 em cada 20 notas foi revisada com motivos pedagógicos variados e realistas.
-- Registrado pelo professor responsável da turma/disciplina (ou pela coordenação/secretaria quando aplicável).
INSERT INTO grade_history (changed_at, new_grade, notes, old_grade, grade_id, changed_by)
SELECT g.created_at + interval '1 day',
       g.score,
       (ARRAY[
           'Retificação após deferimento de recurso na questão discursiva',
           'Correção de critério na rubrica de avaliação após conferência com o aluno',
           'Nota atualizada após entrega de trabalho complementar autorizado pela coordenação',
           'Revisão da pontuação de cálculo na folha de resposta da prova',
           'Ajuste decorrente de realização de 2ª chamada da avaliação trimestral',
           'Correção de digitação involuntária no lançamento inicial da nota',
           'Bonificação atribuída pela participação ativa na feira cultural e científica',
           'Reavaliação pedagógica deliberada em reunião do conselho de classe'
       ])[(g.id / 20) % 8 + 1],
       CASE 
           WHEN g.score >= 0.5 THEN g.score - 0.5
           ELSE g.score + 0.5
       END,
       g.id,
       tcs.teacher_id
FROM grades g
JOIN assessments a ON a.id = g.assessment_id
JOIN teacher_class_subject tcs ON tcs.id = a.teacher_class_subject_id
WHERE g.id % 20 = 0
ORDER BY g.id;

-- =====================================================================
-- 12. AVISOS (RECADOS)
-- =====================================================================
-- Avisos publicados pela direção (autor 1) e pela secretaria (autor 2).
--   archived = true  -> aviso antigo, já arquivado;
--   target_teachers  -> true = aviso destinado aos professores;
--   event_date       -> data do evento citado no aviso.
-- Os avisos 1 e 2 pertencem a 2025; os demais, a 2026.
INSERT INTO announcements (id, archived, created_at, event_date, message, target_teachers, title, author_id) VALUES
 (1, true,  '2025-04-28 09:00:00', '2025-05-10', 'Convidamos os responsáveis para a reunião de pais e mestres do 1º semestre, às 9h, no pátio da escola.', false, 'Reunião de pais e mestres', 1),
 (2, true,  '2025-10-20 10:00:00', '2025-11-14', 'A Feira de Ciências 2025 acontecerá no ginásio. Os trabalhos devem ser entregues aos professores até o dia 7.', false, 'Feira de Ciências 2025', 2),
 (3, false, '2026-02-02 08:00:00', '2026-02-09', 'Bem-vindos ao ano letivo de 2026! As aulas começam no dia 9 de fevereiro. Confiram a turma e o horário na secretaria.', false, 'Início do ano letivo 2026', 1),
 (4, false, '2026-08-17 14:00:00', '2026-08-28', 'O conselho de classe do 2º trimestre será realizado no dia 28 de agosto. Professores, lancem as notas até o dia 26.', true,  'Conselho de classe - 2º trimestre', 1),
 (5, false, '2026-09-28 09:30:00', '2026-10-30', 'A Feira de Ciências 2026 será no dia 30 de outubro. Os grupos devem definir seus temas com os professores até 16 de outubro.', false, 'Feira de Ciências 2026', 2),
 (6, false, '2026-10-02 16:00:00', '2026-10-09', 'Reunião pedagógica no dia 9 de outubro, às 17h30, na sala dos professores. Pauta: planejamento das avaliações do 3º trimestre.', true,  'Reunião pedagógica - 3º trimestre', 1),
 (7, false, '2026-10-05 11:00:00', '2026-10-12', 'Não haverá aula no dia 12 de outubro (feriado nacional). As atividades retornam normalmente no dia 13.', false, 'Feriado de 12 de outubro', 2),
 (8, true,  '2024-03-04 09:00:00', '2024-03-15', 'Reunião inicial com pais e responsáveis para o ano letivo de 2024.', false, 'Reunião de Pais e Mestres 2024', 1),
 (9, true,  '2024-11-18 10:00:00', '2024-11-29', 'Encerramento do ano letivo de 2024 e orientações para renovação de matrículas.', false, 'Encerramento do Ano Letivo 2024', 2),
 (10, true, '2025-08-11 15:00:00', '2025-08-22', 'Conselho pedagógico do 2º trimestre de 2025 na sala dos professores.', true,  'Conselho de Classe 2025', 1);

-- Turmas que recebem cada aviso (turmas 1 a 3 = 2024; 4 a 6 = 2025; 7 a 9 = 2026).
-- Os avisos para professores (4, 6 e 10) não são ligados a turmas.
INSERT INTO announcement_classrooms (announcement_id, class_room_id) VALUES
 (1, 4), (1, 5), (1, 6),
 (2, 4), (2, 5), (2, 6),
 (3, 7), (3, 8), (3, 9),
 (5, 7), (5, 8), (5, 9),
 (7, 7), (7, 8), (7, 9),
 (8, 1), (8, 2), (8, 3),
 (9, 1), (9, 2), (9, 3);

-- =====================================================================
-- 13. CHAT
-- =====================================================================
-- Mensagens dos chats das turmas 7 (coorte A) e 8 (coorte B) e do chat
-- global dos professores (is_global_teacher_chat = true, sem turma).
--   replied_to_id    -> mensagem respondida (resposta encadeada);
--   is_edited        -> mensagem editada pelo autor;
--   is_deleted       -> mensagem apagada pelo autor;
--   deleted_by_admin -> mensagem removida pela moderação.
-- Inclui mensagens arquivadas de 2024 e 2025 para consulta no arquivo histórico.
INSERT INTO chat_messages (id, archived, content, deleted_by_admin, is_deleted, is_edited, is_global_teacher_chat, "timestamp", classroom_id, replied_to_id, sender_id) VALUES
 (1, false, 'Bom dia, turma! Lembrem que a prova de Língua Portuguesa é na próxima semana.', false, false, false, false, '2026-09-28 07:45:00', 7, NULL, 3),
 (2, false, 'Professora, a prova vai incluir interpretação de texto?', false, false, false, false, '2026-09-28 07:52:00', 7, 1, 11),
 (3, false, 'Vai sim, e também gramática. Revisem os exercícios das últimas aulas.', false, false, true,  false, '2026-09-28 08:05:00', 7, 2, 3),
 (4, false, 'Alguém tem o material da aula de ontem?', false, true,  false, false, '2026-09-29 18:20:00', 7, NULL, 13),
 (5, false, 'Turma, o trabalho de Matemática pode ser entregue até sexta-feira.', false, false, false, false, '2026-10-01 13:10:00', 8, NULL, 4),
 (6, false, 'Obrigada, professor!', false, false, false, false, '2026-10-01 13:25:00', 8, 5, 22),
 (7, false, 'Pessoal, lembrete da reunião pedagógica na sexta, às 17h30.', false, false, false, true, '2026-10-02 16:10:00', NULL, NULL, 1),
 (8, false, 'Confirmado, estarei presente.', false, false, false, true, '2026-10-02 16:20:00', NULL, 7, 5),
 (9, false, 'Mensagem removida pela moderação.', true, true, false, false, '2026-10-03 10:00:00', 8, NULL, 25),
 (10, true, 'Bom dia, turma! Sejam bem-vindos ao ano letivo de 2024.', false, false, false, false, '2024-02-15 08:00:00', 1, NULL, 3),
 (11, true, 'Bom dia, professora Mariana! Ansioso pelo ano.', false, false, false, false, '2024-02-15 08:05:00', 1, 10, 11),
 (12, true, 'Lembrem-se de trazer o material para a aula de gramática amanhã.', false, false, false, false, '2024-03-10 09:30:00', 1, NULL, 3),
 (13, true, 'Turma, lista de exercícios preparatória para a prova já está liberada.', false, false, false, false, '2024-04-18 10:00:00', 2, NULL, 4),
 (14, true, 'Professor, teremos revisão antes da avaliação?', false, false, false, false, '2024-04-18 10:15:00', 2, 13, 21),
 (15, true, 'Sim, faremos resolução de dúvidas na quarta-feira.', false, false, false, false, '2024-04-18 10:20:00', 2, 14, 4),
 (16, true, 'Colegas, reunião geral do corpo docente agendada para sexta-feira, 14h.', false, false, false, true, '2024-02-16 14:00:00', NULL, NULL, 1),
 (17, true, 'Presença confirmada. Levarei as pautas do planejamento.', false, false, false, true, '2024-02-16 14:20:00', NULL, 16, 3),
 (18, true, 'Iniciando o ano letivo de 2025! Bons estudos a todos.', false, false, false, false, '2025-02-18 08:00:00', 4, NULL, 3),
 (19, true, 'Professora, qual o tema da primeira redação?', false, false, false, false, '2025-02-25 09:00:00', 4, NULL, 11),
 (20, true, 'O tema será Convivência e Cidadania na Escola.', false, false, false, false, '2025-02-25 09:12:00', 4, 19, 3),
 (21, true, 'Pessoal do 1º Médio, entrega do trabalho de trigonometria até dia 25.', false, false, false, false, '2025-04-15 11:00:00', 5, NULL, 4),
 (22, true, 'Entendido professor Carlos, obrigado!', false, false, false, false, '2025-04-15 11:05:00', 5, 21, 21),
 (23, true, 'Lembrete: fechamento das médias do 2º trimestre até o dia 28/08/2025.', false, false, false, true, '2025-08-20 15:00:00', NULL, NULL, 1),
 (24, true, 'Notas de Língua Portuguesa já devidamente consolidadas no sistema.', false, false, false, true, '2025-08-22 16:30:00', NULL, 23, 3),
 (25, true, 'Boas-vindas ao 1º ano do Ensino Médio de 2024! Nosso cronograma está disponível.', false, false, false, false, '2024-02-19 08:30:00', 3, NULL, 4),
 (26, true, 'Professor, os livros didáticos serão entregues nesta semana?', false, false, false, false, '2024-02-19 08:45:00', 3, 25, 31),
 (27, true, 'Turma do 2º Médio de 2025: projeto interdisciplinar com entrega no final do trimestre.', false, false, false, false, '2025-03-12 10:00:00', 6, NULL, 5),
 (28, true, 'Combinado, professora Patrícia! Já formamos os grupos de pesquisa.', false, false, false, false, '2025-03-12 10:20:00', 6, 27, 31);

-- Mensagem ocultada por um usuário apenas para si mesmo (aluno 15 ocultou a mensagem 4).
INSERT INTO chat_hidden_messages (id, hidden_at, message_id, user_id) VALUES
 (1, '2026-09-29 19:00:00', 4, 15);

-- Recibos de leitura: até qual mensagem cada usuário leu em cada chat
-- (usado para calcular as mensagens não lidas).
INSERT INTO chat_read_receipts (id, is_global_teacher_chat, last_read_at, last_read_message_id, classroom_id, user_id) VALUES
 (1, false, '2026-09-28 08:10:00', 3, 7, 11),
 (2, false, '2026-10-01 13:30:00', 6, 8, 4),
 (3, true,  '2026-10-02 16:25:00', 8, NULL, 3),
 (4, true,  '2026-10-02 16:25:00', 8, NULL, 1);

-- Histórico de conversa com o assistente (memória de chat): uma pergunta do
-- aluno 11 e a resposta do assistente.
INSERT INTO chat_message_history (content, conversation_id, created_at, message_type) VALUES
 ('Como calculo minha média do 3º trimestre?', 'user-11', '2026-10-03 20:00:00', 'USER'),
 ('A média é calculada a partir das notas das avaliações do trimestre. Confira as notas na aba de avaliações.', 'user-11', '2026-10-03 20:00:05', 'ASSISTANT');

-- =====================================================================
-- 14. SINCRONIZAÇÃO DAS SEQUENCES
-- =====================================================================
-- Nas tabelas em que os IDs foram informados manualmente, a sequence
-- precisa ser ajustada para o maior ID existente. Sem isso, o próximo
-- INSERT feito pela aplicação tentaria reutilizar o ID 1 e falharia.
-- (Nas demais tabelas os IDs foram gerados automaticamente.)
SELECT setval(pg_get_serial_sequence('school_settings','id'),      (SELECT max(id) FROM school_settings));
SELECT setval(pg_get_serial_sequence('academic_years','id'),       (SELECT max(id) FROM academic_years));
SELECT setval(pg_get_serial_sequence('academic_periods','id'),     (SELECT max(id) FROM academic_periods));
SELECT setval(pg_get_serial_sequence('users','id'),                (SELECT max(id) FROM users));
SELECT setval(pg_get_serial_sequence('subjects','id'),             (SELECT max(id) FROM subjects));
SELECT setval(pg_get_serial_sequence('class_rooms','id'),          (SELECT max(id) FROM class_rooms));
SELECT setval(pg_get_serial_sequence('announcements','id'),        (SELECT max(id) FROM announcements));
SELECT setval(pg_get_serial_sequence('chat_messages','id'),        (SELECT max(id) FROM chat_messages));
SELECT setval(pg_get_serial_sequence('chat_hidden_messages','id'), (SELECT max(id) FROM chat_hidden_messages));
SELECT setval(pg_get_serial_sequence('chat_read_receipts','id'),   (SELECT max(id) FROM chat_read_receipts));

COMMIT; -- confirma a transação
