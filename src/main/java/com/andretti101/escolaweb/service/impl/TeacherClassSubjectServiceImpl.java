package com.andretti101.escolaweb.service.impl;

import com.andretti101.escolaweb.model.entity.ClassRoom;
import com.andretti101.escolaweb.model.entity.Subject;
import com.andretti101.escolaweb.model.entity.Teacher;
import com.andretti101.escolaweb.model.entity.TeacherClassSubject;
import com.andretti101.escolaweb.repository.AssessmentRepository;
import com.andretti101.escolaweb.repository.LessonRepository;
import com.andretti101.escolaweb.repository.TeacherClassSubjectRepository;
import com.andretti101.escolaweb.service.ClassRoomService;
import com.andretti101.escolaweb.service.SubjectService;
import com.andretti101.escolaweb.service.TeacherClassSubjectService;
import com.andretti101.escolaweb.service.TeacherService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Gestão do vínculo pedagógico entre professor, turma e disciplina com carga horária semanal e limites de avaliações
@Service
@RequiredArgsConstructor
public class TeacherClassSubjectServiceImpl implements TeacherClassSubjectService {

    private final TeacherClassSubjectRepository teacherClassSubjectRepository;
    private final LessonRepository lessonRepository;
    private final AssessmentRepository assessmentRepository;
    private final TeacherService teacherService;
    private final ClassRoomService classRoomService;
    private final SubjectService subjectService;

    // Vincula professor à turma e matéria com validação de status ativo e limites avaliativos
    @Override
    @Transactional
    public TeacherClassSubject create(TeacherClassSubject tcs) {
        Teacher teacher = resolveTeacher(tcs.getTeacher());
        ClassRoom classRoom = resolveClassRoom(tcs.getClassRoom());
        Subject subject = resolveSubject(tcs.getSubject());

        if (!teacher.isActive()) {
            throw new IllegalStateException("Não é possível vincular um professor inativo à turma.");
        }
        if (!classRoom.isActive()) {
            throw new IllegalStateException("Não é possível vincular professores a uma turma inativa.");
        }
        if (!subject.isActive()) {
            throw new IllegalStateException("Não é possível vincular uma matéria inativa.");
        }
        validateTeacherCanTeachSubject(teacher, subject);
        if (teacherClassSubjectRepository.existsByClassRoomAndSubject(classRoom, subject)) {
            throw new IllegalStateException("Esta matéria já está vinculada a esta turma (apenas um professor por matéria).");
        }

        applyWeeklyLessonsAndAssessmentRule(tcs);

        tcs.setTeacher(teacher);
        tcs.setClassRoom(classRoom);
        tcs.setSubject(subject);

        return teacherClassSubjectRepository.save(tcs);
    }

    @Override
    @Transactional
    public TeacherClassSubject update(Integer id, TeacherClassSubject incoming) {
        TeacherClassSubject existing = findTcsOrThrow(id);

        Teacher teacher = resolveTeacher(incoming.getTeacher());
        ClassRoom classRoom = resolveClassRoom(incoming.getClassRoom());
        Subject subject = resolveSubject(incoming.getSubject());

        validateTeacherCanTeachSubject(teacher, subject);

        boolean changed = !existing.getClassRoom().getId().equals(classRoom.getId())
                || !existing.getSubject().getId().equals(subject.getId());

        if (changed && teacherClassSubjectRepository.existsByClassRoomAndSubject(classRoom, subject)) {
            throw new IllegalStateException("Esta matéria já está vinculada a esta turma.");
        }

        applyWeeklyLessonsAndAssessmentRule(incoming);

        existing.setTeacher(teacher);
        existing.setClassRoom(classRoom);
        existing.setSubject(subject);
        existing.setWeeklyLessons(incoming.getWeeklyLessons());
        existing.setMinAssessmentsPerPeriod(incoming.getMinAssessmentsPerPeriod());
        existing.setMaxAssessmentsPerPeriod(incoming.getMaxAssessmentsPerPeriod());

        return existing;
    }

