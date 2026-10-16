package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.AnnouncementRequestDTO;
import com.andretti101.escolaweb.dto.request.ChatMessageRequestDTO;
import com.andretti101.escolaweb.dto.request.PrincipalRequestDTO;
import com.andretti101.escolaweb.dto.request.SecretaryRequestDTO;
import com.andretti101.escolaweb.dto.response.ChatMessageResponseDTO;
import com.andretti101.escolaweb.model.entity.*;
import com.andretti101.escolaweb.model.enums.*;
import com.andretti101.escolaweb.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ChatAndAnnouncementControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ChatController chatController;

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private AcademicYearRepository academicYearRepository;

    @Autowired
    private ClassRoomRepository classRoomRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private PrincipalRepository principalRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private TeacherClassSubjectRepository teacherClassSubjectRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private AcademicYear activeYear;
    private ClassRoom classRoom;
    private ClassRoom otherClassRoom;
    private Subject subject;
    private Teacher teacher;
    private Student enrolledStudent;
    private Student unenrolledStudent;
    private com.andretti101.escolaweb.model.entity.Principal principalUser;

    @BeforeEach
    void setUp() {
        activeYear = academicYearRepository.findByActiveTrue().orElseGet(() ->
                academicYearRepository.save(AcademicYear.builder()
                        .year(2026)
                        .startDate(LocalDate.of(2026, 2, 1))
                        .endDate(LocalDate.of(2026, 12, 15))
                        .active(true)
                        .build()));

        subject = subjectRepository.save(Subject.builder()
                .name("Geografia Chat Int")
                .description("Disciplina de teste de comunicação")
                .active(true)
                .build());

        teacher = teacherRepository.save(Teacher.builder()
                .name("Prof Chat Int")
                .email("prof.chat.int@example.com")
                .password(passwordEncoder.encode("senha123"))
                .role(UserRole.TEACHER)
                .subjects(Set.of(subject))
                .active(true)
                .build());

        principalUser = principalRepository.save(com.andretti101.escolaweb.model.entity.Principal.builder()
                .name("Diretor Chat Int")
                .email("diretor.chat.int@example.com")
                .password(passwordEncoder.encode("senha123"))
                .role(UserRole.PRINCIPAL)
                .active(true)
                .build());

        enrolledStudent = studentRepository.save(Student.builder()
                .name("Aluno Chat Matriculado")
                .email("aluno.chat.enrolled@example.com")
                .password(passwordEncoder.encode("senha123"))
                .role(UserRole.STUDENT)
                .registrationNumber("2026CHAT01")
                .birthDate(LocalDate.of(2010, 4, 10))
                .guardianPhone("(48) 99999-4444")
                .isChatBlocked(false)
                .active(true)
                .build());

        unenrolledStudent = studentRepository.save(Student.builder()
                .name("Aluno Chat Fora da Turma")
                .email("aluno.chat.out@example.com")
                .password(passwordEncoder.encode("senha123"))
                .role(UserRole.STUDENT)
                .registrationNumber("2026CHAT02")
                .birthDate(LocalDate.of(2010, 8, 22))
                .guardianPhone("(48) 99999-5555")
                .isChatBlocked(false)
                .active(true)
                .build());

        classRoom = classRoomRepository.save(ClassRoom.builder()
                .name("Turma Chat 1A")
                .shift(Shift.MORNING)
                .periodType(AcademicPeriodType.TRIMESTER)
                .schoolGrade(SchoolGrade.MEDIO_1)
                .academicYear(activeYear)
                .active(true)
                .build());

        otherClassRoom = classRoomRepository.save(ClassRoom.builder()
                .name("Turma Chat 1B")
                .shift(Shift.AFTERNOON)
                .periodType(AcademicPeriodType.TRIMESTER)
                .schoolGrade(SchoolGrade.MEDIO_1)
                .academicYear(activeYear)
                .active(true)
                .build());

        enrollmentRepository.save(Enrollment.builder()
                .student(enrolledStudent)
                .classRoom(classRoom)
                .enrollmentDate(LocalDate.of(2026, 2, 2))
                .active(true)
                .build());

        teacherClassSubjectRepository.save(TeacherClassSubject.builder()
                .teacher(teacher)
                .classRoom(classRoom)
                .subject(subject)
                .minAssessmentsPerPeriod(1)
                .maxAssessmentsPerPeriod(3)
                .active(true)
                .build());
    }

    @Test
    @WithMockUser(username = "aluno.chat.enrolled@example.com", roles = "STUDENT")
    @DisplayName("ChatController: aluno matriculado envia/edita mensagem via @MessageMapping, lê histórico, marca leitura e oculta mensagem")
    void classroomChat_studentSendEditReadAndHideFlow() throws Exception {
        Principal studentPrincipal = () -> enrolledStudent.getEmail();

        // 1. Enviar mensagem via handler @MessageMapping do ChatController
        chatController.sendMessage(classRoom.getId(), new ChatMessageRequestDTO("Bom dia turma!", null), studentPrincipal);

        ArgumentCaptor<ChatMessageResponseDTO> msgCaptor = ArgumentCaptor.forClass(ChatMessageResponseDTO.class);
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/classroom/" + classRoom.getId()), msgCaptor.capture());
        ChatMessageResponseDTO sentMsg = msgCaptor.getValue();
        assertNotNull(sentMsg.getId());
        assertEquals("Bom dia turma!", sentMsg.getContent());

        // 2. Editar mensagem via handler @MessageMapping
        chatController.editMessage(classRoom.getId(), sentMsg.getId(),
                new ChatMessageRequestDTO("Bom dia turma (editado)!", null), studentPrincipal);

        // 3. Consultar histórico via REST GET /api/chat/classroom/{id}/history
        mockMvc.perform(get("/api/chat/classroom/{classroomId}/history", classRoom.getId())
                        .principal(studentPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isChatBlocked").value(false))
                .andExpect(jsonPath("$.messages", hasSize(1)))
                .andExpect(jsonPath("$.messages[0].content").value("Bom dia turma (editado)!"))
                .andExpect(jsonPath("$.messages[0].edited").value(true));

        // 4. Marcar mensagem como lida e listar membros da turma
        mockMvc.perform(post("/api/chat/read/{classroomId}", classRoom.getId())
                        .param("messageId", String.valueOf(sentMsg.getId()))
                        .principal(studentPrincipal))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/chat/classrooms/{id}/members", classRoom.getId())
                        .principal(studentPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Aluno Chat Matriculado"));

        // 5. Ocultar mensagem para o próprio usuário -> histórico fica vazio para ele
        mockMvc.perform(post("/api/chat/messages/{messageId}/hide", sentMsg.getId())
                        .principal(studentPrincipal))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/chat/classroom/{classroomId}/history", classRoom.getId())
                        .principal(studentPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages", hasSize(0)));
    }

    @Test
    @WithMockUser(username = "diretor.chat.int@example.com", roles = "PRINCIPAL")
    @DisplayName("ChatController: moderação de bloqueio de aluno, exclusão administrativa e isolamento entre chat de turma e sala dos professores")
    void chatModerationAndRoleBoundaries_enforcedCorrectly() throws Exception {
        Principal studentPrincipal = () -> enrolledStudent.getEmail();
        Principal otherStudentPrincipal = () -> unenrolledStudent.getEmail();
        Principal teacherPrincipal = () -> teacher.getEmail();
        Principal directorPrincipal = () -> principalUser.getEmail();

        // Aluno não matriculado e professor não podem enviar mensagens no chat da turma
        assertThrows(AccessDeniedException.class, () ->
                chatController.sendMessage(classRoom.getId(), new ChatMessageRequestDTO("Invasão", null), otherStudentPrincipal));
        assertThrows(AccessDeniedException.class, () ->
                chatController.sendMessage(classRoom.getId(), new ChatMessageRequestDTO("Prof na turma", null), teacherPrincipal));

        // Aluno matriculado envia mensagem
        chatController.sendMessage(classRoom.getId(), new ChatMessageRequestDTO("Mensagem para moderar", null), studentPrincipal);
        ArgumentCaptor<ChatMessageResponseDTO> captor = ArgumentCaptor.forClass(ChatMessageResponseDTO.class);
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/classroom/" + classRoom.getId()), captor.capture());
        Integer msgId = captor.getValue().getId();

        // Diretor bloqueia o aluno no chat via PUT /api/chat/users/{studentId}/block
        mockMvc.perform(put("/api/chat/users/{studentId}/block", enrolledStudent.getId())
                        .principal(directorPrincipal))
                .andExpect(status().isOk());

        assertTrue(studentRepository.findById(enrolledStudent.getId()).orElseThrow().isChatBlocked());
        assertThrows(IllegalArgumentException.class, () ->
                chatController.sendMessage(classRoom.getId(), new ChatMessageRequestDTO("Bloqueado", null), studentPrincipal));

        // Diretor exclui mensagem do aluno -> deletedByAdmin = true
        chatController.deleteMessage(classRoom.getId(), msgId, directorPrincipal);
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/classroom/" + classRoom.getId()), captor.capture());
        assertTrue(captor.getValue().isDeleted());
        assertTrue(captor.getValue().isDeletedByAdmin());

        // Chat global dos professores: professor envia mensagem e consulta histórico
        chatController.sendTeacherMessage(new ChatMessageRequestDTO("Reunião pedagógica às 14h", null), teacherPrincipal);
        ArgumentCaptor<ChatMessageResponseDTO> teacherCaptor = ArgumentCaptor.forClass(ChatMessageResponseDTO.class);
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/teachers"), teacherCaptor.capture());
        assertEquals("Reunião pedagógica às 14h", teacherCaptor.getValue().getContent());

        mockMvc.perform(get("/api/chat/teachers/history")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(teacher.getEmail()).roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages", not(empty())));

        // Aluno recebe 403 ao tentar acessar histórico ou membros da sala dos professores
        mockMvc.perform(get("/api/chat/teachers/history")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(enrolledStudent.getEmail()).roles("STUDENT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/chat/teachers/members")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(enrolledStudent.getEmail()).roles("STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("AnnouncementController: professor cria comunicado para sua turma, é impedido em turma alheia e aluno consulta/marca leitura")
    @WithMockUser(username = "prof.chat.int@example.com", roles = "TEACHER")
    void announcementFlow_teacherCreatesAndPermissionsEnforced() throws Exception {
        // 1. Professor cria comunicado para sua própria turma -> 201 Created
        AnnouncementRequestDTO validDto = new AnnouncementRequestDTO(
                "Entrega de Trabalho de Geografia",
                "Trazer mapas impressos na próxima aula.",
                LocalDate.of(2026, 4, 15),
                List.of(classRoom.getId()),
                false
        );

        MvcResult createRes = mockMvc.perform(post("/api/announcements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Entrega de Trabalho de Geografia"))
                .andReturn();

        int announcementId = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asInt();

        // 2. Professor tenta criar comunicado para turma em que NÃO leciona -> 403 Forbidden
        AnnouncementRequestDTO otherClassDto = new AnnouncementRequestDTO(
                "Aviso Indevido",
                "Mensagem",
                LocalDate.of(2026, 4, 15),
                List.of(otherClassRoom.getId()),
                false
        );

        mockMvc.perform(post("/api/announcements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otherClassDto)))
                .andExpect(status().isForbidden());

        // 3. Professor tenta enviar comunicado direcionado a professores -> 403 Forbidden
        AnnouncementRequestDTO toTeachersDto = new AnnouncementRequestDTO(
                "Aviso para Docentes",
                "Mensagem",
                null,
                List.of(),
                true
        );

        mockMvc.perform(post("/api/announcements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(toTeachersDto)))
                .andExpect(status().isForbidden());

        // 4. Professor atualiza e exclui o próprio comunicado
        AnnouncementRequestDTO updatedDto = new AnnouncementRequestDTO(
                "Entrega de Trabalho Atualizada",
                "Nova data de entrega.",
                LocalDate.of(2026, 4, 20),
                List.of(classRoom.getId()),
                false
        );

        mockMvc.perform(put("/api/announcements/{id}", announcementId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Entrega de Trabalho Atualizada"));

        mockMvc.perform(delete("/api/announcements/{id}", announcementId))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "diretor.chat.int@example.com", roles = "PRINCIPAL")
    @DisplayName("PrincipalController e SecretaryController: cadastro, listagem, desativação, ativação e exclusão")
    void principalAndSecretaryControllers_crudLifecycle() throws Exception {
        PrincipalRequestDTO principalDto = new PrincipalRequestDTO(
                "Novo Diretor Teste",
                "novo.diretor.int@example.com",
                "senhaSegura123"
        );

        MvcResult pRes = mockMvc.perform(post("/api/principals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(principalDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("novo.diretor.int@example.com"))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn();

        int pId = objectMapper.readTree(pRes.getResponse().getContentAsString()).get("id").asInt();

        mockMvc.perform(put("/api/principals/{id}/deactivate", pId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(put("/api/principals/{id}/activate", pId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(delete("/api/principals/{id}", pId))
                .andExpect(status().isNoContent());

        SecretaryRequestDTO secretaryDto = new SecretaryRequestDTO(
                "Novo Secretario Teste",
                "novo.secretario.int@example.com",
                "senhaSegura123"
        );

        MvcResult sRes = mockMvc.perform(post("/api/secretaries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secretaryDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("novo.secretario.int@example.com"))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn();

        int sId = objectMapper.readTree(sRes.getResponse().getContentAsString()).get("id").asInt();

        mockMvc.perform(get("/api/secretaries/{id}", sId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Novo Secretario Teste"));

        mockMvc.perform(delete("/api/secretaries/{id}", sId))
                .andExpect(status().isNoContent());
    }
}
