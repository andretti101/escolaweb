package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.AcademicYear;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AcademicYearRepository extends JpaRepository<AcademicYear, Integer> {
    Optional<AcademicYear> findByActiveTrue();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AcademicYear a WHERE a.active = true")
    Optional<AcademicYear> findActiveForUpdate();

    Optional<AcademicYear> findByYear(Integer year);
    boolean existsByYear(Integer year);
    boolean existsByYearAndIdNot(Integer year, Integer id);
}
