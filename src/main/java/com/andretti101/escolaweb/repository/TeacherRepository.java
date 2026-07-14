package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Integer> {
    List<Teacher> findByActiveTrue();
}
