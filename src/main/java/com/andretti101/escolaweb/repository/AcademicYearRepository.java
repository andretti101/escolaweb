package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AcademicYearRepository extends JpaRepository<AcademicYear, Integer> {
    Optional<AcademicYear> findByActiveTrue();
    boolean existsByYear(Integer year);
    boolean existsByYearAndIdNot(Integer year, Integer id);
}
