package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.model.entity.User;
import com.andretti101.escolaweb.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public User create(User user) {

        if (repository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("E-mail já cadastrado.");
        }

        if (user.getCpf() != null &&
                repository.existsByCpf(user.getCpf())) {
            throw new RuntimeException("CPF já cadastrado.");
        }

        user.setPassword(
                passwordEncoder.encode(user.getPassword())
        );

        return repository.save(user);
    }

    public List<User> findAll() {
        return repository.findAll();
    }

    public User findById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Usuário não encontrado."));
    }

    public User update(Integer id, User data) {

        User user = findById(id);

        user.setName(data.getName());
        user.setEmail(data.getEmail());
        user.setPhone(data.getPhone());
        user.setCpf(data.getCpf());
        user.setType(data.getType());
        user.setActive(data.isActive());

        return repository.save(user);
    }

    public void delete(Integer id) {
        findById(id);
        repository.deleteById(id);
    }
}