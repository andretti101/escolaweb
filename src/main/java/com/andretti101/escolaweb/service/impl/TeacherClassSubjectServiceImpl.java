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

@Service
@RequiredArgsConstructor
public class TeacherClassSubjectServiceImpl implements TeacherClassSubjectService {

    private final TeacherClassSubjectRepository teacherClassSubjectRepository;
    private final LessonRepository lessonRepository;
    private final AssessmentRepository assessmentRepository;
    private final TeacherService teacherService;
    private final ClassRoomService classRoomService;
    private final SubjectService subjectService;

    @Override
    @Transactional
    public TeacherClassSubject create(TeacherClassSubject tcs) {
        Teacher teacher = resolveTeacher(tcs.getTeacher());
        ClassRoom classRoom = resolveClassRoom(tcs.getClassRoom());
        Subject subject = resolveSubject(tcs.getSubject());

        if (!teacher.isActive()) {
            throw new IllegalStateException(
                    "Cannot assign inactive teacher with id " + teacher.getId() + " to a class.");
        }
        if (!classRoom.isActive()) {
            throw new IllegalStateException(
                    "Cannot assign to inactive classroom with id " + classRoom.getId() + ".");
        }
        if (!subject.isActive()) {
            throw new IllegalStateException(
                    "Cannot assign inactive subject with id " + subject.getId() + ".");
        }

        if (teacherClassSubjectRepository.existsByTeacherAndClassRoomAndSubject(teacher, classRoom, subject)) {
            throw new IllegalStateException(
                    "Teacher with id " + teacher.getId()
                    + " is already assigned to subject with id " + subject.getId()
                    + " in classroom with id " + classRoom.getId() + ".");
        }

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

        boolean changed = !existing.getTeacher().getId().equals(teacher.getId())
                || !existing.getClassRoom().getId().equals(classRoom.getId())
                || !existing.getSubject().getId().equals(subject.getId());

        if (changed && teacherClassSubjectRepository.existsByTeacherAndClassRoomAndSubject(teacher, classRoom, subject)) {
            throw new IllegalStateException(
                    "Teacher with id " + teacher.getId()
                    + " is already assigned to subject with id " + subject.getId()
                    + " in classroom with id " + classRoom.getId() + ".");
        }

        existing.setTeacher(teacher);
        existing.setClassRoom(classRoom);
        existing.setSubject(subject);

        return existing;
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        TeacherClassSubject tcs = findTcsOrThrow(id);

        if (lessonRepository.existsByTeacherClassSubject(tcs)) {
            throw new IllegalStateException(
                    "Cannot delete assignment with id " + id + " because it has lessons registered.");
        }
        if (assessmentRepository.existsByTeacherClassSubject(tcs)) {
            throw new IllegalStateException(
                    "Cannot delete assignment with id " + id + " because it has assessments registered.");
        }

        teacherClassSubjectRepository.deleteById(id);
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
    public List<TeacherClassSubject> findByTeacher(Integer teacherId) {
        Teacher teacher = teacherService.findById(teacherId);
        return teacherClassSubjectRepository.findByTeacher(teacher);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherClassSubject> findByClassRoom(Integer classRoomId) {
        ClassRoom classRoom = classRoomService.findById(classRoomId);
        return teacherClassSubjectRepository.findByClassRoom(classRoom);
    }

    TeacherClassSubject findTcsOrThrow(Integer id) {
        return teacherClassSubjectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "TeacherClassSubject not found with id: " + id));
    }

    private Teacher resolveTeacher(Teacher ref) {
        if (ref == null || ref.getId() == null) {
            throw new IllegalArgumentException("Teacher is required.");
        }
        return teacherService.findById(ref.getId());
    }

    private ClassRoom resolveClassRoom(ClassRoom ref) {
        if (ref == null || ref.getId() == null) {
            throw new IllegalArgumentException("Classroom is required.");
        }
        return classRoomService.findById(ref.getId());
    }

    private Subject resolveSubject(Subject ref) {
        if (ref == null || ref.getId() == null) {
            throw new IllegalArgumentException("Subject is required.");
        }
        return subjectService.findById(ref.getId());
    }
}
