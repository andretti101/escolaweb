package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.AcademicYear;
import com.andretti101.escolaweb.model.entity.ClassRoom;
import com.andretti101.escolaweb.model.entity.Enrollment;
import com.andretti101.escolaweb.model.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Integer> {

    @Override
    @EntityGraph(attributePaths = {"student", "classRoom", "classRoom.academicYear"})
    Optional<Enrollment> findById(Integer id);

    @Override
    @EntityGraph(attributePaths = {"student", "classRoom", "classRoom.academicYear"})
    List<Enrollment> findAll();

    @Override
    @EntityGraph(attributePaths = {"student", "classRoom", "classRoom.academicYear"})
    Page<Enrollment> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"student", "classRoom", "classRoom.academicYear"})
    List<Enrollment> findByStudent(Student student);

    @EntityGraph(attributePaths = {"student", "classRoom", "classRoom.academicYear"})
    List<Enrollment> findByClassRoomOrderByStudentNameAsc(ClassRoom classRoom);

    @EntityGraph(attributePaths = {"student", "classRoom", "classRoom.academicYear"})
    List<Enrollment> findByStudentAndActiveTrue(Student student);

    @EntityGraph(attributePaths = {"student", "classRoom", "classRoom.academicYear"})
    List<Enrollment> findByActiveTrue();

    @EntityGraph(attributePaths = {"student", "classRoom", "classRoom.academicYear"})
    Page<Enrollment> findByActiveTrue(Pageable pageable);

    boolean existsByStudentAndClassRoom(Student student, ClassRoom classRoom);
    boolean existsByStudentAndClassRoomAndActiveTrue(Student student, ClassRoom classRoom);
    boolean existsByStudentAndClassRoom_AcademicYear(Student student, AcademicYear academicYear);
    boolean existsByStudentAndClassRoom_AcademicYearAndActiveTrue(Student student, AcademicYear academicYear);
    boolean existsByClassRoom(ClassRoom classRoom);
    boolean existsByStudent_IdAndClassRoom_IdAndActiveTrue(Integer studentId, Integer classRoomId);

    @EntityGraph(attributePaths = {"student", "classRoom", "classRoom.academicYear"})
    List<Enrollment> findByClassRoom_AcademicYear(AcademicYear academicYear);

    @EntityGraph(attributePaths = {"student", "classRoom", "classRoom.academicYear"})
    List<Enrollment> findByClassRoom_AcademicYearAndActiveTrue(AcademicYear academicYear);
    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @org.springframework.data.jpa.repository.Query("UPDATE Enrollment e SET e.active = false WHERE e.classRoom.academicYear = :year AND e.active = true")
    int deactivateAllByAcademicYear(@org.springframework.data.repository.query.Param("year") AcademicYear academicYear);
}
