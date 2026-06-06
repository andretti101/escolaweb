package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.Assessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssessmentRepository extends JpaRepository<Assessment, Integer> {

}