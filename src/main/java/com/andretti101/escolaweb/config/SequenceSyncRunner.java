package com.andretti101.escolaweb.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SequenceSyncRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SequenceSyncRunner.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        log.info("Sincronizando Auto-Increments (Sequences) do PostgreSQL...");
        String[] tables = {
            "academic_periods", "academic_years", "announcements", "assessments",
            "attendance_history", "attendances", "chat_hidden_messages", "chat_message_history",
            "chat_messages", "chat_read_receipts", "class_rooms", "enrollments",
            "grade_history", "grades", "lessons", "password_reset_tokens",
            "refresh_tokens", "school_settings", "subjects", "teacher_class_subject",
            "users"
        };
        
        for (String table : tables) {
            try {
                String sql = String.format(
                    "SELECT setval('%s_id_seq', COALESCE((SELECT MAX(id)+1 FROM %s), 1), false)", 
                    table, table
                );
                jdbcTemplate.execute(sql);
            } catch (Exception e) {
                log.warn("Aviso ao sincronizar sequence para a tabela {}: {}", table, e.getMessage());
            }
        }


        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM teacher_subjects", Integer.class);
            if (count != null && count == 0) {
                jdbcTemplate.execute(
                    "INSERT INTO teacher_subjects (teacher_id, subject_id) " +
                    "SELECT DISTINCT teacher_id, subject_id FROM teacher_class_subject " +
                    "WHERE teacher_id IS NOT NULL AND subject_id IS NOT NULL"
                );
            }
        } catch (Exception e) {
            // ignorar silenciosamente se as tabelas ainda não existirem
        }

        log.info("Sincronização de Sequences finalizada.");
    }
}
