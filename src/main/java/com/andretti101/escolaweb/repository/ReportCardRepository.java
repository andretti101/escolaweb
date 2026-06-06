package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.ReportCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportCardRepository extends JpaRepository<ReportCard, Integer> {

}