package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.AcademicPeriod;
import com.andretti101.escolaweb.model.entity.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AcademicPeriodRepository extends JpaRepository<AcademicPeriod, Integer> {
    List<AcademicPeriod> findByAcademicYear(AcademicYear academicYear);
    boolean existsByAcademicYear(AcademicYear academicYear);
}
