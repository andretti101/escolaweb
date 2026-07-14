package com.andretti101.escolaweb.service.impl;

import com.andretti101.escolaweb.model.entity.Principal;
import com.andretti101.escolaweb.repository.PrincipalRepository;
import com.andretti101.escolaweb.repository.UserRepository;
import com.andretti101.escolaweb.service.PrincipalService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PrincipalServiceImpl implements PrincipalService {

    private final PrincipalRepository principalRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public Principal create(Principal principal) {
        if (userRepository.existsByEmail(principal.getEmail())) {
            throw new IllegalStateException("Email already in use: " + principal.getEmail());
        }
        principal.setPassword(passwordEncoder.encode(principal.getPassword()));
        principal.setActive(true);
        return principal;
    }

    @Override
    @Transactional
    public Principal update(Integer id, Principal incoming) {
        Principal existing = findPrincipalOrThrow(id);

        if (!existing.getEmail().equals(incoming.getEmail())
                && userRepository.existsByEmailAndIdNot(incoming.getEmail(), id)) {
            throw new IllegalStateException("Email already in use: " + incoming.getEmail());
        }

        existing.setName(incoming.getName());
        existing.setEmail(incoming.getEmail());

        return principalRepository.save(existing);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        findPrincipalOrThrow(id);
        principalRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Principal findById(Integer id) {
        return findPrincipalOrThrow(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Principal> findAll() {
        return principalRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Principal> findAllActive() {
        return principalRepository.findByActiveTrue();
    }

    @Override
    @Transactional
    public Principal activate(Integer id) {
        Principal principal = findPrincipalOrThrow(id);
        principal.setActive(true);
        return principal;
    }

    @Override
    @Transactional
    public Principal deactivate(Integer id) {
        Principal principal = findPrincipalOrThrow(id);
        principal.setActive(false);
        return principal;
    }

    private Principal findPrincipalOrThrow(Integer id) {
        return principalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Principal not found with id: " + id));
    }
}
