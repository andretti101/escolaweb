package com.andretti101.escolaweb.service.impl;

import com.andretti101.escolaweb.exception.BusinessException;
import com.andretti101.escolaweb.exception.DuplicateResourceException;
import com.andretti101.escolaweb.exception.ResourceNotFoundException;
import com.andretti101.escolaweb.model.entity.User;
import com.andretti101.escolaweb.model.enums.UserType;
import com.andretti101.escolaweb.repository.UserRepository;
import com.andretti101.escolaweb.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public User create(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new DuplicateResourceException("Já existe um usuário cadastrado com este e-mail.");
        }
        if (user.getCpf() != null && userRepository.existsByCpf(user.getCpf())) {
            throw new DuplicateResourceException("Já existe um usuário cadastrado com este CPF.");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User update(Integer id, User user) {
        User existing = findById(id);

        userRepository.findByEmail(user.getEmail()).ifPresent(other -> {
            if (!other.getId().equals(id)) {
                throw new DuplicateResourceException("Já existe um usuário cadastrado com este e-mail.");
            }
        });

        if (user.getCpf() != null && !user.getCpf().isBlank()) {
            userRepository.findByCpf(user.getCpf()).ifPresent(other -> {
                if (!other.getId().equals(id)) {
                    throw new DuplicateResourceException("Já existe um usuário cadastrado com este CPF.");
                }
            });
        }

        existing.setName(user.getName());
        existing.setEmail(user.getEmail());
        existing.setPhone(user.getPhone());
        existing.setCpf(user.getCpf());
        existing.setActive(user.isActive());

        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            existing.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        return userRepository.save(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public User findById(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
    }

    @Override
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        User user = findById(id);
        userRepository.delete(user);
    }

    @Override
    @Transactional(readOnly = true)
    public User authenticate(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("E-mail ou senha inválidos."));

        if (!user.isActive()) {
            throw new BusinessException("Usuário inativo. Contate a secretaria.");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException("E-mail ou senha inválidos.");
        }

        return user;
    }

    @Override
    public void validateAccess(User requestingUser, UserType... allowedTypes) {
        if (requestingUser == null || !Arrays.asList(allowedTypes).contains(requestingUser.getType())) {
            throw new AccessDeniedException("Seu perfil não tem permissão para acessar este recurso.");
        }
    }

    @Override
    public boolean hasFullManagementAccess(User user) {
        return user != null && user.getType() == UserType.ADMINISTRATION;
    }

    @Override
    public boolean hasAdminPanelAccess(User user) {
        return user != null
                && (user.getType() == UserType.ADMINISTRATION || user.getType() == UserType.MANAGEMENT);
    }
}