package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.model.entity.*;
import com.andretti101.escolaweb.repository.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherAiTools {

    private final TransactionTemplate transactionTemplate;
    private final AuthenticatedUserService authService;
    private final TeacherClassSubjectService tcsService;
    private final StudentRepository studentRepository;
    private final AssessmentRepository assessmentRepository;
    private final GradeRepository gradeRepository;
    private final AcademicYearService yearService;
    private final AcademicPeriodService periodService;
    private final SchoolSettingsService schoolSettingsService;
    private final TeacherService teacherService;

    @Tool(description = "Lista todas as turmas e matérias que o professor logado leciona")
    public String getTeacherClasses() {
        return transactionTemplate.execute(status -> {
            Teacher teacher = authService.getAuthenticatedTeacher();
            var classes = tcsService.findByTeacher(teacher.getId());
            if (classes != null) {
                classes = classes.stream()
                        .filter(tcs -> tcs.isActive() && (tcs.getClassRoom() == null || tcs.getClassRoom().getAcademicYear() == null || tcs.getClassRoom().getAcademicYear().isActive()))
                        .toList();
            }
            if (classes == null || classes.isEmpty()) return "Você não tem turmas atribuídas.";
            return classes.stream().map(tcs -> 
                "Turma: " + (tcs.getClassRoom() != null ? tcs.getClassRoom().getName() : "Sem Turma") + " - Matéria: " + (tcs.getSubject() != null ? tcs.getSubject().getName() : "Sem Matéria")
            ).collect(Collectors.joining("\n"));
        });
    }

    @Tool(description = "Mostra os alunos de uma turma pelo seu classRoomId")
    public String getTeacherClassStudents(Integer classRoomId) {
        return transactionTemplate.execute(status -> {
            Teacher teacher = authService.getAuthenticatedTeacher();
            var classes = tcsService.findByTeacher(teacher.getId());
            boolean hasAccess = classes.stream().anyMatch(t -> t.getClassRoom().getId().equals(classRoomId));
            if (!hasAccess) return "Acesso negado: Você não leciona nesta turma.";
            var students = studentRepository.findByClassroomId(classRoomId);
            return "Alunos na turma:\n" + students.stream().map(Student::getName).collect(Collectors.joining("\n"));
        });
    }

    @Tool(description = "Mostra os piores desempenhos nas matérias do professor")
    public String getTeacherStudentPerformance() {
        return transactionTemplate.execute(status -> {
            Teacher teacher = authService.getAuthenticatedTeacher();
            var classes = tcsService.findByTeacher(teacher.getId());
            if (classes != null) {
                classes = classes.stream()
                        .filter(tcs -> tcs.isActive() && (tcs.getClassRoom() == null || tcs.getClassRoom().getAcademicYear() == null || tcs.getClassRoom().getAcademicYear().isActive()))
                        .toList();
            }
            StringBuilder sb = new StringBuilder();
            if (classes != null) {
                for (var tcs : classes) {
                    sb.append("Turma: ").append(tcs.getClassRoom() != null ? tcs.getClassRoom().getName() : "Sem Turma")
                      .append(" - ").append(tcs.getSubject() != null ? tcs.getSubject().getName() : "Sem Matéria").append("\n");
                    var assessments = assessmentRepository.findByTeacherClassSubject(tcs);
                    for (var a : assessments) {
                        var grades = gradeRepository.findByAssessment(a);
                        var badGrades = grades.stream()
                            .filter(g -> g.getEffectiveValue() != null && g.getEffectiveValue().compareTo(new BigDecimal("6.0")) < 0)
                            .collect(Collectors.toList());
                        if (!badGrades.isEmpty()) {
                            sb.append("  Avaliação: ").append(a.getTitle()).append(" - Alunos com nota baixa:\n");
                            badGrades.forEach(g -> sb.append("    - ").append(g.getStudent().getName()).append(": ").append(g.getEffectiveValue()).append("\n"));
                        }
                    }
                }
            }
            if (sb.isEmpty()) return "Nenhum aluno com baixo desempenho.";
            return sb.toString();
        });
    }

    @Tool(description = "Verifica avaliações pendentes por turma/período")
    public String getTeacherPendingAssessments() {
        return transactionTemplate.execute(status -> {
            Teacher teacher = authService.getAuthenticatedTeacher();
            AcademicYear year = yearService.findActive();
            if (year == null) return "Sem ano letivo ativo.";
            
            var periods = periodService.findByAcademicYear(year.getId());
            var classes = tcsService.findByTeacher(teacher.getId());
            if (classes != null) {
                classes = classes.stream()
                        .filter(tcs -> tcs.isActive() && (tcs.getClassRoom() == null || tcs.getClassRoom().getAcademicYear() == null || tcs.getClassRoom().getAcademicYear().isActive()))
                        .toList();
            }
            StringBuilder sb = new StringBuilder();
            if (classes != null) {
                for (var tcs : classes) {
                    for (var period : periods) {
                        if (period.getPeriodType() != null && tcs.getClassRoom() != null && tcs.getClassRoom().getPeriodType() != null && period.getPeriodType() != tcs.getClassRoom().getPeriodType()) {
                            continue;
                        }
                        long count = assessmentRepository.countByTeacherClassSubjectAndPeriod(tcs, period);
                        long min = tcs.getMinAssessmentsPerPeriod();
                        if (count < min) {
                            sb.append("Turma: ").append(tcs.getClassRoom() != null ? tcs.getClassRoom().getName() : "Sem Turma")
                              .append(", Matéria: ").append(tcs.getSubject() != null ? tcs.getSubject().getName() : "Sem Matéria")
                              .append(", Período: ").append(period.getName())
                              .append(" - Pendentes: ").append(min - count).append(" avaliações.\n");
                        }
                    }
                }
            }
            return sb.length() > 0 ? sb.toString() : "Todas as avaliações mínimas foram criadas.";
        });
    }

    @Tool(description = "Lista os outros professores da escola (colegas)")
    public String getOtherTeachers() {
        return transactionTemplate.execute(status -> {
            Teacher teacher = authService.getAuthenticatedTeacher();
            var teachers = teacherService.findAllActive().stream()
                .filter(t -> !t.getId().equals(teacher.getId()))
                .map(Teacher::getName)
                .collect(Collectors.toList());
            if (teachers.isEmpty()) return "Nenhum outro professor ativo encontrado.";
            return "Você tem " + teachers.size() + " colegas professores: " + String.join(", ", teachers);
        });
    }

    @Tool(description = "Obtém o ano letivo atual e seus períodos acadêmicos (com datas de início e fim)")
    public String getAcademicYearAndPeriods() {
        return transactionTemplate.execute(status -> {
            AcademicYear year = yearService.findActive();
            if (year == null) return "Nenhum ano letivo ativo encontrado.";
            
            var periods = periodService.findByAcademicYear(year.getId());
            String periodsInfo = periods.stream()
                    .map(p -> p.getName() + " (De " + p.getStartDate() + " até " + p.getEndDate() + ")")
                    .collect(Collectors.joining("; "));
            
            return "Ano Letivo: " + year.getYear() + " (De " + year.getStartDate() + " até " + year.getEndDate() + ")\nPeríodos: " + (periodsInfo.isEmpty() ? "Nenhum período cadastrado." : periodsInfo);
        });
    }

    @Tool(description = "Obtém as configurações escolares definidas pela escola")
    public String getSchoolSettings() {
        return transactionTemplate.execute(status -> {
            SchoolSettings settings = schoolSettingsService.findSettings();
            if (settings == null) return "Configurações escolares não encontradas.";
            return "Escola: " + settings.getSchoolName() +
                   "\nMédia mínima para aprovação: " + settings.getMinimumGrade() +
                   "\nFrequência mínima (%): " + settings.getMinimumAttendance() +
                   "\nDivisão de períodos: varia por turma (cada turma possui sua divisão)";
        });
    }

    @Tool(description = "Gera uma proposta de plano de aula estruturado alinhado à BNCC para a disciplina e ano escolar")
    public String suggestLessonPlan(String subjectName, String gradeLevel, String topic, String objectives) {
        return transactionTemplate.execute(status -> {
            SchoolSettings settings = schoolSettingsService.findSettings();
            String schoolName = (settings != null && settings.getSchoolName() != null)
                    ? settings.getSchoolName()
                    : "Escola Web";

            String resolvedSubject = (subjectName != null && !subjectName.isBlank()) ? subjectName : "Disciplina Geral";
            String resolvedGrade = (gradeLevel != null && !gradeLevel.isBlank()) ? gradeLevel : "Ensino Fundamental / Médio";
            String resolvedTopic = (topic != null && !topic.isBlank()) ? topic : "Tema Central";
            String resolvedObjectives = (objectives != null && !objectives.isBlank()) ? objectives : "Desenvolver a compreensão teórica e prática do tema.";

            StringBuilder sb = new StringBuilder();
            sb.append("=== PROPOSTA DE PLANO DE AULA (ALINHADO À BNCC) ===\n\n");
            sb.append("1. DADOS DE IDENTIFICAÇÃO:\n");
            sb.append("   • Instituição: ").append(schoolName).append("\n");
            sb.append("   • Disciplina: ").append(resolvedSubject).append("\n");
            sb.append("   • Ano/Série: ").append(resolvedGrade).append("\n");
            sb.append("   • Tema da Aula: ").append(resolvedTopic).append("\n");
            sb.append("   • Duração Estimada: 50 minutos\n\n");

            sb.append("2. OBJETIVOS DE APRENDIZAGEM:\n");
            sb.append("   • Objetivo Geral: ").append(resolvedObjectives).append("\n");
            sb.append("   • Objetivos Específicos:\n");
            sb.append("     - Identificar os conceitos e fundamentos centrais de ").append(resolvedTopic).append(";\n");
            sb.append("     - Relacionar o conteúdo teórico com situações práticas do cotidiano e pensamento crítico;\n");
            sb.append("     - Resolver exercícios e formular conclusões embasadas sobre a temática.\n\n");

            sb.append("3. COMPETÊNCIAS E HABILIDADES BNCC:\n");
            sb.append("   • Competência Geral: Exercitar a curiosidade intelectual e recorrer à abordagem própria das ciências, incluindo a investigação, a reflexão, a análise crítica e a imaginação;\n");
            sb.append("   • Habilidade Correlata: Compreender, aplicar e analisar conceitos específicos da área de conhecimento de ").append(resolvedSubject).append(" na resolução de problemas complexos.\n\n");

            sb.append("4. DESENVOLVIMENTO METODOLÓGICO (50 min):\n");
            sb.append("   • [00-10 min] Acolhida e Levantamento Prévio: Questão disparadora e sondagem inicial dos conhecimentos prévios sobre ").append(resolvedTopic).append(";\n");
            sb.append("   • [10-30 min] Mediação Teórico-Expositiva Dialogada: Apresentação e debate dos conceitos-chave com recursos visuais;\n");
            sb.append("   • [30-45 min] Atividade Prática / Investigativa: Dinâmica em grupos para fixação prática e aplicação colaborativa;\n");
            sb.append("   • [45-50 min] Síntese e Fechamento: Roda de fechamento dos aprendizados e direcionamento para a próxima aula.\n\n");

            sb.append("5. RECURSOS DIDÁTICOS:\n");
            sb.append("   • Quadro/projetor, material didático impresso ou digital, e recursos multimídia.\n\n");

            sb.append("6. AVALIAÇÃO FORMATIVA:\n");
            sb.append("   • Participação ativa nas discussões, engajamento na atividade prática e verificação contínua do aprendizado.\n");
            sb.append("==================================================");

            return sb.toString();
        });
    }

    @Tool(description = "Gera um rascunho de questões de prova/teste sobre um assunto específico com gabarito comentado")
    public String generateExamQuestions(String subjectName, String topic, int count, String questionType) {
        return transactionTemplate.execute(status -> {
            String resolvedSubject = (subjectName != null && !subjectName.isBlank()) ? subjectName : "Disciplina Geral";
            String resolvedTopic = (topic != null && !topic.isBlank()) ? topic : "Tema Geral";
            int resolvedCount = Math.min(Math.max(count > 0 ? count : 3, 1), 20);
            String resolvedType = (questionType != null && !questionType.isBlank()) ? questionType.toUpperCase(java.util.Locale.ROOT) : "MÚLTIPLA ESCOLHA";

            StringBuilder sb = new StringBuilder();
            sb.append("=== RASCUNHO DE QUESTÕES DE AVALIAÇÃO COM GABARITO COMENTADO ===\n\n");
            sb.append("Disciplina: ").append(resolvedSubject).append("\n");
            sb.append("Assunto: ").append(resolvedTopic).append("\n");
            sb.append("Quantidade de Questões: ").append(resolvedCount).append("\n");
            sb.append("Tipo de Questão: ").append(resolvedType).append("\n\n");
            sb.append("--- CADERNO DE QUESTÕES ---\n\n");

            boolean isDiscursive = resolvedType.contains("DISCURSIV");

            for (int i = 1; i <= resolvedCount; i++) {
                sb.append("Questão ").append(i).append(" (Valor: 1,0 ponto)\n");
                sb.append("Enunciado: Com base nos estudos de ").append(resolvedSubject).append(" acerca de ")
                  .append(resolvedTopic).append(", analise a situação contextualizada referente ao tópico (Aspecto #").append(i).append(") e responda:\n");

                if (isDiscursive) {
                    sb.append("Desenvolva um texto explicativo abordando os principais mecanismos e implicações práticas deste conceito.\n");
                    sb.append("[Espaço para resposta do aluno - mínimo 5 linhas]\n\n");
                } else {
                    sb.append("A) Proposição abordando o conceito de forma incorreta ou incompleta.\n");
                    sb.append("B) Proposição distratora com terminologia plausível mas conclusão equivocada.\n");
                    sb.append("C) Proposição CORRETA que sintetiza adequadamente o conceito de ").append(resolvedTopic).append(".\n");
                    sb.append("D) Proposição distratora generalista e dissociada do contexto apresentado.\n\n");
                }
            }

            sb.append("--- GABARITO COMENTADO E CRITÉRIOS DE CORREÇÃO (PARA O PROFESSOR) ---\n\n");
            for (int i = 1; i <= resolvedCount; i++) {
                sb.append("Gabarito Questão ").append(i).append(":\n");
                if (isDiscursive) {
                    sb.append("• Expectativa de Resposta: O estudante deve definir com clareza o conceito de ")
                      .append(resolvedTopic).append(", citando ao menos dois aspectos fundamentais e demonstrando raciocínio crítico.\n");
                    sb.append("• Critérios de Pontuação: 0,5 pela conceituação correta; 0,3 pela contextualização/exemplificação; 0,2 pela clareza e coesão textual.\n\n");
                } else {
                    sb.append("• Alternativa Correta: C\n");
                    sb.append("• Justificativa Pedagógica: A alternativa C contempla com precisão a fundamentação teórica de ")
                      .append(resolvedTopic).append(". As opções A, B e D funcionam como distratores baseados em equívocos conceituais comuns.\n\n");
                }
            }
            sb.append("=================================================================");

            return sb.toString();
        });
    }
}
