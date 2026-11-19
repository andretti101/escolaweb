package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.RefreshToken;
import com.andretti101.escolaweb.model.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RefreshToken r WHERE r.token = :token")
    Optional<RefreshToken> findByTokenForUpdate(@Param("token") String token);

    @Modifying
    @Transactional
    void deleteByToken(String token);

    @Modifying
    @Transactional
    void deleteByUser(User user);

    @Modifying
    @Transactional
    void deleteByExpiresAtBefore(LocalDateTime now);
}
