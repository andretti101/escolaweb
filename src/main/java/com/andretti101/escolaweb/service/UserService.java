package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.model.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserService {
    User findById(Integer id);
    Optional<User> findByEmail(String email);
    List<User> findAll();
    List<User> findAllActive();
    void changePassword(Integer id, String currentPassword, String newPassword);
    void resetPassword(Integer id, String newPassword);
    User activate(Integer id);
    User deactivate(Integer id);
    List<com.andretti101.escolaweb.dto.response.UserSummaryDTO> findSummaries(com.andretti101.escolaweb.model.enums.UserRole role, Boolean active);
}
