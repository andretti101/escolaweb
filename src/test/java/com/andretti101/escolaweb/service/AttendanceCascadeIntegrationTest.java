package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.model.entity.*;
import com.andretti101.escolaweb.model.enums.AttendanceStatus;
import com.andretti101.escolaweb.model.enums.UserRole;
import com.andretti101.escolaweb.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AttendanceCascadeIntegrationTest {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private AttendanceHistoryRepository attendanceHistoryRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Deletar Attendance cascateia exclusão em AttendanceHistory sem violar integridade")
    void deleteAttendance_cascadesToAttendanceHistory() {
        Attendance attendance = attendanceRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Nenhuma presença encontrada no banco"));
        User user = userRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Nenhum usuário encontrado no banco"));

        AttendanceHistory history = AttendanceHistory.builder()
                .attendance(attendance)
                .previousStatus("PRESENT")
                .newStatus("JUSTIFIED_ABSENCE")
                .changedBy(user)
                .changedAt(LocalDateTime.now())
                .build();

        attendanceHistoryRepository.save(history);

        entityManager.flush();
        entityManager.clear();

        Integer attendanceId = attendance.getId();
        Integer historyId = history.getId();

        assertTrue(attendanceRepository.existsById(attendanceId));
        assertTrue(attendanceHistoryRepository.existsById(historyId));

        // Realiza a exclusão da presença
        attendanceRepository.deleteById(attendanceId);

        entityManager.flush();
        entityManager.clear();

        assertFalse(attendanceRepository.existsById(attendanceId), "Presença deveria ter sido excluída");
        assertFalse(attendanceHistoryRepository.existsById(historyId), "Histórico de presença deveria ter sido excluído em cascata");
    }

    @Test
    @DisplayName("Deletar User cascateia exclusão em RefreshToken sem violar integridade")
    void deleteUser_cascadesToRefreshTokens() {
        User tempUser = Teacher.builder()
                .name("Temp Cascade Teacher")
                .email("temp.cascade.teacher@example.com")
                .password("hash123")
                .role(UserRole.TEACHER)
                .active(true)
                .build();

        tempUser = userRepository.save(tempUser);

        RefreshToken token1 = RefreshToken.builder()
                .token("cascade-token-1")
                .user(tempUser)
                .revoked(false)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();

        RefreshToken token2 = RefreshToken.builder()
                .token("cascade-token-2")
                .user(tempUser)
                .revoked(true)
                .expiresAt(LocalDateTime.now().plusDays(2))
                .build();

        refreshTokenRepository.save(token1);
        refreshTokenRepository.save(token2);

        entityManager.flush();
        entityManager.clear();

        Integer userId = tempUser.getId();
        assertTrue(userRepository.existsById(userId));
        assertTrue(refreshTokenRepository.findByToken("cascade-token-1").isPresent());
        assertTrue(refreshTokenRepository.findByToken("cascade-token-2").isPresent());

        // Deleta o usuário
        userRepository.deleteById(userId);

        entityManager.flush();
        entityManager.clear();

        assertFalse(userRepository.existsById(userId));
        assertTrue(refreshTokenRepository.findByToken("cascade-token-1").isEmpty());
        assertTrue(refreshTokenRepository.findByToken("cascade-token-2").isEmpty());
    }
}
