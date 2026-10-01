package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.Attendance;
import com.andretti101.escolaweb.model.entity.Lesson;
import com.andretti101.escolaweb.model.entity.Student;
import com.andretti101.escolaweb.model.entity.AcademicYear;
import com.andretti101.escolaweb.model.enums.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Integer> {
    List<Attendance> findByLesson(Lesson lesson);
    List<Attendance> findByStudent(Student student);
    boolean existsByStudent(Student student);
    boolean existsByLessonAndStudent(Lesson lesson, Student student);
    boolean existsByLesson(Lesson lesson);
    List<Attendance> findByLessonInAndStudentAndStatus(List<Lesson> lessons, Student student, AttendanceStatus status);
    
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"student", "lesson", "lesson.teacherClassSubject", "lesson.teacherClassSubject.subject"})
    @Query("SELECT a FROM Attendance a WHERE a.student IN :students AND a.lesson.teacherClassSubject.classRoom.academicYear = :year")
    List<Attendance> findByStudentsAndYear(@Param("students") List<Student> students, @Param("year") AcademicYear year);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Attendance a SET a.active = false WHERE a.lesson.teacherClassSubject.classRoom.academicYear = :year AND a.active = true")
    int deactivateAllByAcademicYear(@org.springframework.data.repository.query.Param("year") AcademicYear academicYear);
}