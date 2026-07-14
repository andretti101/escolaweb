package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.model.entity.Principal;

import java.util.List;

public interface PrincipalService {
    Principal create(Principal principal);
    Principal update(Integer id, Principal principal);
    void delete(Integer id);
    Principal findById(Integer id);
    List<Principal> findAll();
    List<Principal> findAllActive();
    Principal activate(Integer id);
    Principal deactivate(Integer id);
}
