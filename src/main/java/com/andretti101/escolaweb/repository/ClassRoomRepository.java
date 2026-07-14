package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.AcademicYear;
import com.andretti101.escolaweb.model.entity.ClassRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassRoomRepository extends JpaRepository<ClassRoom, Integer> {
    List<ClassRoom> findByAcademicYear(AcademicYear academicYear);
    List<ClassRoom> findByAcademicYearAndActiveTrue(AcademicYear academicYear);
    List<ClassRoom> findByActiveTrue();
    boolean existsByAcademicYear(AcademicYear academicYear);
    boolean existsByAcademicYearAndName(AcademicYear academicYear, String name);
}