    private void validateTeacherCanTeachSubject(Teacher teacher, Subject subject) {
        if (teacher.getSubjects() != null && !teacher.getSubjects().isEmpty()
                && !teacher.getSubjects().contains(subject)) {
            throw new IllegalStateException(
                    "O professor '" + teacher.getName() + "' não está habilitado para lecionar a matéria '" + subject.getName() + "'.");
        }
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        TeacherClassSubject tcs = findTcsOrThrow(id);

        if (lessonRepository.existsByTeacherClassSubject(tcs)) {
            throw new IllegalStateException("Não é possível excluir este vínculo pois já existem aulas registradas.");
        }
        if (assessmentRepository.existsByTeacherClassSubject(tcs)) {
            throw new IllegalStateException("Não é possível excluir este vínculo pois já existem avaliações registradas.");
        }

        teacherClassSubjectRepository.delete(tcs);
        teacherClassSubjectRepository.flush();
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherClassSubject findById(Integer id) {
        return findTcsOrThrow(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherClassSubject> findAll() {
        return teacherClassSubjectRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<TeacherClassSubject> findAll(org.springframework.data.domain.Pageable pageable) {
        return teacherClassSubjectRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherClassSubject> findByTeacher(Integer teacherId) {
        Teacher teacher = teacherService.findById(teacherId);
        return teacherClassSubjectRepository.findByTeacher(teacher);
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<TeacherClassSubject> findByTeacherActiveAcademicYear(Integer teacherId, org.springframework.data.domain.Pageable pageable) {
        return teacherClassSubjectRepository.findByTeacherActiveAcademicYear(teacherId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherClassSubject> findByClassRoom(Integer classRoomId) {
        ClassRoom classRoom = classRoomService.findById(classRoomId);
        return teacherClassSubjectRepository.findByClassRoom(classRoom);
    }

    // ── Private helpers
    private void applyWeeklyLessonsAndAssessmentRule(TeacherClassSubject tcs) {
        validateAssessmentLimits(tcs.getMinAssessmentsPerPeriod(), tcs.getMaxAssessmentsPerPeriod());
        Integer weekly = tcs.getWeeklyLessons();
        if (weekly != null) {
            if (weekly < 1 || weekly > 4) {
                throw new IllegalArgumentException("O número de aulas semanais deve ser no mínimo 1 e no máximo 4.");
            }
            switch (weekly) {
                case 1 -> {
                    tcs.setMinAssessmentsPerPeriod(1);
                    tcs.setMaxAssessmentsPerPeriod(2);
                }
                case 2 -> {
                    tcs.setMinAssessmentsPerPeriod(2);
                    tcs.setMaxAssessmentsPerPeriod(3);
                }
                case 3 -> {
                    tcs.setMinAssessmentsPerPeriod(3);
                    tcs.setMaxAssessmentsPerPeriod(4);
                }
                case 4 -> {
                    tcs.setMinAssessmentsPerPeriod(4);
                    tcs.setMaxAssessmentsPerPeriod(4);
                }
            }
        } else {
            if (tcs.getMaxAssessmentsPerPeriod() != null) {
                if (tcs.getMaxAssessmentsPerPeriod() <= 2) {
                    tcs.setWeeklyLessons(1);
                } else if (tcs.getMaxAssessmentsPerPeriod() == 3) {
                    tcs.setWeeklyLessons(2);
                } else if (tcs.getMinAssessmentsPerPeriod() != null && tcs.getMinAssessmentsPerPeriod() >= 4) {
                    tcs.setWeeklyLessons(4);
                } else {
                    tcs.setWeeklyLessons(3);
                }
            } else {
                tcs.setWeeklyLessons(2);
                tcs.setMinAssessmentsPerPeriod(2);
                tcs.setMaxAssessmentsPerPeriod(3);
            }
        }
        validateAssessmentLimits(tcs.getMinAssessmentsPerPeriod(), tcs.getMaxAssessmentsPerPeriod());
    }

    private void validateAssessmentLimits(Integer min, Integer max) {
        if (min != null && max != null && min > max) {
            throw new IllegalArgumentException(
                    "O número mínimo de avaliações (" + min
                            + ") não pode ser maior que o número máximo (" + max + "). Ajuste os limites informados.");
        }
    }

    TeacherClassSubject findTcsOrThrow(Integer id) {
        return teacherClassSubjectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Vínculo de professor e disciplina na turma não encontrado ou já removido."));
    }

    private Teacher resolveTeacher(Teacher ref) {
        if (ref == null || ref.getId() == null) {
            throw new IllegalArgumentException("Selecione o professor responsável.");
        }
        return teacherService.findById(ref.getId());
    }

    private ClassRoom resolveClassRoom(ClassRoom ref) {
        if (ref == null || ref.getId() == null) {
            throw new IllegalArgumentException("Selecione a turma correspondente.");
        }
        return classRoomService.findById(ref.getId());
    }

    private Subject resolveSubject(Subject ref) {
        if (ref == null || ref.getId() == null) {
            throw new IllegalArgumentException("Selecione a disciplina correspondente.");
        }
        return subjectService.findById(ref.getId());
    }
}
