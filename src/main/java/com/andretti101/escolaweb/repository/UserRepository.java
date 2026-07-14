package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, Integer id);
    List<User> findByActiveTrue();
    List<User> findByActiveFalse();
}
