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
            "academic_periods", "academic_years", "attendance_history", 
            "attendances", "class_rooms", "enrollments", "lessons", 
            "students", "subjects", "teacher_class_subjects", "teachers", "users",
            "assessments", "grades"
        };
        
        for (String table : tables) {
            try {
                String sql = String.format(
                    "SELECT setval('%s_id_seq', COALESCE((SELECT MAX(id)+1 FROM %s), 1), false)", 
                    table, table
                );
                jdbcTemplate.execute(sql);
            } catch (Exception e) {
                // ignorar silenciosamente as tabelas que derem falha
            }
        }
        log.info("Sincronização de Sequences finalizada.");
    }
}
