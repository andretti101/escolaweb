package com.andretti101.escolaweb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

// Ponto de entrada principal da aplicação Spring Boot EscolaWeb
@SpringBootApplication
public class EscolawebApplication {
     
    public static void main(String[] args) {
        SpringApplication.run(EscolawebApplication.class, args);
    }

    // Executado na inicialização para remover eventuais mensagens corrompidas no histórico de chat
    @Bean
    public CommandLineRunner dbCleanup(JdbcTemplate jdbcTemplate) {
        return args -> {
            int deleted = jdbcTemplate.update("DELETE FROM chat_message_history WHERE content LIKE 'Could not initialize proxy%'");
            System.out.println(">>> CLEANUP: Deleted " + deleted + " corrupted chat messages from history.");
        };
    }
}