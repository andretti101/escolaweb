package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.AcademicPeriod;
import com.andretti101.escolaweb.model.entity.AcademicYear;
import com.andretti101.escolaweb.model.enums.AcademicPeriodType;
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

    @Query(value = "SELECT p FROM AcademicPeriod p JOIN p.academicYear y ORDER BY y.year DESC, p.id DESC",
           countQuery = "SELECT count(p) FROM AcademicPeriod p")
    org.springframework.data.domain.Page<AcademicPeriod> findAll(org.springframework.data.domain.Pageable pageable);

    List<AcademicPeriod> findAllByOrderByAcademicYearYearDescIdDesc();

    @Query("SELECT p FROM AcademicPeriod p WHERE p.academicYear = :academicYear ORDER BY p.id DESC")
    List<AcademicPeriod> findByAcademicYear(@Param("academicYear") AcademicYear academicYear);

    List<AcademicPeriod> findByAcademicYearOrderByIdAsc(AcademicYear academicYear);

    List<AcademicPeriod> findByAcademicYearOrderByIdDesc(AcademicYear academicYear);

    List<AcademicPeriod> findByAcademicYearInOrderByIdAsc(java.util.Collection<AcademicYear> academicYears);

    List<AcademicPeriod> findByAcademicYearAndPeriodTypeOrderByIdAsc(AcademicYear year, AcademicPeriodType type);

    boolean existsByAcademicYear(AcademicYear academicYear);

    boolean existsByAcademicYearAndPeriodType(AcademicYear year, AcademicPeriodType type);

    @Query("SELECT CASE WHEN COUNT(p) = 0 THEN false ELSE (COUNT(CASE WHEN p.closed = false THEN 1 END) = 0) END FROM AcademicPeriod p WHERE p.academicYear = :year")
    boolean areAllPeriodsClosed(@Param("year") AcademicYear year);

    @Query("SELECT CASE WHEN COUNT(p) = 0 THEN false ELSE (COUNT(CASE WHEN p.closed = false THEN 1 END) = 0) END " +
           "FROM AcademicPeriod p WHERE p.academicYear = :year AND p.periodType = :type")
    boolean areAllPeriodsClosedByType(@Param("year") AcademicYear year, @Param("type") AcademicPeriodType type);
}
