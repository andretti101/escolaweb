package com.andretti101.escolaweb.service.impl;

import com.andretti101.escolaweb.dto.response.PagedResponse;
import com.andretti101.escolaweb.dto.response.ReportCardDTO;
import com.andretti101.escolaweb.dto.response.UserStatsDTO;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

import java.time.LocalDate;
import java.util.*;

@Slf4j
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
        return mapUsersToSummaries(users);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<UserSummaryDTO> findSummaries(UserRole role, Boolean active, Pageable pageable) {
        return findSummaries(role != null ? List.of(role) : null, active, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<UserSummaryDTO> findSummaries(List<UserRole> roles, Boolean active, Pageable pageable) {
        // Consulta paginada com ordenação padrão por nome alfabético e filtros combinados
        Pageable effectivePageable = (pageable == null || pageable.isUnpaged())
                ? Pageable.unpaged()
                : (pageable.getSort().isUnsorted()
                    ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("name").ascending())
                    : pageable);

        Page<User> page;
        if (roles == null || roles.isEmpty()) {
            page = userRepository.findUsersFilteredPageable(null, active, effectivePageable);
        } else if (roles.size() == 1) {
            page = userRepository.findUsersFilteredPageable(roles.get(0), active, effectivePageable);
        } else {
            page = userRepository.findUsersByRolesFilteredPageable(roles, active, effectivePageable);
        }

        List<User> users = page.getContent();
        if (users.isEmpty()) {
            return PagedResponse.of(List.of(), page.getPageable(), page.getTotalElements());
        }

        List<UserSummaryDTO> dtoList = mapUsersToSummaries(users);
        return PagedResponse.of(dtoList, page.getPageable(), page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<UserSummaryDTO> findSummaries(List<UserRole> roles, Boolean active, String search, String situation, Pageable pageable) {
        boolean hasSearch = search != null && !search.isBlank();
        boolean hasSituation = situation != null && !situation.isBlank() && !"ALL".equalsIgnoreCase(situation);
        if (!hasSearch && !hasSituation) {
            return findSummaries(roles, active, pageable);
        }

        List<User> candidateUsers;
        if (roles == null || roles.isEmpty()) {
            candidateUsers = userRepository.findUsersFiltered(null, active);
        } else if (roles.size() == 1) {
            candidateUsers = userRepository.findUsersFiltered(roles.get(0), active);
        } else {
            candidateUsers = userRepository.findUsersFiltered(null, active).stream()
                    .filter(u -> roles.contains(u.getRole()))
                    .toList();
        }

        List<UserSummaryDTO> allSummaries = candidateUsers.isEmpty() ? List.of() : mapUsersToSummaries(candidateUsers);
        String q = hasSearch ? search.trim().toLowerCase(Locale.ROOT) : "";
        String sit = hasSituation ? situation.trim().toUpperCase(Locale.ROOT) : "";

        List<UserSummaryDTO> filtered = allSummaries.stream()
                .filter(u -> {
                    if (hasSituation) {
                        if (u.role() != UserRole.STUDENT) {
                            return false;
                        }
                        if ("UNENROLLED".equals(sit)) {
                            if (u.className() != null && !u.className().isBlank()) return false;
                        } else if ("PENDING".equals(sit)) {
                            if (u.className() == null || u.className().isBlank()) return false;
                            if (u.generalSituation() != null && u.generalSituation() != StudentSituation.PENDING) return false;
                        } else {
                            if (u.generalSituation() == null || !u.generalSituation().name().equalsIgnoreCase(sit)) {
                                return false;
                            }
                        }
                    }
                    if (hasSearch) {
                        boolean matchName = u.name() != null && u.name().toLowerCase(Locale.ROOT).contains(q);
                        boolean matchEmail = u.email() != null && u.email().toLowerCase(Locale.ROOT).contains(q);
                        boolean matchEnroll = u.enrollmentNumber() != null && u.enrollmentNumber().toLowerCase(Locale.ROOT).contains(q);
                        boolean matchClass = u.className() != null && u.className().toLowerCase(Locale.ROOT).contains(q);
                        boolean matchYear = u.academicYear() != null && u.academicYear().toLowerCase(Locale.ROOT).contains(q);
                        boolean matchSubj = u.subjects() != null && u.subjects().stream().anyMatch(s -> s != null && s.toLowerCase(Locale.ROOT).contains(q));
                        boolean matchRooms = u.classrooms() != null && u.classrooms().stream().anyMatch(c -> c != null && c.toLowerCase(Locale.ROOT).contains(q));
                        return matchName || matchEmail || matchEnroll || matchClass || matchYear || matchSubj || matchRooms;
                    }
                    return true;
                })
                .toList();

        Pageable effectivePageable = (pageable == null || pageable.isUnpaged())
                ? PageRequest.of(0, Math.max(1, filtered.size()), Sort.by("name").ascending())
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                        pageable.getSort().isUnsorted() ? Sort.by("name").ascending() : pageable.getSort());

        int start = (int) Math.min((long) effectivePageable.getPageNumber() * effectivePageable.getPageSize(), filtered.size());
        int end = Math.min(start + effectivePageable.getPageSize(), filtered.size());
        List<UserSummaryDTO> pageSlice = filtered.subList(start, end);

        return PagedResponse.of(pageSlice, effectivePageable, filtered.size());
    }

    // Calcula métricas consolidadas leves para os cards do dashboard (sem carregar registros inteiros na memória)
    @Override
    @Transactional(readOnly = true)
    public UserStatsDTO getUserStats(Boolean active) {
        List<Object[]> counts = userRepository.countByRoleGrouped(active);
        long totalUsers = 0;
        long totalStaff = 0;
        long totalTeachers = 0;
        long totalStudents = 0;

        for (Object[] row : counts) {
            UserRole r = (UserRole) row[0];
            long cnt = ((Number) row[1]).longValue();
            totalUsers += cnt;
            if (r == UserRole.PRINCIPAL || r == UserRole.SECRETARY) {
                totalStaff += cnt;
            } else if (r == UserRole.TEACHER) {
                totalTeachers += cnt;
            } else if (r == UserRole.STUDENT) {
                totalStudents += cnt;
            }
        }

        long approved = 0;
        long pending = 0;
        long failed = 0;

        if (totalStudents > 0 && (active == null || Boolean.TRUE.equals(active))) {
            try {
                AcademicYear activeYear = academicYearRepository.findByActiveTrue().orElse(null);
                if (activeYear != null) {
                    List<Enrollment> activeEnrollments = enrollmentRepository.findByActiveTrue();
                    Map<Integer, Enrollment> studentActiveEnrollmentMap = new HashMap<>();
                    for (Enrollment e : activeEnrollments) {
                        if (e.getStudent() != null && e.getStudent().getId() != null
                                && (active == null || e.getStudent().isActive() == active)) {
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

                    List<Enrollment> currentYearEnrollments = studentActiveEnrollmentMap.values().stream()
                            .filter(e -> e.getClassRoom() != null
                                    && e.getClassRoom().getAcademicYear() != null
                                    && e.getClassRoom().getAcademicYear().getId().equals(activeYear.getId()))
                            .toList();

                    if (!currentYearEnrollments.isEmpty()) {
                        List<ReportCardDTO> reports = reportCardService.generateReportCardsBatch(currentYearEnrollments, activeYear);
                        for (ReportCardDTO r : reports) {
                            if (r.generalSituation() == StudentSituation.APPROVED) {
                                approved++;
                            } else if (r.generalSituation() == StudentSituation.FAILED) {
                                failed++;
                            } else {
                                pending++;
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                log.debug("Não foi possível consolidar estatísticas acadêmicas no UserServiceImpl: {}", ex.getMessage());
            }
        }

        long remaining = totalStudents - (approved + failed + pending);
        if (remaining > 0) {
            pending += remaining;
        }

        return new UserStatsDTO(
                totalUsers,
                totalStaff,
                totalTeachers,
                totalStudents,
                approved,
                pending,
                failed
        );
    }

    // Mapeia usuários para DTOs, calculando boletins estritamente para os alunos da página atual
    private List<UserSummaryDTO> mapUsersToSummaries(List<User> users) {
        Map<Integer, Enrollment> studentActiveEnrollmentMap = new HashMap<>();
        Map<Integer, StudentSituation> studentSituationMap = new HashMap<>();

        Set<Integer> studentIds = users.stream()
                .filter(u -> u instanceof Student)
                .map(User::getId)
                .collect(Collectors.toSet());

        if (!studentIds.isEmpty()) {
            List<Enrollment> activeEnrollments = enrollmentRepository.findByStudent_IdInAndActiveTrue(studentIds);
            if (activeEnrollments == null || activeEnrollments.isEmpty()) {
                activeEnrollments = enrollmentRepository.findByActiveTrue();
            }
            if (activeEnrollments != null) {
                for (Enrollment e : activeEnrollments) {
                    if (e.getStudent() != null && e.getStudent().getId() != null
                            && studentIds.contains(e.getStudent().getId())) {
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
            }

            try {
                if (academicYearRepository != null && reportCardService != null) {
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
                }
            } catch (Exception ex) {
                log.debug("Não foi possível consolidar estatísticas acadêmicas no UserServiceImpl: {}", ex.getMessage());
            }
        }

        Map<Integer, Set<String>> teacherClassroomsMap = new HashMap<>();
        Map<Integer, Set<String>> teacherSubjectsMap = new HashMap<>();

        Set<Integer> teacherIds = users.stream()
                .filter(u -> u instanceof Teacher)
                .map(User::getId)
                .collect(Collectors.toSet());

        if (!teacherIds.isEmpty()) {
            List<TeacherClassSubject> tcsList = teacherClassSubjectRepository.findByTeacher_IdInAndActiveTrue(teacherIds);
            if (tcsList == null || tcsList.isEmpty()) {
                tcsList = teacherClassSubjectRepository.findAll();
            }
            if (tcsList != null) {
                for (TeacherClassSubject tcs : tcsList) {
                    if (tcs.isActive() && tcs.getTeacher() != null && tcs.getTeacher().getId() != null
                            && teacherIds.contains(tcs.getTeacher().getId())) {
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
