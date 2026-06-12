package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.model.entity.Student;
import com.andretti101.escolaweb.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository repository;

    public Student create(Student student) {

        if (repository.existsByEnrollment(student.getEnrollment())) {
            throw new RuntimeException("Matrícula já cadastrada.");
        }

        return repository.save(student);
    }

    public List<Student> findAll() {
        return repository.findAll();
    }

    public Student findById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Aluno não encontrado."));
    }

    public Student update(Integer id, Student data) {

        Student student = findById(id);

        student.setEnrollment(data.getEnrollment());
        student.setBirthDate(data.getBirthDate());
        student.setClassRoom(data.getClassRoom());

        return repository.save(student);
    }

    public void delete(Integer id) {
        findById(id);
        repository.deleteById(id);
    }
}