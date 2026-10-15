package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.AcademicYear;
import com.andretti101.escolaweb.model.entity.ClassRoom;
import com.andretti101.escolaweb.model.enums.AcademicPeriodType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassRoomRepository extends JpaRepository<ClassRoom, Integer> {
    List<ClassRoom> findByAcademicYear(AcademicYear academicYear);
    List<ClassRoom> findByAcademicYearAndActiveTrue(AcademicYear academicYear);
    List<ClassRoom> findByActiveTrue();
    boolean existsByAcademicYear(AcademicYear academicYear);
    boolean existsByAcademicYearAndName(AcademicYear academicYear, String name);
    boolean existsByAcademicYearAndPeriodTypeAndActiveTrue(AcademicYear year, AcademicPeriodType type);

    @Query("SELECT DISTINCT c.periodType FROM ClassRoom c WHERE c.academicYear = :year AND c.active = true")
    List<AcademicPeriodType> findDistinctActivePeriodTypesByYear(@Param("year") AcademicYear year);

    @Modifying
    @Query("UPDATE ClassRoom c SET c.active = false WHERE c.academicYear = :year AND c.active = true")
    int deactivateAllByAcademicYear(@Param("year") AcademicYear academicYear);
}
