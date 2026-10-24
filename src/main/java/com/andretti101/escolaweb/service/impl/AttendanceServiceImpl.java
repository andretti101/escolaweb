package com.andretti101.escolaweb.service.impl;

import com.andretti101.escolaweb.model.entity.Attendance;
import com.andretti101.escolaweb.model.entity.AttendanceHistory;
import com.andretti101.escolaweb.model.entity.Lesson;
import com.andretti101.escolaweb.model.entity.Student;
import com.andretti101.escolaweb.model.entity.TeacherClassSubject;
import com.andretti101.escolaweb.model.enums.AttendanceStatus;
import com.andretti101.escolaweb.repository.AttendanceHistoryRepository;
import com.andretti101.escolaweb.repository.AttendanceRepository;
import com.andretti101.escolaweb.repository.EnrollmentRepository;
import com.andretti101.escolaweb.repository.LessonRepository;
import com.andretti101.escolaweb.service.AttendanceService;
import com.andretti101.escolaweb.service.LessonService;
import com.andretti101.escolaweb.service.StudentService;
import com.andretti101.escolaweb.service.TeacherClassSubjectService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final AttendanceHistoryRepository attendanceHistoryRepository;
    private final com.andretti101.escolaweb.service.AuthenticatedUserService authenticatedUserService;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonRepository lessonRepository;
    private final LessonService lessonService;
    private final StudentService studentService;
    private final TeacherClassSubjectService teacherClassSubjectService;
    private final com.andretti101.escolaweb.service.NotificationPublisherService notificationPublisherService;

    @Override
    @Transactional
    public Attendance register(Attendance attendance) {
        Lesson lesson = resolveLesson(attendance.getLesson());
        Student student = resolveStudent(attendance.getStudent());

        if (attendance.getStatus() == null) {
            throw new IllegalArgumentException("O status da frequência é obrigatório.");
        }
        // Impede lancamento duplicado de frequencia para o mesmo discente na mesma aula
        if (attendanceRepository.existsByLessonAndStudent(lesson, student)) {
            throw new IllegalStateException(
                    "A frequência do aluno '" + student.getName() + "' nesta aula já foi registrada.");
        }

        validateLessonYearActive(lesson);
        validateActiveEnrollment(student, lesson.getTeacherClassSubject());

        attendance.setLesson(lesson);
        attendance.setStudent(student);

        Attendance saved = attendanceRepository.save(attendance);

        recordHistory(saved, null, saved.getStatus());

        dispatchAttendanceNotification(saved, "Frequência Registrada");

        return saved;
    }

    @Override
    @Transactional
    public Attendance update(Integer id, Attendance incoming) {
        Attendance existing = findAttendanceOrThrow(id);

        if (incoming.getStatus() == null) {
            throw new IllegalArgumentException("O status da frequência é obrigatório.");
        }

        validateLessonYearActive(existing.getLesson());

        AttendanceStatus previous = existing.getStatus();

        existing.setStatus(incoming.getStatus());

        // Registra historico de auditoria caso o status da presenca seja alterado (ex: falta para justificada)
        if (previous != incoming.getStatus()) {
            recordHistory(existing, previous, existing.getStatus());
            dispatchAttendanceNotification(existing, "Frequência Atualizada");
        }

        return existing;
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        Attendance existing = findAttendanceOrThrow(id);
        validateLessonYearActive(existing.getLesson());
        attendanceRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Attendance findById(Integer id) {
        return findAttendanceOrThrow(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Attendance> findAll() {
        return attendanceRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Attendance> findAll(org.springframework.data.domain.Pageable pageable) {
        return attendanceRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Attendance> findByTeacherActiveAcademicYear(Integer teacherId, org.springframework.data.domain.Pageable pageable) {
        return attendanceRepository.findByTeacherActiveAcademicYear(teacherId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Attendance> findByLesson(Integer lessonId) {
        Lesson lesson = lessonService.findById(lessonId);
        return attendanceRepository.findByLesson(lesson);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Attendance> findByStudent(Integer studentId) {
        Student student = studentService.findById(studentId);
        return attendanceRepository.findByStudent(student);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateFrequency(Integer studentId, Integer teacherClassSubjectId) {
        Student student = studentService.findById(studentId);
        TeacherClassSubject tcs = teacherClassSubjectService.findById(teacherClassSubjectId);

        List<Lesson> lessons = lessonRepository.findByTeacherClassSubject(tcs);
        if (lessons.isEmpty()) {
            return BigDecimal.valueOf(100);
        }

        List<Attendance> attendances = attendanceRepository.findByStudent(student).stream()
                .filter(a -> a.getLesson().getTeacherClassSubject().getId().equals(teacherClassSubjectId))
                .toList();

        int presencaValida = 0;
        int faltasNaoJustificadas = 0;

        for (Attendance a : attendances) {
            int count = a.getLesson().getLessonCount() != null ? a.getLesson().getLessonCount().getValue() : 1;
            if (a.getStatus() == AttendanceStatus.PRESENT || a.getStatus() == AttendanceStatus.JUSTIFIED_ABSENCE) {
                presencaValida += count;
            } else if (a.getStatus() == AttendanceStatus.ABSENT) {
                faltasNaoJustificadas += count;
            }
        }

        int totalAulas = presencaValida + faltasNaoJustificadas;
        if (totalAulas == 0) {
            return BigDecimal.valueOf(100);
        }

        return BigDecimal.valueOf(presencaValida)
                .divide(BigDecimal.valueOf(totalAulas), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional(readOnly = true)
    public com.andretti101.escolaweb.dto.response.StudentAttendanceReportDTO getStudentAttendanceReport(Integer studentId, Integer yearId) {
        Student student = studentService.findById(studentId);
        List<Attendance> attendances = attendanceRepository.findByStudent(student);

        Map<String, Integer> validPresencePerSubject = new java.util.HashMap<>();
        Map<String, Integer> unjustifiedAbsencesPerSubject = new java.util.HashMap<>();

        int totalValidPresence = 0;
        int totalUnjustifiedAbsences = 0;

        for (Attendance a : attendances) {
            if (a.getLesson() == null || 
                a.getLesson().getTeacherClassSubject() == null || 
                a.getLesson().getTeacherClassSubject().getClassRoom() == null || 
                a.getLesson().getTeacherClassSubject().getClassRoom().getAcademicYear() == null) {
                continue;
            }

            if (yearId != null) {
                if (!a.getLesson().getTeacherClassSubject().getClassRoom().getAcademicYear().getId().equals(yearId)) {
                    continue;
                }
            } else {
                if (!a.getLesson().getTeacherClassSubject().getClassRoom().getAcademicYear().isActive()) {
                    continue;
                }
            }
            String subjectName = a.getLesson().getTeacherClassSubject().getSubject().getName();
            int count = a.getLesson().getLessonCount() != null ? a.getLesson().getLessonCount().getValue() : 1;

            if (a.getStatus() == AttendanceStatus.PRESENT || a.getStatus() == AttendanceStatus.JUSTIFIED_ABSENCE) {
                validPresencePerSubject.put(subjectName, validPresencePerSubject.getOrDefault(subjectName, 0) + count);
                totalValidPresence += count;
            } else if (a.getStatus() == AttendanceStatus.ABSENT) {
                unjustifiedAbsencesPerSubject.put(subjectName, unjustifiedAbsencesPerSubject.getOrDefault(subjectName, 0) + count);
                totalUnjustifiedAbsences += count;
            }
        }

        Map<String, Integer> absencesPerSubject = new java.util.HashMap<>(unjustifiedAbsencesPerSubject);
        Map<String, BigDecimal> frequencyPerSubject = new java.util.HashMap<>();

        java.util.Set<String> allSubjects = new java.util.HashSet<>();
        allSubjects.addAll(validPresencePerSubject.keySet());
        allSubjects.addAll(unjustifiedAbsencesPerSubject.keySet());

        for (String subject : allSubjects) {
            int valid = validPresencePerSubject.getOrDefault(subject, 0);
            int unjustified = unjustifiedAbsencesPerSubject.getOrDefault(subject, 0);
            int total = valid + unjustified;

            BigDecimal freq;
            if (total == 0) {
                freq = BigDecimal.valueOf(100);
            } else {
                freq = BigDecimal.valueOf(valid)
                        .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
            }
            frequencyPerSubject.put(subject, freq);
        }

        int overallTotal = totalValidPresence + totalUnjustifiedAbsences;
        BigDecimal generalFrequency;
        if (overallTotal == 0) {
            generalFrequency = BigDecimal.valueOf(100);
        } else {
            generalFrequency = BigDecimal.valueOf(totalValidPresence)
                    .divide(BigDecimal.valueOf(overallTotal), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return new com.andretti101.escolaweb.dto.response.StudentAttendanceReportDTO(
                studentId,
                generalFrequency,
                absencesPerSubject,
                frequencyPerSubject
        );
    }


    private void recordHistory(Attendance attendance, AttendanceStatus previous, AttendanceStatus current) {
        AttendanceHistory history = AttendanceHistory.builder()
                .attendance(attendance)
                .previousStatus(previous != null ? previous.name() : null)
                .newStatus(current != null ? current.name() : null)
                .changedBy(authenticatedUserService.findAuthenticatedUserOrNull())
                .build();
        attendanceHistoryRepository.save(history);
    }

    Attendance findAttendanceOrThrow(Integer id) {
        return attendanceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Registro de frequência não encontrado."));
    }

    private void validateLessonYearActive(Lesson lesson) {
        if (lesson != null && lesson.getTeacherClassSubject() != null
                && lesson.getTeacherClassSubject().getClassRoom() != null
                && lesson.getTeacherClassSubject().getClassRoom().getAcademicYear() != null
                && !lesson.getTeacherClassSubject().getClassRoom().getAcademicYear().isActive()) {
            throw new IllegalStateException(
                    "O ano letivo '" + lesson.getTeacherClassSubject().getClassRoom().getAcademicYear().getYear()
                    + "' não está ativo. Não é permitido registrar, alterar ou excluir frequências de anos letivos encerrados.");
        }
    }

    private void validateActiveEnrollment(Student student, TeacherClassSubject tcs) {
        if (!enrollmentRepository.existsByStudentAndClassRoomAndActiveTrue(student, tcs.getClassRoom())) {
            throw new IllegalStateException(
                    "O aluno '" + student.getName() + "' não possui matrícula ativa na turma '"
                    + tcs.getClassRoom().getName() + "'. Apenas alunos com matrícula ativa podem ter presença registrada.");
        }
    }

    private Lesson resolveLesson(Lesson ref) {
        if (ref == null || ref.getId() == null) {
            throw new IllegalArgumentException("A identificação da aula é obrigatória.");
        }
        return lessonService.findById(ref.getId());
    }

    private Student resolveStudent(Student ref) {
        if (ref == null || ref.getId() == null) {
            throw new IllegalArgumentException("A seleção do aluno é obrigatória.");
        }
        return studentService.findById(ref.getId());
    }

    private void dispatchAttendanceNotification(Attendance attendance, String title) {
        if (notificationPublisherService == null || attendance.getStudent() == null) return;
        try {
            Lesson lesson = attendance.getLesson();
            String subjectName = (lesson != null && lesson.getTeacherClassSubject() != null && lesson.getTeacherClassSubject().getSubject() != null)
                    ? lesson.getTeacherClassSubject().getSubject().getName()
                    : "Disciplina";
            String statusDesc = attendance.getStatus() == AttendanceStatus.PRESENT ? "Presença" :
                    (attendance.getStatus() == AttendanceStatus.ABSENT ? "Falta" : "Falta Justificada");
            String dateStr = (lesson != null && lesson.getLessonDate() != null)
                    ? lesson.getLessonDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    : "";

            com.andretti101.escolaweb.dto.response.NotificationMessageDTO notif =
                    com.andretti101.escolaweb.dto.response.NotificationMessageDTO.builder()
                            .id(java.util.UUID.randomUUID().toString())
                            .type("ATTENDANCE_RECORDED")
                            .title(title != null ? title : "Frequência Registrada")
                            .message(String.format("Registro de %s em %s%s", statusDesc, subjectName, dateStr.isEmpty() ? "" : " (Aula de " + dateStr + ")"))
                            .linkUrl("/aluno/frequencias")
                            .timestamp(java.time.LocalDateTime.now())
                            .build();

            notificationPublisherService.notifyStudent(attendance.getStudent().getId(), notif);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(AttendanceServiceImpl.class)
                    .warn("Erro ao publicar notificação de frequência: {}", e.getMessage());
        }
    }
}
