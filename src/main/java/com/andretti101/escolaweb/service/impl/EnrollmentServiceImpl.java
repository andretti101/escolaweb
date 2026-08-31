package com.andretti101.escolaweb.service.impl;

import com.andretti101.escolaweb.model.entity.ClassRoom;
import com.andretti101.escolaweb.model.entity.Enrollment;
import com.andretti101.escolaweb.model.entity.Student;
import com.andretti101.escolaweb.repository.AttendanceRepository;
import com.andretti101.escolaweb.repository.EnrollmentRepository;
import com.andretti101.escolaweb.repository.GradeRepository;
import com.andretti101.escolaweb.service.ClassRoomService;
import com.andretti101.escolaweb.service.EnrollmentService;
import com.andretti101.escolaweb.service.StudentService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final GradeRepository gradeRepository;
    private final AttendanceRepository attendanceRepository;
    private final StudentService studentService;
    private final ClassRoomService classRoomService;

    @Override
    @Transactional
    public Enrollment create(Enrollment enrollment) {
        Student student = resolveStudent(enrollment.getStudent());
        ClassRoom classRoom = resolveClassRoom(enrollment.getClassRoom());

        if (!student.isActive()) {
            throw new IllegalStateException("Não é possível matricular um aluno inativo.");
        }
        if (!classRoom.isActive()) {
            throw new IllegalStateException("Não é possível realizar matrículas em uma turma inativa.");
        }
        if (enrollmentRepository.existsByStudentAndClassRoom_AcademicYearAndActiveTrue(
                student, classRoom.getAcademicYear())) {
            throw new IllegalStateException("O aluno já possui uma matrícula ativa para este ano letivo.");
        }
        if (enrollmentRepository.existsByStudentAndClassRoom(student, classRoom)) {
            throw new IllegalStateException("O aluno já está matriculado nesta turma.");
        }

        enrollment.setStudent(student);
        enrollment.setClassRoom(classRoom);
        enrollment.setActive(true);

        return enrollmentRepository.save(enrollment);
    }

    @Override
    @Transactional
    public Enrollment update(Integer id, Enrollment incoming) {
        Enrollment existing = findEnrollmentOrThrow(id);

        Student student = resolveStudent(incoming.getStudent());
        ClassRoom classRoom = resolveClassRoom(incoming.getClassRoom());

        boolean studentChanged = !existing.getStudent().getId().equals(student.getId());
        boolean classRoomChanged = !existing.getClassRoom().getId().equals(classRoom.getId());

        if ((studentChanged || classRoomChanged)
                && enrollmentRepository.existsByStudentAndClassRoom_AcademicYearAndActiveTrue(
                        student, classRoom.getAcademicYear())) {
            throw new IllegalStateException("O aluno já possui uma matrícula ativa para este ano letivo.");
        }

        existing.setStudent(student);
        existing.setClassRoom(classRoom);
        existing.setEnrollmentDate(incoming.getEnrollmentDate());

        return existing;
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        Enrollment enrollment = findEnrollmentOrThrow(id);
        Student student = enrollment.getStudent();

        if (gradeRepository.existsByStudent(student)) {
            throw new IllegalStateException("Não é possível excluir esta matrícula pois o aluno já possui notas registradas. Considere desativar a matrícula em vez de excluir.");
        }
        if (attendanceRepository.existsByStudent(student)) {
            throw new IllegalStateException("Não é possível excluir esta matrícula pois o aluno já possui presenças registradas. Considere desativar a matrícula em vez de excluir.");
        }

        enrollmentRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Enrollment findById(Integer id) {
        return findEnrollmentOrThrow(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Enrollment> findAll() {
        return enrollmentRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Enrollment> findAllActive() {
        return enrollmentRepository.findByActiveTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Enrollment> findByStudent(Integer studentId) {
        Student student = studentService.findById(studentId);
        return enrollmentRepository.findByStudent(student);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Enrollment> findByClassRoom(Integer classRoomId) {
        ClassRoom classRoom = classRoomService.findById(classRoomId);
        return enrollmentRepository.findByClassRoom(classRoom);
    }

    @Override
    @Transactional
    public Enrollment activate(Integer id) {
        Enrollment enrollment = findEnrollmentOrThrow(id);
        enrollment.setActive(true);
        return enrollment;
    }

    @Override
    @Transactional
    public Enrollment deactivate(Integer id) {
        Enrollment enrollment = findEnrollmentOrThrow(id);
        enrollment.setActive(false);
        return enrollment;
    }

    private Enrollment findEnrollmentOrThrow(Integer id) {
        return enrollmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Enrollment not found with id: " + id));
    }

    private Student resolveStudent(Student ref) {
        if (ref == null || ref.getId() == null) {
            throw new IllegalArgumentException("Student is required.");
        }
        return studentService.findById(ref.getId());
    }

    private ClassRoom resolveClassRoom(ClassRoom ref) {
        if (ref == null || ref.getId() == null) {
            throw new IllegalArgumentException("Classroom is required.");
        }
        return classRoomService.findById(ref.getId());
    }
}
