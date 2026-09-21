package com.andretti101.escolaweb.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseMigrationFixer {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void fixDatabase() {
        try {
            jdbcTemplate.execute("ALTER TABLE chat_messages ALTER COLUMN classroom_id DROP NOT NULL");
            System.out.println("FIX: chat_messages classroom_id dropped NOT NULL");
        } catch (Exception e) {
            System.out.println("FIX ERROR: " + e.getMessage());
        }

        try {
            jdbcTemplate.execute("ALTER TABLE chat_read_receipts ALTER COLUMN classroom_id DROP NOT NULL");
            System.out.println("FIX: chat_read_receipts classroom_id dropped NOT NULL");
        } catch (Exception e) {
            System.out.println("FIX ERROR: " + e.getMessage());
        }
    }
}
