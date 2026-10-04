package com.andretti101.escolaweb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
public class EscolawebApplication {
     
    public static void main(String[] args) {
        SpringApplication.run(EscolawebApplication.class, args);
    }

    @Bean
    public CommandLineRunner dbCleanup(JdbcTemplate jdbcTemplate) {
        return args -> {
            int deleted = jdbcTemplate.update("DELETE FROM chat_message_history WHERE content LIKE 'Could not initialize proxy%'");
            System.out.println(">>> CLEANUP: Deleted " + deleted + " corrupted chat messages from history.");
        };
    }
}