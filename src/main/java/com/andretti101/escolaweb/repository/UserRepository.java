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

    @org.springframework.data.jpa.repository.Query("SELECT s.id FROM Student s JOIN s.enrollments e WHERE e.classRoom.id = :classroomId AND e.active = true")
    List<Integer> findStudentIdsByClassroomId(@org.springframework.data.repository.query.Param("classroomId") Integer classroomId);

    @org.springframework.data.jpa.repository.Query("SELECT u.id FROM User u WHERE u.role IN :roles")
    List<Integer> findIdsByRoles(@org.springframework.data.repository.query.Param("roles") List<com.andretti101.escolaweb.model.enums.UserRole> roles);

    List<User> findByRole(com.andretti101.escolaweb.model.enums.UserRole role);
}
