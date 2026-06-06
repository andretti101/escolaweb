package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.AttendanceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AttendanceHistoryRepository extends JpaRepository<AttendanceHistory, Integer> {

}
