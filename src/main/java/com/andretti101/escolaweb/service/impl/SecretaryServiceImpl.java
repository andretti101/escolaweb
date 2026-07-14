package com.andretti101.escolaweb.service.impl;

import com.andretti101.escolaweb.model.entity.Secretary;
import com.andretti101.escolaweb.repository.SecretaryRepository;
import com.andretti101.escolaweb.repository.UserRepository;
import com.andretti101.escolaweb.service.SecretaryService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SecretaryServiceImpl implements SecretaryService {

    private final SecretaryRepository secretaryRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public Secretary create(Secretary secretary) {
        if (userRepository.existsByEmail(secretary.getEmail())) {
            throw new IllegalStateException("Email already in use: " + secretary.getEmail());
        }
        secretary.setPassword(passwordEncoder.encode(secretary.getPassword()));
        secretary.setActive(true);
        return secretary;
    }

    @Override
    @Transactional
    public Secretary update(Integer id, Secretary incoming) {
        Secretary existing = findSecretaryOrThrow(id);

        if (!existing.getEmail().equals(incoming.getEmail())
                && userRepository.existsByEmailAndIdNot(incoming.getEmail(), id)) {
            throw new IllegalStateException("Email already in use: " + incoming.getEmail());
        }

        existing.setName(incoming.getName());
        existing.setEmail(incoming.getEmail());

        return secretaryRepository.save(existing);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        findSecretaryOrThrow(id);
        secretaryRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Secretary findById(Integer id) {
        return findSecretaryOrThrow(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Secretary> findAll() {
        return secretaryRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Secretary> findAllActive() {
        return secretaryRepository.findByActiveTrue();
    }

    @Override
    @Transactional
    public Secretary activate(Integer id) {
        Secretary secretary = findSecretaryOrThrow(id);
        secretary.setActive(true);
        return secretary;
    }

    @Override
    @Transactional
    public Secretary deactivate(Integer id) {
        Secretary secretary = findSecretaryOrThrow(id);
        secretary.setActive(false);
        return secretary;
    }

    private Secretary findSecretaryOrThrow(Integer id) {
        return secretaryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Secretary not found with id: " + id));
    }
}
