package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.model.entity.User;
import com.andretti101.escolaweb.model.enums.UserType;

import java.util.List;

public interface UserService {

    User create(User user);

    User update(Integer id, User user);

    User findById(Integer id);

    User findByEmail(String email);

    List<User> findAll();

    void delete(Integer id);

    User authenticate(String email, String password);

    void validateAccess(User requestingUser, UserType... allowedTypes);

    boolean hasFullManagementAccess(User user);

    boolean hasAdminPanelAccess(User user);
}