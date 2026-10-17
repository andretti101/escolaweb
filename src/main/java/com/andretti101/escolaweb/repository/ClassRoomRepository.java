package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.AcademicYear;
import com.andretti101.escolaweb.model.entity.ClassRoom;
import com.andretti101.escolaweb.model.enums.AcademicPeriodType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassRoomRepository extends JpaRepository<ClassRoom, Integer> {

    @Override
    @EntityGraph(attributePaths = {"academicYear"})
    Optional<ClassRoom> findById(Integer id);

    @Override
    @EntityGraph(attributePaths = {"academicYear"})
    List<ClassRoom> findAll();

    @Override
    @EntityGraph(attributePaths = {"academicYear"})
    Page<ClassRoom> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"academicYear"})
    List<ClassRoom> findByAcademicYear(AcademicYear academicYear);

    @EntityGraph(attributePaths = {"academicYear"})
    Page<ClassRoom> findByAcademicYear(AcademicYear academicYear, Pageable pageable);

    @EntityGraph(attributePaths = {"academicYear"})
    List<ClassRoom> findByAcademicYearAndActiveTrue(AcademicYear academicYear);

    @EntityGraph(attributePaths = {"academicYear"})
    Page<ClassRoom> findByAcademicYearAndActiveTrue(AcademicYear academicYear, Pageable pageable);

    @EntityGraph(attributePaths = {"academicYear"})
    List<ClassRoom> findByActiveTrue();

    @EntityGraph(attributePaths = {"academicYear"})
    Page<ClassRoom> findByActiveTrue(Pageable pageable);

    boolean existsByAcademicYear(AcademicYear academicYear);
    boolean existsByAcademicYearAndName(AcademicYear academicYear, String name);
    boolean existsByAcademicYearAndPeriodTypeAndActiveTrue(AcademicYear year, AcademicPeriodType type);

    @Query("SELECT DISTINCT c.periodType FROM ClassRoom c WHERE c.academicYear = :year AND c.active = true")
    List<AcademicPeriodType> findDistinctActivePeriodTypesByYear(@Param("year") AcademicYear year);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ClassRoom c SET c.active = false WHERE c.academicYear = :year AND c.active = true")
    int deactivateAllByAcademicYear(@Param("year") AcademicYear academicYear);
}
