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
    User activate(Integer id);
    User deactivate(Integer id);
}
