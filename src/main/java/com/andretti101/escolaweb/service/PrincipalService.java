package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.model.entity.Principal;

import java.util.List;

public interface PrincipalService {
    Principal create(Principal principal);
    Principal update(Integer id, Principal principal);
    void delete(Integer id);
    Principal findById(Integer id);
    List<Principal> findAll();
    org.springframework.data.domain.Page<Principal> findAll(org.springframework.data.domain.Pageable pageable);
    List<Principal> findAllActive();
    org.springframework.data.domain.Page<Principal> findAllActive(org.springframework.data.domain.Pageable pageable);
    Principal activate(Integer id);
    Principal deactivate(Integer id);
}
