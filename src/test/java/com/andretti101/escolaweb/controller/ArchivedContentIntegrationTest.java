package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.model.entity.*;
import com.andretti101.escolaweb.model.enums.SchoolGrade;
import com.andretti101.escolaweb.model.enums.Shift;
import com.andretti101.escolaweb.model.enums.UserRole;
import com.andretti101.escolaweb.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ArchivedContentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private ClassRoomRepository classRoomRepository;

    @Autowired
    private AcademicYearRepository academicYearRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private Teacher teacher;
    private ClassRoom classRoom;
    private AcademicYear academicYear;

    @BeforeEach
    void setUp() {
        teacher = Teacher.builder()
                .name("Prof Teste Arquivamento")
                .email("prof.arquivamento@escola.com")
                .password(passwordEncoder.encode("senha123"))
                .role(UserRole.TEACHER)
                .active(true)
                .build();
        teacher = teacherRepository.save(teacher);

        academicYear = academicYearRepository.findAll().stream().findFirst().orElseGet(() -> {
            AcademicYear ay = AcademicYear.builder()
                    .year(2025)
                    .startDate(LocalDate.of(2025, 2, 1))
                    .endDate(LocalDate.of(2025, 12, 20))
                    .active(true)
                    .build();
            return academicYearRepository.save(ay);
        });

        classRoom = ClassRoom.builder()
                .name("Turma Arquivada A")
                .shift(Shift.MORNING)
                .schoolGrade(SchoolGrade.FUNDAMENTAL_1)
                .academicYear(academicYear)
                .build();
        classRoom = classRoomRepository.save(classRoom);
    }

    // ==========================================
    // Testes de Recados Arquivados (/api/announcements/archived)
    // ==========================================

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("GET /api/announcements/archived by SECRETARY returns 200 and only archived announcements")
    void findArchivedAnnouncements_asSecretary_returns200AndOnlyArchived() throws Exception {
        Announcement archivedAnnouncement = Announcement.builder()
                .title("Aviso Histórico Arquivado")
                .message("Conteúdo do aviso arquivado")
                .author(teacher)
                .classRooms(Set.of(classRoom))
                .targetTeachers(false)
                .archived(true)
                .build();
        announcementRepository.save(archivedAnnouncement);

        Announcement activeAnnouncement = Announcement.builder()
                .title("Aviso Ativo Atual")
                .message("Conteúdo do aviso ativo")
                .author(teacher)
                .classRooms(Set.of(classRoom))
                .targetTeachers(false)
                .archived(false)
                .build();
        announcementRepository.save(activeAnnouncement);

        mockMvc.perform(get("/api/announcements/archived"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", not(empty())))
                .andExpect(jsonPath("$.content[*].title", hasItem("Aviso Histórico Arquivado")))
                .andExpect(jsonPath("$.content[*].title", not(hasItem("Aviso Ativo Atual"))));
    }

    @Test
    @WithMockUser(roles = "PRINCIPAL")
    @DisplayName("GET /api/announcements/archived by PRINCIPAL returns 200 OK")
    void findArchivedAnnouncements_asPrincipal_returns200() throws Exception {
        mockMvc.perform(get("/api/announcements/archived"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("GET /api/announcements/archived by TEACHER returns 403 Forbidden")
    void findArchivedAnnouncements_asTeacher_returns403() throws Exception {
        mockMvc.perform(get("/api/announcements/archived"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("GET /api/announcements/archived by STUDENT returns 403 Forbidden")
    void findArchivedAnnouncements_asStudent_returns403() throws Exception {
        mockMvc.perform(get("/api/announcements/archived"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/announcements/archived unauthenticated returns 401 or 403")
    void findArchivedAnnouncements_unauthenticated_returnsUnauthorizedOrForbidden() throws Exception {
        mockMvc.perform(get("/api/announcements/archived"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("GET /api/announcements/archived with filters applies author and target correctly")
    void findArchivedAnnouncements_withFilters_filtersCorrectly() throws Exception {
        Announcement arch1 = Announcement.builder()
                .title("Recado do Professor")
                .message("Mensagem para turmas")
                .author(teacher)
                .classRooms(Set.of(classRoom))
                .targetTeachers(false)
                .archived(true)
                .build();
        announcementRepository.save(arch1);

        Announcement arch2 = Announcement.builder()
                .title("Recado Apenas Professores")
                .message("Mensagem docente")
                .author(teacher)
                .targetTeachers(true)
                .archived(true)
                .build();
        announcementRepository.save(arch2);

        // Filtrando por targetTeachers = true
        mockMvc.perform(get("/api/announcements/archived")
                        .param("targetTeachers", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].title", hasItem("Recado Apenas Professores")))
                .andExpect(jsonPath("$.content[*].title", not(hasItem("Recado do Professor"))));

        // Filtrando por classRoomId
        mockMvc.perform(get("/api/announcements/archived")
                        .param("classRoomId", classRoom.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].title", hasItem("Recado do Professor")))
                .andExpect(jsonPath("$.content[*].title", not(hasItem("Recado Apenas Professores"))));
    }

    // ==========================================
    // Testes de Chat de Turma Arquivado (/api/chat/classroom/{id}/archived-history)
    // ==========================================

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("GET /api/chat/classroom/{id}/archived-history by SECRETARY returns 200 and only archived messages")
    void getArchivedChatHistory_asSecretary_returns200AndOnlyArchived() throws Exception {
        ChatMessage archivedMsg = ChatMessage.builder()
                .classroom(classRoom)
                .sender(teacher)
                .content("Mensagem de chat arquivada")
                .archived(true)
                .build();
        chatMessageRepository.save(archivedMsg);

        ChatMessage activeMsg = ChatMessage.builder()
                .classroom(classRoom)
                .sender(teacher)
                .content("Mensagem de chat ativa")
                .archived(false)
                .build();
        chatMessageRepository.save(activeMsg);

        mockMvc.perform(get("/api/chat/classroom/{classroomId}/archived-history", classRoom.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].content").value("Mensagem de chat arquivada"))
                .andExpect(jsonPath("$[0].classroomId").value(classRoom.getId()));
    }

    @Test
    @WithMockUser(roles = "PRINCIPAL")
    @DisplayName("GET /api/chat/classroom/{id}/archived-history by PRINCIPAL returns 200 OK")
    void getArchivedChatHistory_asPrincipal_returns200() throws Exception {
        mockMvc.perform(get("/api/chat/classroom/{classroomId}/archived-history", classRoom.getId()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("GET /api/chat/classroom/{id}/archived-history by TEACHER returns 403 Forbidden")
    void getArchivedChatHistory_asTeacher_returns403() throws Exception {
        mockMvc.perform(get("/api/chat/classroom/{classroomId}/archived-history", classRoom.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("GET /api/chat/classroom/{id}/archived-history by STUDENT returns 403 Forbidden")
    void getArchivedChatHistory_asStudent_returns403() throws Exception {
        mockMvc.perform(get("/api/chat/classroom/{classroomId}/archived-history", classRoom.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("GET /api/chat/classroom/{id}/archived-history for non-existent classroom returns 404")
    void getArchivedChatHistory_nonExistentClassroom_returns404() throws Exception {
        mockMvc.perform(get("/api/chat/classroom/{classroomId}/archived-history", 999999))
                .andExpect(status().isNotFound());
    }

    // ==========================================
    // Testes de Chat dos Professores Arquivado (/api/chat/teachers/archived-history)
    // ==========================================

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("GET /api/chat/teachers/archived-history by SECRETARY returns 200 and only archived teacher messages")
    void getArchivedTeacherChatHistory_asSecretary_returns200AndOnlyArchived() throws Exception {
        ChatMessage archivedTeacherMsg = ChatMessage.builder()
                .classroom(null)
                .isGlobalTeacherChat(true)
                .sender(teacher)
                .content("Mensagem arquivada sala dos professores")
                .archived(true)
                .build();
        chatMessageRepository.save(archivedTeacherMsg);

        ChatMessage activeTeacherMsg = ChatMessage.builder()
                .classroom(null)
                .isGlobalTeacherChat(true)
                .sender(teacher)
                .content("Mensagem ativa sala dos professores")
                .archived(false)
                .build();
        chatMessageRepository.save(activeTeacherMsg);

        mockMvc.perform(get("/api/chat/teachers/archived-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].content", hasItem("Mensagem arquivada sala dos professores")))
                .andExpect(jsonPath("$[*].content", not(hasItem("Mensagem ativa sala dos professores"))));
    }

    @Test
    @WithMockUser(roles = "PRINCIPAL")
    @DisplayName("GET /api/chat/teachers/archived-history by PRINCIPAL returns 200 OK")
    void getArchivedTeacherChatHistory_asPrincipal_returns200() throws Exception {
        mockMvc.perform(get("/api/chat/teachers/archived-history"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("GET /api/chat/teachers/archived-history by TEACHER returns 403 Forbidden")
    void getArchivedTeacherChatHistory_asTeacher_returns403() throws Exception {
        mockMvc.perform(get("/api/chat/teachers/archived-history"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("GET /api/chat/teachers/archived-history by STUDENT returns 403 Forbidden")
    void getArchivedTeacherChatHistory_asStudent_returns403() throws Exception {
        mockMvc.perform(get("/api/chat/teachers/archived-history"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/chat/classroom/{id}/archived-history unauthenticated returns 401 Unauthorized")
    void getArchivedChatHistory_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/chat/classroom/{classroomId}/archived-history", classRoom.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/chat/teachers/archived-history unauthenticated returns 401 Unauthorized")
    void getArchivedTeacherChatHistory_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/chat/teachers/archived-history"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("GET /api/announcements/archived with multi-classroom announcement counts distinct correctly")
    void findArchivedAnnouncements_withMultipleClassrooms_countsDistinct() throws Exception {
        ClassRoom classRoom2 = ClassRoom.builder()
                .name("Turma Arquivada B")
                .shift(Shift.AFTERNOON)
                .schoolGrade(SchoolGrade.FUNDAMENTAL_2)
                .academicYear(academicYear)
                .build();
        classRoom2 = classRoomRepository.save(classRoom2);

        Announcement multiRoom = Announcement.builder()
                .title("Aviso em Multiplas Turmas")
                .message("Conteúdo multi turmas")
                .author(teacher)
                .classRooms(Set.of(classRoom, classRoom2))
                .targetTeachers(false)
                .archived(true)
                .build();
        announcementRepository.save(multiRoom);

        mockMvc.perform(get("/api/announcements/archived")
                        .param("authorId", teacher.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Aviso em Multiplas Turmas"))
                .andExpect(jsonPath("$.content[0].classRooms", hasSize(2)));
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("GET /api/announcements/archived with null targetTeachers is included when targetTeachers=false")
    void findArchivedAnnouncements_nullTargetTeachers_includedWhenFilteringFalse() throws Exception {
        Announcement nullTarget = Announcement.builder()
                .title("Aviso com targetTeachers Nulo")
                .message("Conteúdo legado")
                .author(teacher)
                .classRooms(Set.of(classRoom))
                .targetTeachers(null)
                .archived(true)
                .build();
        announcementRepository.save(nullTarget);

        mockMvc.perform(get("/api/announcements/archived")
                        .param("targetTeachers", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].title", hasItem("Aviso com targetTeachers Nulo")));
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("GET /api/announcements/archived with academicYearId filters classroom announcements correctly")
    void findArchivedAnnouncements_withAcademicYearId_filtersCorrectly() throws Exception {
        AcademicYear ay2024 = academicYearRepository.findByYear(2024).orElseGet(() ->
                academicYearRepository.save(AcademicYear.builder().year(2024).startDate(LocalDate.of(2024, 2, 1)).endDate(LocalDate.of(2024, 12, 20)).active(false).build()));
        AcademicYear ay2025 = academicYearRepository.findByYear(2025).orElseGet(() ->
                academicYearRepository.save(AcademicYear.builder().year(2025).startDate(LocalDate.of(2025, 2, 1)).endDate(LocalDate.of(2025, 12, 20)).active(false).build()));

        ClassRoom room2024 = classRoomRepository.save(ClassRoom.builder()
                .name("Turma 2024 Alpha")
                .shift(Shift.MORNING)
                .schoolGrade(SchoolGrade.FUNDAMENTAL_1)
                .academicYear(ay2024)
                .build());

        ClassRoom room2025 = classRoomRepository.save(ClassRoom.builder()
                .name("Turma 2025 Beta")
                .shift(Shift.MORNING)
                .schoolGrade(SchoolGrade.FUNDAMENTAL_1)
                .academicYear(ay2025)
                .build());

        Announcement ann2024 = Announcement.builder()
                .title("Aviso de 2024")
                .message("Conteudo 2024")
                .author(teacher)
                .classRooms(Set.of(room2024))
                .targetTeachers(false)
                .archived(true)
                .build();
        announcementRepository.save(ann2024);

        Announcement ann2025 = Announcement.builder()
                .title("Aviso de 2025")
                .message("Conteudo 2025")
                .author(teacher)
                .classRooms(Set.of(room2025))
                .targetTeachers(false)
                .archived(true)
                .build();
        announcementRepository.save(ann2025);

        // Filter by 2024 academicYearId
        mockMvc.perform(get("/api/announcements/archived")
                        .param("academicYearId", ay2024.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].title", hasItem("Aviso de 2024")))
                .andExpect(jsonPath("$.content[*].title", not(hasItem("Aviso de 2025"))));

        // Filter by 2025 academicYearId
        mockMvc.perform(get("/api/announcements/archived")
                        .param("academicYearId", ay2025.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].title", hasItem("Aviso de 2025")))
                .andExpect(jsonPath("$.content[*].title", not(hasItem("Aviso de 2024"))));
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("GET /api/chat/teachers/archived-history with year param filters messages by year")
    void getArchivedTeacherChatHistory_withYearFilter_returnsFilteredMessages() throws Exception {
        ChatMessage msg2024 = ChatMessage.builder()
                .classroom(null)
                .isGlobalTeacherChat(true)
                .sender(teacher)
                .content("Mensagem Professores 2024 Teste")
                .archived(true)
                .build();
        msg2024 = chatMessageRepository.saveAndFlush(msg2024);
        entityManager.createNativeQuery("UPDATE chat_messages SET timestamp = '2024-06-15 10:00:00' WHERE id = :id")
                .setParameter("id", msg2024.getId())
                .executeUpdate();

        ChatMessage msg2025 = ChatMessage.builder()
                .classroom(null)
                .isGlobalTeacherChat(true)
                .sender(teacher)
                .content("Mensagem Professores 2025 Teste")
                .archived(true)
                .build();
        msg2025 = chatMessageRepository.saveAndFlush(msg2025);
        entityManager.createNativeQuery("UPDATE chat_messages SET timestamp = '2025-06-15 10:00:00' WHERE id = :id")
                .setParameter("id", msg2025.getId())
                .executeUpdate();

        entityManager.flush();
        entityManager.clear();

        // Filter by year 2024
        mockMvc.perform(get("/api/chat/teachers/archived-history")
                        .param("year", "2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].content", hasItem("Mensagem Professores 2024 Teste")))
                .andExpect(jsonPath("$[*].content", not(hasItem("Mensagem Professores 2025 Teste"))));

        // Filter by year 2025
        mockMvc.perform(get("/api/chat/teachers/archived-history")
                        .param("year", "2025"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].content", hasItem("Mensagem Professores 2025 Teste")))
                .andExpect(jsonPath("$[*].content", not(hasItem("Mensagem Professores 2024 Teste"))));

        // Filter without year returns both
        mockMvc.perform(get("/api/chat/teachers/archived-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].content", hasItem("Mensagem Professores 2024 Teste")))
                .andExpect(jsonPath("$[*].content", hasItem("Mensagem Professores 2025 Teste")));
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("GET /api/classrooms with academicYearId filters classrooms by year")
    void findClassrooms_withAcademicYearId_returnsFilteredClassrooms() throws Exception {
        AcademicYear ay2024 = academicYearRepository.findByYear(2024).orElseGet(() ->
                academicYearRepository.save(AcademicYear.builder().year(2024).startDate(LocalDate.of(2024, 2, 1)).endDate(LocalDate.of(2024, 12, 20)).active(false).build()));
        AcademicYear ay2025 = academicYearRepository.findByYear(2025).orElseGet(() ->
                academicYearRepository.save(AcademicYear.builder().year(2025).startDate(LocalDate.of(2025, 2, 1)).endDate(LocalDate.of(2025, 12, 20)).active(false).build()));

        ClassRoom room2024 = classRoomRepository.save(ClassRoom.builder()
                .name("Turma Teste Ano 2024")
                .shift(Shift.MORNING)
                .schoolGrade(SchoolGrade.FUNDAMENTAL_1)
                .academicYear(ay2024)
                .build());

        ClassRoom room2025 = classRoomRepository.save(ClassRoom.builder()
                .name("Turma Teste Ano 2025")
                .shift(Shift.MORNING)
                .schoolGrade(SchoolGrade.FUNDAMENTAL_1)
                .academicYear(ay2025)
                .build());

        mockMvc.perform(get("/api/classrooms")
                        .param("academicYearId", ay2024.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("Turma Teste Ano 2024")))
                .andExpect(jsonPath("$[*].name", not(hasItem("Turma Teste Ano 2025"))));

        mockMvc.perform(get("/api/classrooms")
                        .param("academicYearId", ay2025.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("Turma Teste Ano 2025")))
                .andExpect(jsonPath("$[*].name", not(hasItem("Turma Teste Ano 2024"))));
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("GET /api/classrooms with 4-digit calendar year resolves and filters correctly")
    void findClassrooms_withCalendarYearNumber_resolvesAndFiltersCorrectly() throws Exception {
        AcademicYear ay2024 = academicYearRepository.findByYear(2024).orElseGet(() ->
                academicYearRepository.save(AcademicYear.builder().year(2024).startDate(LocalDate.of(2024, 2, 1)).endDate(LocalDate.of(2024, 12, 20)).active(false).build()));

        ClassRoom room2024 = classRoomRepository.save(ClassRoom.builder()
                .name("Turma Ano 2024 Cal")
                .shift(Shift.MORNING)
                .schoolGrade(SchoolGrade.FUNDAMENTAL_1)
                .academicYear(ay2024)
                .build());

        mockMvc.perform(get("/api/classrooms")
                        .param("academicYearId", "2024")
                        .param("activeOnly", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("Turma Ano 2024 Cal")));
    }

    @Test
    @WithMockUser(username = "secretaria@example.com", roles = "SECRETARY")
    @DisplayName("GET /api/chat/classrooms/{id}/members returns students from archived classrooms with inactive enrollments")
    void getChatMembers_inArchivedClassroom_returnsStudents() throws Exception {
        Student student = Student.builder()
                .name("Aluno Arquivado Teste")
                .email("aluno.arquivado@teste.com")
                .password(passwordEncoder.encode("senha123"))
                .role(UserRole.STUDENT)
                .active(true)
                .registrationNumber("2024999")
                .build();
        student = studentRepository.save(student);

        Enrollment inactiveEnrollment = Enrollment.builder()
                .student(student)
                .classRoom(classRoom)
                .enrollmentDate(LocalDate.of(2024, 2, 1))
                .active(false)
                .build();
        enrollmentRepository.save(inactiveEnrollment);

        mockMvc.perform(get("/api/chat/classrooms/{id}/members", classRoom.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("Aluno Arquivado Teste")));
    }
}
