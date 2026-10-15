package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.model.entity.Secretary;

import java.util.List;

public interface SecretaryService {
    Secretary create(Secretary secretary);
    Secretary update(Integer id, Secretary secretary);
    void delete(Integer id);
    Secretary findById(Integer id);
    List<Secretary> findAll();
    org.springframework.data.domain.Page<Secretary> findAll(org.springframework.data.domain.Pageable pageable);
    List<Secretary> findAllActive();
    org.springframework.data.domain.Page<Secretary> findAllActive(org.springframework.data.domain.Pageable pageable);
    Secretary activate(Integer id);
    Secretary deactivate(Integer id);
}
