package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.AcademicPeriod;
import com.andretti101.escolaweb.model.entity.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AcademicPeriodRepository extends JpaRepository<AcademicPeriod, Integer> {
    List<AcademicPeriod> findByAcademicYear(AcademicYear academicYear);
    List<AcademicPeriod> findByAcademicYearOrderByIdAsc(AcademicYear academicYear);
    boolean existsByAcademicYear(AcademicYear academicYear);

    @org.springframework.data.jpa.repository.Query("SELECT CASE WHEN COUNT(p) = 0 THEN false ELSE (COUNT(CASE WHEN p.closed = false THEN 1 END) = 0) END FROM AcademicPeriod p WHERE p.academicYear = :year")
    boolean areAllPeriodsClosed(@org.springframework.data.repository.query.Param("year") AcademicYear year);
}
