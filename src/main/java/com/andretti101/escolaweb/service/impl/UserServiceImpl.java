package com.andretti101.escolaweb.service.impl;

import com.andretti101.escolaweb.dto.response.ReportCardDTO;
import com.andretti101.escolaweb.dto.response.UserSummaryDTO;
import com.andretti101.escolaweb.model.entity.AcademicYear;
import com.andretti101.escolaweb.model.entity.Enrollment;
import com.andretti101.escolaweb.model.entity.Student;
import com.andretti101.escolaweb.model.entity.Teacher;
import com.andretti101.escolaweb.model.entity.TeacherClassSubject;
import com.andretti101.escolaweb.model.entity.User;
import com.andretti101.escolaweb.model.enums.StudentSituation;
import com.andretti101.escolaweb.model.enums.UserRole;
import com.andretti101.escolaweb.repository.AcademicYearRepository;
import com.andretti101.escolaweb.repository.EnrollmentRepository;
import com.andretti101.escolaweb.repository.TeacherClassSubjectRepository;
import com.andretti101.escolaweb.repository.UserRepository;
import com.andretti101.escolaweb.service.ReportCardService;
import com.andretti101.escolaweb.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EnrollmentRepository enrollmentRepository;
    private final TeacherClassSubjectRepository teacherClassSubjectRepository;
    private final AcademicYearRepository academicYearRepository;
    private final ReportCardService reportCardService;

    @Override
    @Transactional(readOnly = true)
    public User findById(Integer id) {
        return findUserOrThrow(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAllActive() {
        return userRepository.findByActiveTrue();
    }

    @Override
    @Transactional
    public void changePassword(Integer id, String currentPassword, String newPassword) {
        User user = findUserOrThrow(id);

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("A senha atual informada está incorreta. Verifique e tente novamente.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
    }

    @Override
    @Transactional
    public void resetPassword(Integer id, String newPassword) {
        User user = findUserOrThrow(id);
        user.setPassword(passwordEncoder.encode(newPassword));
    }

    @Override
    @Transactional
    public User activate(Integer id) {
        User user = findUserOrThrow(id);
        user.setActive(true);
        return user;
    }

    @Override
    @Transactional
    public User deactivate(Integer id) {
        User user = findUserOrThrow(id);
        user.setActive(false);
        return user;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummaryDTO> findSummaries(UserRole role, Boolean active) {
        List<User> users = userRepository.findUsersFiltered(role, active);
        if (users.isEmpty()) {
            return List.of();
        }

        Map<Integer, Enrollment> studentActiveEnrollmentMap = new HashMap<>();
        Map<Integer, StudentSituation> studentSituationMap = new HashMap<>();
        boolean hasStudents = role == null || role == UserRole.STUDENT || users.stream().anyMatch(u -> u instanceof Student);
        if (hasStudents) {
            List<Enrollment> activeEnrollments = enrollmentRepository.findByActiveTrue();
            for (Enrollment e : activeEnrollments) {
                if (e.getStudent() != null && e.getStudent().getId() != null) {
                    Integer sId = e.getStudent().getId();
                    Enrollment current = studentActiveEnrollmentMap.get(sId);
                    if (current == null) {
                        studentActiveEnrollmentMap.put(sId, e);
                    } else if (current.getClassRoom() != null && !current.getClassRoom().isActive()
                            && e.getClassRoom() != null && e.getClassRoom().isActive()) {
                        studentActiveEnrollmentMap.put(sId, e);
                    }
                }
            }

            try {
                AcademicYear activeYear = academicYearRepository.findByActiveTrue().orElse(null);
                if (activeYear != null) {
                    List<Enrollment> currentYearEnrollments = studentActiveEnrollmentMap.values().stream()
                            .filter(e -> e.getClassRoom() != null
                                    && e.getClassRoom().getAcademicYear() != null
                                    && e.getClassRoom().getAcademicYear().getId().equals(activeYear.getId()))
                            .toList();
                    if (!currentYearEnrollments.isEmpty()) {
                        List<ReportCardDTO> reports = reportCardService.generateReportCardsBatch(currentYearEnrollments, activeYear);
                        for (ReportCardDTO r : reports) {
                            studentSituationMap.put(r.studentId(), r.generalSituation());
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        Map<Integer, Set<String>> teacherClassroomsMap = new HashMap<>();
        Map<Integer, Set<String>> teacherSubjectsMap = new HashMap<>();
        boolean hasTeachers = role == null || role == UserRole.TEACHER || users.stream().anyMatch(u -> u instanceof Teacher);
        if (hasTeachers) {
            List<TeacherClassSubject> tcsList = teacherClassSubjectRepository.findAll();
            for (TeacherClassSubject tcs : tcsList) {
                if (tcs.isActive() && tcs.getTeacher() != null && tcs.getTeacher().getId() != null) {
                    Integer tId = tcs.getTeacher().getId();
                    if (tcs.getClassRoom() != null && tcs.getClassRoom().isActive() && tcs.getClassRoom().getName() != null) {
                        teacherClassroomsMap.computeIfAbsent(tId, k -> new TreeSet<>()).add(tcs.getClassRoom().getName());
                    }
                    if (tcs.getSubject() != null && tcs.getSubject().getName() != null) {
                        teacherSubjectsMap.computeIfAbsent(tId, k -> new TreeSet<>()).add(tcs.getSubject().getName());
                    }
                }
            }
        }

        return users.stream().map(u -> {
            String enrollmentNumber = null;
            String className = null;
            Integer classroomId = null;
            String academicYear = null;
            List<String> subjects = List.of();
            List<String> classrooms = List.of();
            StudentSituation generalSituation = null;
            String ownPhone = null;
            String guardianPhone = null;
            LocalDate birthDate = null;

            if (u instanceof Student student) {
                enrollmentNumber = student.getRegistrationNumber();
                ownPhone = student.getOwnPhone();
                guardianPhone = student.getGuardianPhone();
                birthDate = student.getBirthDate();

                Enrollment enr = studentActiveEnrollmentMap.get(student.getId());
                if (enr != null && enr.getClassRoom() != null) {
                    className = enr.getClassRoom().getName();
                    classroomId = enr.getClassRoom().getId();
                    if (enr.getClassRoom().getAcademicYear() != null && enr.getClassRoom().getAcademicYear().getYear() != null) {
                        academicYear = String.valueOf(enr.getClassRoom().getAcademicYear().getYear());
                    }
                    generalSituation = studentSituationMap.get(student.getId());
                    if (generalSituation == null) {
                        generalSituation = enr.getGeneralSituation();
                    }
                }
            } else if (u instanceof Teacher teacher) {
                Set<String> subSet = new TreeSet<>();
                if (teacher.getSubjects() != null) {
                    teacher.getSubjects().forEach(s -> {
                        if (s != null && s.getName() != null) subSet.add(s.getName());
                    });
                }
                if (teacherSubjectsMap.containsKey(teacher.getId())) {
                    subSet.addAll(teacherSubjectsMap.get(teacher.getId()));
                }
                subjects = new ArrayList<>(subSet);

                if (teacherClassroomsMap.containsKey(teacher.getId())) {
                    classrooms = new ArrayList<>(teacherClassroomsMap.get(teacher.getId()));
                }
            }

            return new UserSummaryDTO(
                    u.getId(),
                    u.getName(),
                    u.getEmail(),
                    u.getRole(),
                    u.isActive(),
                    u.getCreatedAt(),
                    enrollmentNumber,
                    className,
                    classroomId,
                    academicYear,
                    subjects,
                    classrooms,
                    generalSituation,
                    ownPhone,
                    guardianPhone,
                    birthDate
            );
        }).toList();
    }

    private User findUserOrThrow(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado."));
    }
}
