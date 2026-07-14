package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.SchoolSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SchoolSettingsRepository extends JpaRepository<SchoolSettings, Integer> {
    Optional<SchoolSettings> findFirstByOrderByIdAsc();
}
