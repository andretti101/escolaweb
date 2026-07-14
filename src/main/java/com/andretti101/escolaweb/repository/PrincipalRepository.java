package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.Principal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrincipalRepository extends JpaRepository<Principal, Integer> {
    List<Principal> findByActiveTrue();
}
