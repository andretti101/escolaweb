package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.Secretary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SecretaryRepository extends JpaRepository<Secretary, Integer> {
    List<Secretary> findByActiveTrue();
}
