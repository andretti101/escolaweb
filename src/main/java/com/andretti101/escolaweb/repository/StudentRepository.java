package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentRepository extends JpaRepository<Student, Integer> {
    @Override
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"enrollments", "enrollments.classRoom", "enrollments.classRoom.academicYear"})
    java.util.Optional<Student> findById(Integer id);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"enrollments", "enrollments.classRoom", "enrollments.classRoom.academicYear"})
    List<Student> findAll();

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"enrollments", "enrollments.classRoom", "enrollments.classRoom.academicYear"})
    org.springframework.data.domain.Page<Student> findAll(org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"enrollments", "enrollments.classRoom", "enrollments.classRoom.academicYear"})
    List<Student> findByActiveTrue();

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"enrollments", "enrollments.classRoom", "enrollments.classRoom.academicYear"})
    org.springframework.data.domain.Page<Student> findByActiveTrue(org.springframework.data.domain.Pageable pageable);
    
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"enrollments", "enrollments.classRoom", "enrollments.classRoom.academicYear"})
    @Query("SELECT s FROM Student s WHERE s.active = true AND s.id NOT IN (SELECT e.student.id FROM Enrollment e WHERE e.active = true)")
    List<Student> findActiveUnenrolled();

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"enrollments", "enrollments.classRoom", "enrollments.classRoom.academicYear"})
    @Query(value = "SELECT s FROM Student s WHERE s.active = true AND s.id NOT IN (SELECT e.student.id FROM Enrollment e WHERE e.active = true)",
           countQuery = "SELECT count(s) FROM Student s WHERE s.active = true AND s.id NOT IN (SELECT e.student.id FROM Enrollment e WHERE e.active = true)")
    org.springframework.data.domain.Page<Student> findActiveUnenrolled(org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"enrollments", "enrollments.classRoom", "enrollments.classRoom.academicYear"})
    @Query("SELECT s FROM Student s JOIN s.enrollments e WHERE e.classRoom.id = :classroomId ORDER BY s.name ASC")
    List<Student> findByClassroomId(@org.springframework.data.repository.query.Param("classroomId") Integer classroomId);

    boolean existsByRegistrationNumber(String registrationNumber);

    boolean existsByRegistrationNumberAndIdNot(String registrationNumber, Integer id);
}
