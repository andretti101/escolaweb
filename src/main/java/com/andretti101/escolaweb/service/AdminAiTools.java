package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.model.entity.*;
import com.andretti101.escolaweb.repository.*;
import com.andretti101.escolaweb.dto.response.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminAiTools {

    private final TransactionTemplate transactionTemplate;
    private final EnrollmentService enrollmentService;
    private final AcademicYearService yearService;
    private final ReportCardService reportCardService;
    private final GradeRepository gradeRepository;
    private final TeacherClassSubjectRepository tcsRepository;
    private final TeacherRepository teacherRepository;
    private final SubjectRepository subjectRepository;
    private final ClassRoomService classRoomService;
    private final JdbcTemplate jdbcTemplate;
    private final SchoolSettingsService schoolSettingsService;

    private static final java.util.regex.Pattern FORBIDDEN_SQL_PATTERN = java.util.regex.Pattern.compile(
            "\\b(INSERT|UPDATE|DELETE|DROP|ALTER|TRUNCATE|EXECUTE|GRANT|REVOKE|CREATE|REPLACE|MERGE|INTO|LOCK|CALL)\\b",
            java.util.regex.Pattern.CASE_INSENSITIVE
    );

    private static final java.util.regex.Pattern SENSITIVE_SQL_PATTERN = java.util.regex.Pattern.compile(
            "\\b(password_reset_tokens|refresh_tokens|flyway_schema_history|chat_message_history|password|pg_sleep|pg_read_file|lo_import|lo_export)\\b",
            java.util.regex.Pattern.CASE_INSENSITIVE
    );

    @Tool(description = "Compara matrículas entre anos letivos")
    public String getEnrollmentStatsByYear() {
        return transactionTemplate.execute(status -> {
            var enrollments = enrollmentService.findAllActive();
            var years = yearService.findAll();
            Map<String, Long> countByYear = new HashMap<>();
            for (var y : years) {
                long c = enrollments.stream().filter(e -> e.getClassRoom().getAcademicYear().getId().equals(y.getId())).count();
                countByYear.put(String.valueOf(y.getYear()), c);
            }
            return countByYear.entrySet().stream()
                .map(e -> "Ano " + e.getKey() + ": " + e.getValue() + " matrículas")
                .collect(Collectors.joining("\n"));
        });
    }

    @Tool(description = "Verifica estatísticas de aprovação")
    public String getApprovalStats() {
        return transactionTemplate.execute(status -> {
            AcademicYear year = yearService.findActive();
            if (year == null) return "Sem ano letivo ativo.";
            var enrollments = enrollmentService.findAllActive().stream()
                .filter(e -> e.getClassRoom().getAcademicYear().getId().equals(year.getId()))
                .collect(Collectors.toList());
            Map<String, Integer> counts = new HashMap<>();
            for (var e : enrollments) {
                try {
                    ReportCardDTO report = reportCardService.generateReportCard(e.getStudent().getId(), year.getId());
                    String st = report.generalSituation().name();
                    counts.put(st, counts.getOrDefault(st, 0) + 1);
                } catch (Exception ex) {}
            }
            return counts.entrySet().stream()
                .map(entry -> "Situação: " + entry.getKey() + " -> " + entry.getValue() + " alunos")
                .collect(Collectors.joining("\n"));
        });
    }

    @Tool(description = "Calcula a média global de todas as notas do ano atual")
    public String getGlobalGradeAverage() {
        return transactionTemplate.execute(status -> {
            AcademicYear year = yearService.findActive();
            if (year == null) return "Sem ano letivo ativo.";
            var grades = gradeRepository.findByAcademicYear(year).stream()
                .filter(g -> g.getEffectiveValue() != null)
                .collect(Collectors.toList());
            if (grades.isEmpty()) return "Nenhuma nota registrada.";
            BigDecimal sum = grades.stream().map(Grade::getEffectiveValue).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal avg = sum.divide(new BigDecimal(grades.size()), 2, RoundingMode.HALF_UP);
            return "Média global de notas: " + avg + " (Total de notas analisadas: " + grades.size() + ")";
        });
    }

    @Tool(description = "Identifica matérias com pior desempenho e seus professores")
    public String getSubjectPerformanceRanking() {
        return transactionTemplate.execute(status -> {
            AcademicYear year = yearService.findActive();
            if (year == null) return "Sem ano letivo ativo.";
            var grades = gradeRepository.findByAcademicYear(year).stream()
                .filter(g -> g.getEffectiveValue() != null)
                .collect(Collectors.toList());
            Map<Integer, List<Grade>> gradesByTcs = grades.stream()
                .collect(Collectors.groupingBy(g -> g.getAssessment().getTeacherClassSubject().getId()));
            StringBuilder sb = new StringBuilder();
            gradesByTcs.forEach((tcsId, list) -> {
                BigDecimal sum = list.stream().map(Grade::getEffectiveValue).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal avg = sum.divide(new BigDecimal(list.size()), 2, RoundingMode.HALF_UP);
                var tcs = list.get(0).getAssessment().getTeacherClassSubject();
                sb.append("Matéria: ").append(tcs.getSubject().getName())
                  .append(" | Turma: ").append(tcs.getClassRoom().getName())
                  .append(" | Prof: ").append(tcs.getTeacher().getName())
                  .append(" | Média: ").append(avg).append("\n");
            });
            return sb.isEmpty() ? "Sem notas para analisar." : sb.toString();
        });
    }

    private boolean isAssignmentActivelyTeaching(TeacherClassSubject tcs) {
        if (tcs == null || !tcs.isActive()) return false;
        if (tcs.getTeacher() == null || !tcs.getTeacher().isActive()) return false;
        if (tcs.getClassRoom() == null || !tcs.getClassRoom().isActive()) return false;
        return tcs.getClassRoom().getAcademicYear() == null || tcs.getClassRoom().getAcademicYear().isActive();
    }

    @Tool(description = "Conta quantos professores existem por matéria, detalhando quantos estão lecionando ativamente e quantos estão habilitados no cadastro")
    public String getTeacherCountBySubject(String subjectName) {
        return transactionTemplate.execute(status -> {
            if (subjectName != null && !subjectName.trim().isEmpty()) {
                var subjectOpt = subjectRepository.findByNameIgnoreCase(subjectName.trim());
                if (subjectOpt.isEmpty()) return "Matéria não encontrada: " + subjectName;
                Subject subject = subjectOpt.get();
                Set<Integer> activeTeachingTeacherIds = new HashSet<>();
                for (var tcs : tcsRepository.findBySubject(subject)) {
                    if (isAssignmentActivelyTeaching(tcs)) {
                        activeTeachingTeacherIds.add(tcs.getTeacher().getId());
                    }
                }
                Set<Integer> qualifiedTeacherIds = new HashSet<>();
                for (var teacher : teacherRepository.findAll()) {
                    if (teacher.isActive() && teacher.getSubjects() != null && teacher.getSubjects().contains(subject)) {
                        qualifiedTeacherIds.add(teacher.getId());
                    }
                }
                return String.format("Matéria %s:\n- Professores lecionando ativamente: %d\n- Professores habilitados no cadastro: %d",
                        subject.getName(), activeTeachingTeacherIds.size(), qualifiedTeacherIds.size());
            } else {
                var allTcs = tcsRepository.findAll();
                Map<String, Set<Integer>> activeTeaching = new HashMap<>();
                for (var tcs : allTcs) {
                    if (isAssignmentActivelyTeaching(tcs)) {
                        activeTeaching.computeIfAbsent(tcs.getSubject().getName(), k -> new HashSet<>())
                                .add(tcs.getTeacher().getId());
                    }
                }
                Map<String, Set<Integer>> qualified = new HashMap<>();
                for (var teacher : teacherRepository.findAll()) {
                    if (teacher.isActive() && teacher.getSubjects() != null) {
                        for (var subject : teacher.getSubjects()) {
                            qualified.computeIfAbsent(subject.getName(), k -> new HashSet<>())
                                    .add(teacher.getId());
                        }
                    }
                }
                Set<String> allSubjects = new TreeSet<>();
                allSubjects.addAll(activeTeaching.keySet());
                allSubjects.addAll(qualified.keySet());

                return allSubjects.stream()
                        .map(sub -> String.format("Matéria: %s -> Lecionando ativamente: %d | Habilitados no cadastro: %d",
                                sub,
                                activeTeaching.getOrDefault(sub, Set.of()).size(),
                                qualified.getOrDefault(sub, Set.of()).size()))
                        .collect(Collectors.joining("\n"));
            }
        });
    }

    @Tool(description = "Conta o total de alunos ativos e turmas, e calcula a média de alunos por turma")
    public String getTotalStudentsAndClassrooms() {
        return transactionTemplate.execute(status -> {
            AcademicYear year = yearService.findActive();
            if (year == null) return "Sem ano letivo ativo.";
            var enrollments = enrollmentService.findAllActive().stream()
                .filter(e -> e.getClassRoom().getAcademicYear().getId().equals(year.getId()))
                .collect(Collectors.toList());
            long activeStudents = enrollments.stream().map(e -> e.getStudent().getId()).distinct().count();
            var classrooms = classRoomService.findByAcademicYear(year.getId());
            long totalClassrooms = classrooms.size();
            double avg = totalClassrooms == 0 ? 0 : (double) activeStudents / totalClassrooms;
            return String.format("Total de Alunos Ativos: %d\nTotal de Turmas: %d\nMédia de alunos por turma: %.2f", 
                                 activeStudents, totalClassrooms, avg);
        });
    }

    @Tool(description = "Executa consultas SQL (PostgreSQL) no banco de dados para análises customizadas. Útil para descobrir 'quem é o aluno com a pior nota', cruzar dados não previstos e responder análises gerais. Retorna até 50 resultados.")
    @Transactional(readOnly = true, timeout = 5)
    public String executeFlexibleSql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return "Erro de validação: Instrução SQL não informada.";
        }

        String trimmedSql = sql.trim();
        while (trimmedSql.endsWith(";")) {
            trimmedSql = trimmedSql.substring(0, trimmedSql.length() - 1).trim();
        }

        if (trimmedSql.contains(";")) {
            return "Erro de segurança: Múltiplas instruções SQL em um único comando não são permitidas.";
        }

        String upperSql = trimmedSql.toUpperCase(Locale.ROOT);
        if (!upperSql.startsWith("SELECT") && !upperSql.startsWith("WITH")) {
            return "Erro de segurança: Apenas consultas de leitura iniciadas com SELECT ou WITH são permitidas.";
        }

        if (FORBIDDEN_SQL_PATTERN.matcher(trimmedSql).find()) {
            return "Erro de segurança: Comando SQL rejeitado por conter palavra-chave restrita de modificação de dados.";
        }

        if (SENSITIVE_SQL_PATTERN.matcher(trimmedSql).find()) {
            return "Erro de segurança: Acesso a tabelas, colunas ou funções restritas do sistema não é permitido.";
        }

        final String finalSql = trimmedSql;
        return transactionTemplate.execute(status -> {
            status.setRollbackOnly();
            try {
                try {
                    jdbcTemplate.execute("SET TRANSACTION READ ONLY");
                } catch (Exception ignored) {
                    // Ignora caso o dialeto ou driver já esteja em transação ativa sem suporte a re-set
                }
                List<Map<String, Object>> results = jdbcTemplate.queryForList(finalSql);
                if (results.isEmpty()) return "Nenhum resultado encontrado.";
                if (results.size() > 50) return "Foram encontrados " + results.size() + " resultados. Mostrando os 50 primeiros:\n" + results.subList(0, 50).toString();
                return results.toString();
            } catch (Exception e) {
                return "Erro ao executar consulta SQL: " + e.getMessage();
            }
        });
    }

    @Tool(description = "Elabora uma minuta de comunicado/aviso oficial para a escola")
    public String draftAnnouncement(String subject, String targetAudience, String keyPoints, String tone) {
        return transactionTemplate.execute(status -> {
            SchoolSettings settings = schoolSettingsService.findSettings();
            String schoolName = (settings != null && settings.getSchoolName() != null)
                    ? settings.getSchoolName()
                    : "Escola Web";

            String resolvedTone = (tone != null && !tone.isBlank()) ? tone : "formal e acolhedor";
            String resolvedAudience = (targetAudience != null && !targetAudience.isBlank()) ? targetAudience : "Toda a comunidade escolar";
            String resolvedSubject = (subject != null && !subject.isBlank()) ? subject : "Comunicado Oficial";

            StringBuilder sb = new StringBuilder();
            sb.append("=== MINUTA DE COMUNICADO OFICIAL ===\n\n");
            sb.append("Instituição: ").append(schoolName).append("\n");
            sb.append("Data de Emissão: ").append(java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n");
            sb.append("Assunto: ").append(resolvedSubject).append("\n");
            sb.append("Público-alvo: ").append(resolvedAudience).append("\n");
            sb.append("Tom do Comunicado: ").append(resolvedTone).append("\n\n");
            sb.append("--- TÍTULO SUGERIDO ---\n");
            sb.append(resolvedSubject.toUpperCase(Locale.ROOT)).append(" - ").append(schoolName).append("\n\n");
            sb.append("--- CORPO DO COMUNICADO ---\n");
            sb.append("Prezados(as) ").append(resolvedAudience).append(",\n\n");
            sb.append("Vimos por meio deste comunicar que:\n");
            if (keyPoints != null && !keyPoints.isBlank()) {
                String[] points = keyPoints.split("[;\\n]+");
                for (String p : points) {
                    if (!p.trim().isEmpty()) {
                        sb.append("• ").append(p.trim()).append("\n");
                    }
                }
            } else {
                sb.append("• ").append(resolvedSubject).append("\n");
            }
            sb.append("\nReforçamos nosso compromisso com a excelência educacional e estamos à disposição para eventuais esclarecimentos.\n\n");
            sb.append("Atenciosamente,\n");
            sb.append("Equipe Gestora e Direção Escolar\n");
            sb.append(schoolName).append("\n");
            sb.append("====================================");

            return sb.toString();
        });
    }

    @Tool(description = "Retorna o schema do banco de dados (tabelas e colunas do schema 'public') para ajudar a montar consultas SQL na ferramenta executeFlexibleSql.")
    public String getDatabaseSchema() {
        return transactionTemplate.execute(status -> {
            try {
                String sql = "SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_SCHEMA = 'public' " +
                        "AND LOWER(TABLE_NAME) NOT IN ('password_reset_tokens', 'refresh_tokens', 'flyway_schema_history', 'chat_message_history') " +
                        "AND NOT (LOWER(TABLE_NAME) = 'users' AND LOWER(COLUMN_NAME) = 'password') " +
                        "ORDER BY TABLE_NAME, ORDINAL_POSITION";
                List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
                if (results.isEmpty()) return "Schema public não possui tabelas visíveis ou não encontrado.";
                List<Map<String, Object>> filteredResults = results.stream()
                        .filter(row -> {
                            Object tblVal = row.get("TABLE_NAME") != null ? row.get("TABLE_NAME") : row.get("table_name");
                            Object colVal = row.get("COLUMN_NAME") != null ? row.get("COLUMN_NAME") : row.get("column_name");
                            String tbl = String.valueOf(tblVal).toLowerCase(Locale.ROOT);
                            String col = String.valueOf(colVal).toLowerCase(Locale.ROOT);
                            if (tbl.equals("password_reset_tokens") || tbl.equals("refresh_tokens") ||
                                    tbl.equals("flyway_schema_history") || tbl.equals("chat_message_history")) {
                                return false;
                            }
                            if (tbl.equals("users") && col.equals("password")) {
                                return false;
                            }
                            return true;
                        })
                        .collect(Collectors.toList());
                return filteredResults.toString();
            } catch (Exception e) {
                return "Erro ao buscar schema do banco: " + e.getMessage();
            }
        });
    }
}
