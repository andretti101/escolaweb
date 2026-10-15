package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.model.entity.*;
import com.andretti101.escolaweb.model.enums.AcademicPeriodType;
import com.andretti101.escolaweb.model.enums.SchoolGrade;
import com.andretti101.escolaweb.model.enums.Shift;
import com.andretti101.escolaweb.model.enums.UserRole;
import com.andretti101.escolaweb.repository.*;
import com.andretti101.escolaweb.dto.request.TeacherUpdateRequestDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TeacherClassSubjectIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AcademicYearRepository academicYearRepository;

    @Autowired
    private AcademicPeriodRepository academicPeriodRepository;

    @Autowired
    private ClassRoomRepository classRoomRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private TeacherClassSubjectRepository teacherClassSubjectRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private ClassRoom classRoom;
    private Teacher teacher;
    private Subject mathSubject;
    private Subject peSubject;
    private Subject artsSubject;

    @BeforeEach
    void setUp() {
        AcademicYear activeYear = academicYearRepository.findByActiveTrue().orElseGet(() ->
                academicYearRepository.save(AcademicYear.builder()
                        .year(2026)
                        .startDate(LocalDate.of(2026, 2, 1))
                        .endDate(LocalDate.of(2026, 12, 15))
                        .active(true)
                        .build()));

        academicPeriodRepository.findByAcademicYearAndPeriodTypeOrderByIdAsc(activeYear, AcademicPeriodType.TRIMESTER)
                .stream().findFirst().orElseGet(() ->
                        academicPeriodRepository.save(AcademicPeriod.builder()
                                .name("1º Trimestre")
                                .periodType(AcademicPeriodType.TRIMESTER)
                                .startDate(LocalDate.of(2026, 2, 1))
                                .endDate(LocalDate.of(2026, 5, 10))
                                .closed(false)
                                .academicYear(activeYear)
                                .build()));

        classRoom = classRoomRepository.save(ClassRoom.builder()
                .name("201 Teste Grade")
                .shift(Shift.MORNING)
                .periodType(AcademicPeriodType.TRIMESTER)
                .schoolGrade(SchoolGrade.MEDIO_2)
                .academicYear(activeYear)
                .active(true)
                .build());

        teacher = teacherRepository.save(Teacher.builder()
                .name("Prof Carlos Teste")
                .email("prof.carlos.tcs.teste@example.com")
                .password(passwordEncoder.encode("senha123"))
                .role(UserRole.TEACHER)
                .active(true)
                .build());

        mathSubject = subjectRepository.save(Subject.builder()
                .name("Matemática Teste TCS")
                .active(true)
                .build());

        peSubject = subjectRepository.save(Subject.builder()
                .name("Educação Física Teste TCS")
                .active(true)
                .build());

        artsSubject = subjectRepository.save(Subject.builder()
                .name("Artes Teste TCS")
                .active(true)
                .build());
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("Vincular professor em matéria, excluir e vincular em outra matéria não deve reter nem recriar matérias excluídas")
    void createDeleteAndRecreateWithDifferentSubjects_onlyKeepsLatestSubject() throws Exception {
        // 1. Cadastra professor para Matemática na turma
        String mathPayload = objectMapper.writeValueAsString(Map.of(
                "teacherId", teacher.getId(),
                "classRoomId", classRoom.getId(),
                "subjectId", mathSubject.getId(),
                "minAssessmentsPerPeriod", 1,
                "maxAssessmentsPerPeriod", 4
        ));

        String mathResponse = mockMvc.perform(post("/api/teacher-class-subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mathPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subjectId").value(mathSubject.getId()))
                .andReturn().getResponse().getContentAsString();

        int mathTcsId = objectMapper.readTree(mathResponse).get("id").asInt();

        // Inicializa a coleção do professor na sessão para garantir que o cascade não ressuscite o vínculo excluído
        if (teacher.getTeacherClassSubjects() != null) {
            teacher.getTeacherClassSubjects().size();
        }

        // 2. Exclui o vínculo de Matemática
        mockMvc.perform(delete("/api/teacher-class-subjects/{id}", mathTcsId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/teacher-class-subjects/classroom/{id}", classRoom.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // 3. Cadastra como Educação Física
        String pePayload = objectMapper.writeValueAsString(Map.of(
                "teacherId", teacher.getId(),
                "classRoomId", classRoom.getId(),
                "subjectId", peSubject.getId(),
                "minAssessmentsPerPeriod", 1,
                "maxAssessmentsPerPeriod", 4
        ));

        String peResponse = mockMvc.perform(post("/api/teacher-class-subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pePayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subjectId").value(peSubject.getId()))
                .andReturn().getResponse().getContentAsString();

        int peTcsId = objectMapper.readTree(peResponse).get("id").asInt();

        // Deve conter APENAS Educação Física (não Matemática)
        mockMvc.perform(get("/api/teacher-class-subjects/classroom/{id}", classRoom.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(peTcsId))
                .andExpect(jsonPath("$[0].subjectId").value(peSubject.getId()));

        // 4. Exclui novamente (Educação Física)
        mockMvc.perform(delete("/api/teacher-class-subjects/{id}", peTcsId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/teacher-class-subjects/classroom/{id}", classRoom.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // 5. Cadastra como Artes
        String artsPayload = objectMapper.writeValueAsString(Map.of(
                "teacherId", teacher.getId(),
                "classRoomId", classRoom.getId(),
                "subjectId", artsSubject.getId(),
                "minAssessmentsPerPeriod", 1,
                "maxAssessmentsPerPeriod", 4
        ));

        JsonNode artsNode = objectMapper.readTree(
                mockMvc.perform(post("/api/teacher-class-subjects")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(artsPayload))
                        .andExpect(status().isCreated())
                        .andReturn().getResponse().getContentAsString()
        );

        // Deve conter APENAS Artes (não Educação Física nem Matemática) tanto por turma quanto por professor
        mockMvc.perform(get("/api/teacher-class-subjects/classroom/{id}", classRoom.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(artsNode.get("id").asInt()))
                .andExpect(jsonPath("$[0].subjectId").value(artsSubject.getId()));

        mockMvc.perform(get("/api/teacher-class-subjects/teacher/{id}", teacher.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(artsNode.get("id").asInt()))
                .andExpect(jsonPath("$[0].subjectId").value(artsSubject.getId()));

        assertEquals(1, teacherClassSubjectRepository.findByClassRoom(classRoom).size());
        assertEquals(1, teacherClassSubjectRepository.findByTeacher(teacher).size());
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("Excluir uma matéria quando o professor leciona múltiplas matérias na turma mantém apenas as não excluídas e permite revincular a matéria excluída")
    void partialDeleteAndRelinkSameSubject_maintainsAccurateSubjectList() throws Exception {
        // Vincula Matemática e Educação Física
        String mathResp = mockMvc.perform(post("/api/teacher-class-subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "teacherId", teacher.getId(),
                                "classRoomId", classRoom.getId(),
                                "subjectId", mathSubject.getId(),
                                "minAssessmentsPerPeriod", 1,
                                "maxAssessmentsPerPeriod", 4
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int mathTcsId = objectMapper.readTree(mathResp).get("id").asInt();

        String peResp = mockMvc.perform(post("/api/teacher-class-subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "teacherId", teacher.getId(),
                                "classRoomId", classRoom.getId(),
                                "subjectId", peSubject.getId(),
                                "minAssessmentsPerPeriod", 1,
                                "maxAssessmentsPerPeriod", 4
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int peTcsId = objectMapper.readTree(peResp).get("id").asInt();

        // Exclui apenas Matemática
        mockMvc.perform(delete("/api/teacher-class-subjects/{id}", mathTcsId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/teacher-class-subjects/classroom/{id}", classRoom.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(peTcsId))
                .andExpect(jsonPath("$[0].subjectId").value(peSubject.getId()));

        // Revincula a mesma matéria (Matemática) que havia sido excluída
        mockMvc.perform(post("/api/teacher-class-subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "teacherId", teacher.getId(),
                                "classRoomId", classRoom.getId(),
                                "subjectId", mathSubject.getId(),
                                "minAssessmentsPerPeriod", 2,
                                "maxAssessmentsPerPeriod", 3
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subjectId").value(mathSubject.getId()));

        mockMvc.perform(get("/api/teacher-class-subjects/classroom/{id}", classRoom.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("Professor com disciplinas habilitadas só pode ser vinculado em turma para uma de suas disciplinas habilitadas")
    void linkTeacherWithEnabledSubjects_allowsEnabledAndRejectsUnauthorizedSubject() throws Exception {
        java.util.Set<Subject> enabledSubjects = new java.util.LinkedHashSet<>();
        enabledSubjects.add(mathSubject);
        teacher.setSubjects(enabledSubjects);
        teacherRepository.saveAndFlush(teacher);

        // 1. Vincular na disciplina habilitada (Matemática) deve funcionar
        String allowedPayload = objectMapper.writeValueAsString(Map.of(
                "teacherId", teacher.getId(),
                "classRoomId", classRoom.getId(),
                "subjectId", mathSubject.getId(),
                "minAssessmentsPerPeriod", 1,
                "maxAssessmentsPerPeriod", 4
        ));

        String createdResp = mockMvc.perform(post("/api/teacher-class-subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(allowedPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subjectId").value(mathSubject.getId()))
                .andReturn().getResponse().getContentAsString();

        int tcsId = objectMapper.readTree(createdResp).get("id").asInt();

        // 2. Vincular em disciplina não habilitada (Artes) deve retornar 422 Unprocessable Entity
        String disallowedPayload = objectMapper.writeValueAsString(Map.of(
                "teacherId", teacher.getId(),
                "classRoomId", classRoom.getId(),
                "subjectId", artsSubject.getId(),
                "minAssessmentsPerPeriod", 1,
                "maxAssessmentsPerPeriod", 4
        ));

        mockMvc.perform(post("/api/teacher-class-subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(disallowedPayload))
                .andExpect(status().isUnprocessableEntity());

        // 3. Atualizar vínculo existente para disciplina não habilitada (Educação Física) também deve retornar 422
        String disallowedUpdatePayload = objectMapper.writeValueAsString(Map.of(
                "teacherId", teacher.getId(),
                "classRoomId", classRoom.getId(),
                "subjectId", peSubject.getId(),
                "minAssessmentsPerPeriod", 1,
                "maxAssessmentsPerPeriod", 4
        ));

        mockMvc.perform(put("/api/teacher-class-subjects/{id}", tcsId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(disallowedUpdatePayload))
                .andExpect(status().isUnprocessableEntity());

        // 4. Excluir disciplina associada ao cadastro do professor deve ser bloqueado com 422
        mockMvc.perform(delete("/api/subjects/{id}", mathSubject.getId()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("Tentativa de remover habilitação de disciplina lecionada ativamente em turma deve falhar com 422")
    void updateTeacher_removingActivelyAssignedSubject_isBlocked() throws Exception {
        // Vincula o professor à matéria de Matemática
        teacherClassSubjectRepository.save(TeacherClassSubject.builder()
                .teacher(teacher)
                .classRoom(classRoom)
                .subject(mathSubject)
                .minAssessmentsPerPeriod(1)
                .maxAssessmentsPerPeriod(3)
                .active(true)
                .build());

        // Tenta atualizar cadastro do professor removendo Matemática e mantendo apenas Artes
        TeacherUpdateRequestDTO updateDto = new TeacherUpdateRequestDTO(
                teacher.getName(),
                teacher.getEmail(),
                null,
                java.util.List.of(artsSubject.getId())
        );

        mockMvc.perform(put("/api/teachers/{id}", teacher.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(containsString("Não é possível remover a habilitação da disciplina")));
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("Remover habilitação de disciplina atribuída apenas a turma inativa deve ter sucesso")
    void updateTeacher_removingSubjectFromInactiveClassroom_succeeds() throws Exception {
        ClassRoom inactiveClassRoom = classRoomRepository.save(ClassRoom.builder()
                .name("Turma Inativa 999")
                .shift(Shift.MORNING)
                .periodType(AcademicPeriodType.TRIMESTER)
                .schoolGrade(SchoolGrade.MEDIO_2)
                .academicYear(classRoom.getAcademicYear())
                .active(false)
                .build());

        teacherClassSubjectRepository.save(TeacherClassSubject.builder()
                .teacher(teacher)
                .classRoom(inactiveClassRoom)
                .subject(mathSubject)
                .minAssessmentsPerPeriod(1)
                .maxAssessmentsPerPeriod(3)
                .active(true)
                .build());

        TeacherUpdateRequestDTO updateDto = new TeacherUpdateRequestDTO(
                teacher.getName(),
                teacher.getEmail(),
                null,
                java.util.List.of(artsSubject.getId())
        );

        mockMvc.perform(put("/api/teachers/{id}", teacher.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk());
    }
}
