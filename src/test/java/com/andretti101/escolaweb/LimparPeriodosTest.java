package com.andretti101.escolaweb;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
public class LimparPeriodosTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @Transactional
    @Commit
    public void limparProvasEPeriodos() {
        System.out.println("\n\n=============================================");
        System.out.println("INICIANDO LIMPEZA DE DADOS...");
        
        int grades = jdbcTemplate.update("DELETE FROM grades");
        int assessments = jdbcTemplate.update("DELETE FROM assessments");
        int periods = jdbcTemplate.update("DELETE FROM academic_periods");
        
        System.out.println("Deletadas " + grades + " notas.");
        System.out.println("Deletadas " + assessments + " provas (avaliacoes).");
        System.out.println("Deletados " + periods + " periodos academicos.");
        System.out.println("=============================================\n\n");
    }
}
