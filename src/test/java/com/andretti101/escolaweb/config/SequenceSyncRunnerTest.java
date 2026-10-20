package com.andretti101.escolaweb.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
class SequenceSyncRunnerTest {

    @Autowired
    private SequenceSyncRunner sequenceSyncRunner;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("SequenceSyncRunner.run deve executar sem lançar exceções")
    void testSequenceSyncRunnerExecutesWithoutError() {
        assertDoesNotThrow(() -> sequenceSyncRunner.run());
    }

    @Test
    @DisplayName("Todas as 23 tabelas e sequences existem no PostgreSQL e aceitam setval sem erro")
    void testAll21TablesAndSequencesExistInDatabase() {
        String[] tables = {
            "academic_periods", "academic_years", "announcements", "assessments",
            "attendance_history", "attendances", "chat_hidden_messages", "chat_message_history",
            "chat_messages", "chat_read_receipts", "class_rooms", "enrollments",
            "grade_history", "grades", "lessons", "meal_menu_slots", "password_reset_tokens",
            "refresh_tokens", "school_settings", "subjects", "teacher_class_subject",
            "timetable_slots", "users"
        };

        for (String table : tables) {
            String sql = String.format(
                "SELECT setval('%s_id_seq', COALESCE((SELECT MAX(id)+1 FROM %s), 1), false)",
                table, table
            );
            assertDoesNotThrow(() -> jdbcTemplate.execute(sql), "Falha ao executar setval para a tabela: " + table);
        }
    }
}
