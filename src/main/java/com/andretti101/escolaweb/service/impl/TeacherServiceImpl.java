package com.andretti101.escolaweb.service.impl;

import com.andretti101.escolaweb.dto.request.TeacherPatchRequestDTO;
import com.andretti101.escolaweb.model.entity.Subject;
import com.andretti101.escolaweb.model.entity.Teacher;
import com.andretti101.escolaweb.repository.SubjectRepository;
import com.andretti101.escolaweb.repository.TeacherClassSubjectRepository;
import com.andretti101.escolaweb.repository.TeacherRepository;
import com.andretti101.escolaweb.repository.UserRepository;
import com.andretti101.escolaweb.service.TeacherService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.andretti101.escolaweb.model.entity.TeacherClassSubject;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherServiceImpl implements TeacherService {

    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherClassSubjectRepository teacherClassSubjectRepository;
    private final PasswordEncoder passwordEncoder;
    private final org.springframework.context.MessageSource messageSource;

    @Override
    @Transactional
    public Teacher create(Teacher teacher) {
        if (userRepository.existsByEmail(teacher.getEmail())) {
            throw new IllegalStateException(messageSource.getMessage("error.email.in.use", null, org.springframework.context.i18n.LocaleContextHolder.getLocale()));
        }
        teacher.setPassword(passwordEncoder.encode(teacher.getPassword()));
        teacher.setActive(true);
        teacher.setSubjects(resolveSubjects(teacher.getSubjects()));
        return teacherRepository.save(teacher);
    }

    @Override
    @Transactional
    public Teacher update(Integer id, Teacher incoming) {
        Teacher existing = findTeacherOrThrow(id);

        if (!existing.getEmail().equals(incoming.getEmail())
                && userRepository.existsByEmailAndIdNot(incoming.getEmail(), id)) {
            throw new IllegalStateException(messageSource.getMessage("error.email.in.use", null, org.springframework.context.i18n.LocaleContextHolder.getLocale()));
        }

        existing.setName(incoming.getName());
        existing.setEmail(incoming.getEmail());

        String incomingPassword = incoming.getPassword();
        if (incomingPassword != null && !incomingPassword.isBlank() && !incomingPassword.equals("SenhaTemporaria123")) {
            existing.setPassword(passwordEncoder.encode(incomingPassword));
        }

        if (incoming.getSubjects() != null) {
            Set<Subject> newSubjects = resolveSubjects(incoming.getSubjects());
            Set<Integer> newSubjectIds = newSubjects.stream().map(Subject::getId).collect(Collectors.toSet());

            List<TeacherClassSubject> activeAssignments = teacherClassSubjectRepository.findByTeacherIdAndActiveTrue(id);
            for (TeacherClassSubject tcs : activeAssignments) {
                if (tcs.getClassRoom() != null && !tcs.getClassRoom().isActive()) {
                    continue;
                }
                if (tcs.getClassRoom() != null && tcs.getClassRoom().getAcademicYear() != null && !tcs.getClassRoom().getAcademicYear().isActive()) {
                    continue;
                }
                if (!newSubjectIds.contains(tcs.getSubject().getId())) {
                    throw new IllegalStateException("Não é possível remover a habilitação da disciplina '"
                            + tcs.getSubject().getName() + "', pois o professor está lecionando ativamente na turma '"
                            + tcs.getClassRoom().getName() + "'.");
                }
            }
            existing.setSubjects(newSubjects);
        }

        return teacherRepository.save(existing);
    }

    @Override
    @Transactional
    public Teacher patch(Integer id, TeacherPatchRequestDTO dto) {
        Teacher existing = findTeacherOrThrow(id);

        if (dto.name() != null) {
            if (dto.name().isBlank()) {
                throw new IllegalArgumentException("O nome não pode ser vazio.");
            }
            existing.setName(dto.name().trim());
        }

        if (dto.email() != null) {
            if (dto.email().isBlank()) {
                throw new IllegalArgumentException("O e-mail não pode ser vazio.");
            }
            if (!existing.getEmail().equalsIgnoreCase(dto.email().trim())
                    && userRepository.existsByEmailAndIdNot(dto.email().trim(), id)) {
                throw new IllegalStateException(messageSource.getMessage("error.email.in.use", null, org.springframework.context.i18n.LocaleContextHolder.getLocale()));
            }
            existing.setEmail(dto.email().trim());
        }

        String incomingPassword = dto.password();
        if (incomingPassword != null && !incomingPassword.isBlank() && !incomingPassword.equals("SenhaTemporaria123")) {
            existing.setPassword(passwordEncoder.encode(incomingPassword));
        }

        if (dto.subjectIds() != null) {
            Set<Subject> subjectStubs = dto.subjectIds().stream()
                    .filter(java.util.Objects::nonNull)
                    .map(subId -> {
                        Subject s = new Subject();
                        s.setId(subId);
                        return s;
                    })
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            Set<Subject> newSubjects = resolveSubjects(subjectStubs);
            Set<Integer> newSubjectIds = newSubjects.stream().map(Subject::getId).collect(Collectors.toSet());

            List<TeacherClassSubject> activeAssignments = teacherClassSubjectRepository.findByTeacherIdAndActiveTrue(id);
            for (TeacherClassSubject tcs : activeAssignments) {
                if (tcs.getClassRoom() != null && !tcs.getClassRoom().isActive()) {
                    continue;
                }
                if (tcs.getClassRoom() != null && tcs.getClassRoom().getAcademicYear() != null && !tcs.getClassRoom().getAcademicYear().isActive()) {
                    continue;
                }
                if (!newSubjectIds.contains(tcs.getSubject().getId())) {
                    throw new IllegalStateException("Não é possível remover a habilitação da disciplina '"
                            + tcs.getSubject().getName() + "', pois o professor está lecionando ativamente na turma '"
                            + tcs.getClassRoom().getName() + "'.");
                }
            }
            existing.setSubjects(newSubjects);
        }

        return teacherRepository.save(existing);
    }

    private Set<Subject> resolveSubjects(Set<Subject> subjects) {
        Set<Subject> resolved = new LinkedHashSet<>();
        if (subjects == null || subjects.isEmpty()) {
            return resolved;
        }
        for (Subject ref : subjects) {
            if (ref == null || ref.getId() == null) {
                continue;
            }
            Subject subject = subjectRepository.findById(ref.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Disciplina não encontrada."));
            resolved.add(subject);
        }
        return resolved;
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        Teacher teacher = findTeacherOrThrow(id);

        if (teacherClassSubjectRepository.existsByTeacher(teacher)) {
            throw new IllegalStateException(
                    "Não é possível excluir o professor '" + teacher.getName() + "' pois ele possui atribuições de turmas ativas. Remova as atribuições antes de excluir.");
        }

        teacherRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Teacher findById(Integer id) {
        return findTeacherOrThrow(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Teacher> findAll() {
        return teacherRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Teacher> findAllActive() {
        return teacherRepository.findByActiveTrue();
    }

    @Override
    @Transactional
    public Teacher activate(Integer id) {
        Teacher teacher = findTeacherOrThrow(id);
        teacher.setActive(true);
        return teacher;
    }

    @Override
    @Transactional
    public Teacher deactivate(Integer id) {
        Teacher teacher = findTeacherOrThrow(id);
        teacher.setActive(false);
        return teacher;
    }

    Teacher findTeacherOrThrow(Integer id) {
        return teacherRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(messageSource.getMessage("error.teacher.not.found", new Object[]{id}, org.springframework.context.i18n.LocaleContextHolder.getLocale())));
    }
}
