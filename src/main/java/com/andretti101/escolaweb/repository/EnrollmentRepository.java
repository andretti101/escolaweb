package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.AcademicYear;
import com.andretti101.escolaweb.model.entity.ClassRoom;
import com.andretti101.escolaweb.model.entity.Enrollment;
import com.andretti101.escolaweb.model.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Integer> {
    List<Enrollment> findByStudent(Student student);
    List<Enrollment> findByClassRoom(ClassRoom classRoom);
    List<Enrollment> findByStudentAndActiveTrue(Student student);
    List<Enrollment> findByActiveTrue();
    boolean existsByStudentAndClassRoom(Student student, ClassRoom classRoom);
    boolean existsByStudentAndClassRoom_AcademicYear(Student student, AcademicYear academicYear);
    boolean existsByStudentAndClassRoom_AcademicYearAndActiveTrue(Student student, AcademicYear academicYear);
    boolean existsByClassRoom(ClassRoom classRoom);
}
