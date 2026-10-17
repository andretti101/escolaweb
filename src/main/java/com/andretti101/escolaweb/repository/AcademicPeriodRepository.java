package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.AcademicPeriod;
import com.andretti101.escolaweb.model.entity.AcademicYear;
import com.andretti101.escolaweb.model.enums.AcademicPeriodType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AcademicPeriodRepository extends JpaRepository<AcademicPeriod, Integer> {

    @Override
    @EntityGraph(attributePaths = {"academicYear"})
    Optional<AcademicPeriod> findById(Integer id);

    @Override
    @EntityGraph(attributePaths = {"academicYear"})
    @Query("SELECT p FROM AcademicPeriod p JOIN p.academicYear y ORDER BY y.year DESC, p.id DESC")
    List<AcademicPeriod> findAll();

    @EntityGraph(attributePaths = {"academicYear"})
    @Query(value = "SELECT p FROM AcademicPeriod p JOIN p.academicYear y ORDER BY y.year DESC, p.id DESC",
           countQuery = "SELECT count(p) FROM AcademicPeriod p")
    Page<AcademicPeriod> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"academicYear"})
    List<AcademicPeriod> findAllByOrderByAcademicYearYearDescIdDesc();

    @EntityGraph(attributePaths = {"academicYear"})
    @Query("SELECT p FROM AcademicPeriod p WHERE p.academicYear = :academicYear ORDER BY p.id DESC")
    List<AcademicPeriod> findByAcademicYear(@Param("academicYear") AcademicYear academicYear);

    @EntityGraph(attributePaths = {"academicYear"})
    List<AcademicPeriod> findByAcademicYearOrderByIdAsc(AcademicYear academicYear);

    @EntityGraph(attributePaths = {"academicYear"})
    List<AcademicPeriod> findByAcademicYearOrderByIdDesc(AcademicYear academicYear);

    @EntityGraph(attributePaths = {"academicYear"})
    List<AcademicPeriod> findByAcademicYearInOrderByIdAsc(Collection<AcademicYear> academicYears);

    @EntityGraph(attributePaths = {"academicYear"})
    @Query("SELECT p FROM AcademicPeriod p WHERE p.academicYear.id IN :yearIds ORDER BY p.id ASC")
    List<AcademicPeriod> findByAcademicYearIdsInOrderByIdAsc(@Param("yearIds") Collection<Integer> yearIds);

    @EntityGraph(attributePaths = {"academicYear"})
    List<AcademicPeriod> findByAcademicYearAndPeriodTypeOrderByIdAsc(AcademicYear year, AcademicPeriodType type);

    boolean existsByAcademicYear(AcademicYear academicYear);

    boolean existsByAcademicYearAndPeriodType(AcademicYear year, AcademicPeriodType type);

    @Query("SELECT CASE WHEN COUNT(p) = 0 THEN false ELSE (COUNT(CASE WHEN p.closed = false THEN 1 END) = 0) END FROM AcademicPeriod p WHERE p.academicYear = :year")
    boolean areAllPeriodsClosed(@Param("year") AcademicYear year);

    @Query("SELECT CASE WHEN COUNT(p) = 0 THEN false ELSE (COUNT(CASE WHEN p.closed = false THEN 1 END) = 0) END " +
           "FROM AcademicPeriod p WHERE p.academicYear = :year AND p.periodType = :type")
    boolean areAllPeriodsClosedByType(@Param("year") AcademicYear year, @Param("type") AcademicPeriodType type);
}
