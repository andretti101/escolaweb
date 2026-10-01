package com.andretti101.escolaweb;

import com.andretti101.escolaweb.dto.request.YearConclusionRequestDTO;
import com.andretti101.escolaweb.model.entity.AcademicYear;
import com.andretti101.escolaweb.model.entity.ClassRoom;
import com.andretti101.escolaweb.model.enums.AcademicPeriodType;
import com.andretti101.escolaweb.repository.AcademicYearRepository;
import com.andretti101.escolaweb.repository.ClassRoomRepository;
import com.andretti101.escolaweb.service.AcademicYearConclusionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ConcludeYearDebug {

    @Autowired
    private AcademicYearConclusionService conclusionService;

    @Autowired
    private ClassRoomRepository classRoomRepository;

    @Autowired
    private AcademicYearRepository academicYearRepository;

    @Autowired
    private com.andretti101.escolaweb.repository.AcademicPeriodRepository academicPeriodRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Test
    public void test() {
        try {
            jdbcTemplate.execute("ALTER TABLE grades ADD COLUMN active BOOLEAN DEFAULT true NOT NULL");
        } catch (Exception e) {}
        try {
            jdbcTemplate.execute("ALTER TABLE attendances ADD COLUMN active BOOLEAN DEFAULT true NOT NULL");
        } catch (Exception e) {}

        AcademicYear year = academicYearRepository.findByActiveTrue().orElseThrow();
        System.out.println("Active year before: " + year.getYear());
        
        List<ClassRoom> roomsBefore = classRoomRepository.findByActiveTrue();
        System.out.println("Active rooms before: " + roomsBefore.size());

        List<com.andretti101.escolaweb.model.entity.AcademicPeriod> periods = academicPeriodRepository.findByAcademicYear(year);
        if (periods.isEmpty()) {
            com.andretti101.escolaweb.model.entity.AcademicPeriod p = new com.andretti101.escolaweb.model.entity.AcademicPeriod();
            p.setAcademicYear(year);
            p.setName("Test Period");
            p.setClosed(true);
            academicPeriodRepository.save(p);
        } else {
            for (com.andretti101.escolaweb.model.entity.AcademicPeriod p : periods) {
                p.setClosed(true);
                academicPeriodRepository.save(p);
            }
        }

        YearConclusionRequestDTO req = new YearConclusionRequestDTO(
            2028, new BigDecimal("6.0"), new BigDecimal("75.0"), AcademicPeriodType.BIMESTER
        );

        conclusionService.concludeYear(req);

        List<ClassRoom> roomsAfter = classRoomRepository.findByActiveTrue();
        System.out.println("Active rooms after: " + roomsAfter.size());
        
        for (ClassRoom r : roomsAfter) {
            System.out.println("Room: " + r.getName() + " year: " + r.getAcademicYear().getYear());
        }
    }
}
