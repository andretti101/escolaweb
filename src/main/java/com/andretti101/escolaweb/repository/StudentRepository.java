package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentRepository extends JpaRepository<Student, Integer> {
    List<Student> findByActiveTrue();
    
    @Query("SELECT s FROM Student s WHERE s.active = true AND s.id NOT IN (SELECT e.student.id FROM Enrollment e)")
    List<Student> findActiveUnenrolled();
}
