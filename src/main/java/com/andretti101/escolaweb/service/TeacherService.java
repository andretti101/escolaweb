package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.model.entity.Teacher;
import com.andretti101.escolaweb.model.entity.User;
import com.andretti101.escolaweb.repository.TeacherRepository;
import com.andretti101.escolaweb.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;

    public Teacher create(Integer userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Usuário não encontrado."));

        Teacher teacher = Teacher.builder()
                .user(user)
                .build();

        return teacherRepository.save(teacher);
    }

    public Teacher findById(Integer id) {

        return teacherRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Professor não encontrado."));
    }

    public List<Teacher> findAll() {
        return teacherRepository.findAll();
    }

    public Teacher update(Integer id, Teacher updatedTeacher) {

        Teacher teacher = findById(id);

        return teacherRepository.save(teacher);
    }

    public void delete(Integer id) {

        Teacher teacher = findById(id);

        teacherRepository.delete(teacher);
    }
}