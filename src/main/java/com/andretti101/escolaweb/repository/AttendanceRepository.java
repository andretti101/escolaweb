package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.Attendance;
import com.andretti101.escolaweb.model.entity.Lesson;
import com.andretti101.escolaweb.model.entity.Student;
import com.andretti101.escolaweb.model.enums.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Integer> {
    List<Attendance> findByLesson(Lesson lesson);
    List<Attendance> findByStudent(Student student);
    boolean existsByStudent(Student student);
    boolean existsByLessonAndStudent(Lesson lesson, Student student);
    boolean existsByLesson(Lesson lesson);
    long countByLessonInAndStudentAndStatus(List<Lesson> lessons, Student student, AttendanceStatus status);
}
