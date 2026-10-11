package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.AcademicPeriod;
import com.andretti101.escolaweb.model.entity.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AcademicPeriodRepository extends JpaRepository<AcademicPeriod, Integer> {
    @Override
    @Query("SELECT p FROM AcademicPeriod p JOIN p.academicYear y ORDER BY y.year DESC, p.id DESC")
    List<AcademicPeriod> findAll();

    List<AcademicPeriod> findAllByOrderByAcademicYearYearDescIdDesc();

    @Query("SELECT p FROM AcademicPeriod p WHERE p.academicYear = :academicYear ORDER BY p.id DESC")
    List<AcademicPeriod> findByAcademicYear(@Param("academicYear") AcademicYear academicYear);

    List<AcademicPeriod> findByAcademicYearOrderByIdAsc(AcademicYear academicYear);

    List<AcademicPeriod> findByAcademicYearOrderByIdDesc(AcademicYear academicYear);

    boolean existsByAcademicYear(AcademicYear academicYear);

    @Query("SELECT CASE WHEN COUNT(p) = 0 THEN false ELSE (COUNT(CASE WHEN p.closed = false THEN 1 END) = 0) END FROM AcademicPeriod p WHERE p.academicYear = :year")
    boolean areAllPeriodsClosed(@Param("year") AcademicYear year);
}
