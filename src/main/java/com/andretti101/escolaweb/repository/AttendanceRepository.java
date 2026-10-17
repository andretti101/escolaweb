package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.Attendance;
import com.andretti101.escolaweb.model.entity.Lesson;
import com.andretti101.escolaweb.model.entity.Student;
import com.andretti101.escolaweb.model.entity.AcademicYear;
import com.andretti101.escolaweb.model.enums.AttendanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Integer> {

    @Override
    @EntityGraph(attributePaths = {
        "student", "lesson", "lesson.teacherClassSubject", "lesson.teacherClassSubject.subject", "lesson.teacherClassSubject.teacher", "lesson.teacherClassSubject.classRoom", "lesson.teacherClassSubject.classRoom.academicYear"
    })
    Optional<Attendance> findById(Integer id);

    @Override
    @EntityGraph(attributePaths = {
        "student", "lesson", "lesson.teacherClassSubject", "lesson.teacherClassSubject.subject", "lesson.teacherClassSubject.teacher", "lesson.teacherClassSubject.classRoom", "lesson.teacherClassSubject.classRoom.academicYear"
    })
    List<Attendance> findAll();

    @Override
    @EntityGraph(attributePaths = {
        "student", "lesson", "lesson.teacherClassSubject", "lesson.teacherClassSubject.subject", "lesson.teacherClassSubject.teacher", "lesson.teacherClassSubject.classRoom", "lesson.teacherClassSubject.classRoom.academicYear"
    })
    Page<Attendance> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {
        "student", "lesson", "lesson.teacherClassSubject", "lesson.teacherClassSubject.subject", "lesson.teacherClassSubject.teacher", "lesson.teacherClassSubject.classRoom", "lesson.teacherClassSubject.classRoom.academicYear"
    })
    List<Attendance> findByLesson(Lesson lesson);

    @EntityGraph(attributePaths = {
        "student", "lesson", "lesson.teacherClassSubject", "lesson.teacherClassSubject.subject", "lesson.teacherClassSubject.teacher", "lesson.teacherClassSubject.classRoom", "lesson.teacherClassSubject.classRoom.academicYear"
    })
    List<Attendance> findByStudent(Student student);

    boolean existsByStudent(Student student);
    boolean existsByStudentAndLesson_TeacherClassSubject_ClassRoom(Student student, com.andretti101.escolaweb.model.entity.ClassRoom classRoom);
    boolean existsByLessonAndStudent(Lesson lesson, Student student);
    boolean existsByLesson(Lesson lesson);
    List<Attendance> findByLessonInAndStudentAndStatus(List<Lesson> lessons, Student student, AttendanceStatus status);

    @EntityGraph(attributePaths = {
        "student", "lesson", "lesson.teacherClassSubject", "lesson.teacherClassSubject.subject", "lesson.teacherClassSubject.teacher", "lesson.teacherClassSubject.classRoom", "lesson.teacherClassSubject.classRoom.academicYear"
    })
    @Query("SELECT a FROM Attendance a WHERE a.lesson.teacherClassSubject.teacher.id = :teacherId AND a.lesson.teacherClassSubject.classRoom.academicYear.active = true")
    Page<Attendance> findByTeacherActiveAcademicYear(@Param("teacherId") Integer teacherId, Pageable pageable);
    
    @EntityGraph(attributePaths = {
        "student", "lesson", "lesson.teacherClassSubject", "lesson.teacherClassSubject.subject", "lesson.teacherClassSubject.teacher", "lesson.teacherClassSubject.classRoom", "lesson.teacherClassSubject.classRoom.academicYear"
    })
    @Query("SELECT a FROM Attendance a WHERE a.student IN :students AND a.lesson.teacherClassSubject.classRoom.academicYear = :year")
    List<Attendance> findByStudentsAndYear(@Param("students") List<Student> students, @Param("year") AcademicYear year);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Attendance a SET a.active = false WHERE a.lesson.teacherClassSubject.classRoom.academicYear = :year AND a.active = true")
    int deactivateAllByAcademicYear(@Param("year") AcademicYear academicYear);

    @Query("SELECT a FROM Attendance a JOIN FETCH a.lesson l JOIN FETCH l.teacherClassSubject tcs JOIN FETCH tcs.teacher t WHERE a.id = :id")
    Optional<Attendance> findWithTcsById(@Param("id") Integer id);
}