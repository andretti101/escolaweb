package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Integer> {
    List<Subject> findByActiveTrue();
    Optional<Subject> findByNameIgnoreCase(String name);
}
