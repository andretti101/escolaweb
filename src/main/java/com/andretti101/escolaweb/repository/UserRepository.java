package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    boolean existsByEmail(String email);

    boolean existsByCpf(String cpf);

    Optional<User> findByEmail(String email);

    Optional<User> findByCpf(String cpf);
}