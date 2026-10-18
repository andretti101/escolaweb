package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.dto.response.ChatMessageResponseDTO;
import com.andretti101.escolaweb.model.entity.*;
import com.andretti101.escolaweb.model.enums.AcademicPeriodType;
import com.andretti101.escolaweb.model.enums.UserRole;
import com.andretti101.escolaweb.repository.*;
import com.andretti101.escolaweb.service.impl.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcademicCalendarAndCommunicationServiceTest {

    @Nested
    @DisplayName("AcademicYearServiceImpl & AcademicPeriodServiceImpl & SchoolSettingsServiceImpl Unit Tests")
    class CalendarAndSettingsTests {

        @Mock
        private AcademicYearRepository academicYearRepository;
        @Mock
        private ClassRoomRepository classRoomRepository;
        @Mock
        private AcademicPeriodRepository academicPeriodRepository;
        @Mock
        private AssessmentRepository assessmentRepository;
        @Mock
        private TeacherClassSubjectRepository teacherClassSubjectRepository;
        @Mock
        private AcademicYearService academicYearService;
        @Mock
        private SchoolSettingsRepository schoolSettingsRepository;

        @InjectMocks
        private AcademicYearServiceImpl academicYearServiceImpl;

        @InjectMocks
        private AcademicPeriodServiceImpl academicPeriodServiceImpl;

        @InjectMocks
        private SchoolSettingsServiceImpl schoolSettingsServiceImpl;

        @Test
        @DisplayName("AcademicYearServiceImpl setActive desativa o ano ativo atual e ativa o novo")
        void setActive_deactivatesCurrentAndActivatesTarget() {
            AcademicYear currentActive = AcademicYear.builder().id(1).year(2025).active(true).build();
            AcademicYear target = AcademicYear.builder().id(2).year(2026).active(false).build();

            when(academicYearRepository.findById(2)).thenReturn(Optional.of(target));
            when(academicYearRepository.findByActiveTrue()).thenReturn(Optional.of(currentActive));

            AcademicYear result = academicYearServiceImpl.setActive(2);

            assertFalse(currentActive.isActive());
            assertTrue(result.isActive());
        }

        @Test
        @DisplayName("AcademicPeriodServiceImpl createDivision cria a quantidade correta de períodos para a divisão")
        void createDivision_createsExpectedNumberOfPeriods() {
            AcademicYear year = AcademicYear.builder().id(1).year(2026).build();
            when(academicYearService.findById(1)).thenReturn(year);
            when(academicPeriodRepository.existsByAcademicYearAndPeriodType(year, AcademicPeriodType.TRIMESTER)).thenReturn(false);
            when(academicPeriodRepository.save(any(AcademicPeriod.class))).thenAnswer(inv -> inv.getArgument(0));

            List<AcademicPeriod> created = academicPeriodServiceImpl.createDivision(1, AcademicPeriodType.TRIMESTER);

            assertEquals(3, created.size());
            assertEquals("1º Trimestre", created.get(0).getName());
            assertEquals("2º Trimestre", created.get(1).getName());
            assertEquals("3º Trimestre", created.get(2).getName());
        }

        @Test
        @DisplayName("AcademicPeriodServiceImpl close valida data de término atingida e mínimo de avaliações")
        void closePeriod_validatesEndDateAndMinAssessments() {
            AcademicYear year = AcademicYear.builder().id(1).year(2026).build();
            AcademicPeriod futureEnd = AcademicPeriod.builder()
                    .id(10)
                    .name("1º Bimestre")
                    .academicYear(year)
                    .periodType(AcademicPeriodType.BIMESTER)
                    .endDate(LocalDate.now().plusDays(10))
                    .closed(false)
                    .build();

            when(academicPeriodRepository.findById(10)).thenReturn(Optional.of(futureEnd));
            assertThrows(IllegalStateException.class, () -> academicPeriodServiceImpl.close(10));

            // Data atingida, mas abaixo do mínimo de avaliações
            futureEnd.setEndDate(LocalDate.now().minusDays(1));
            ClassRoom cr = ClassRoom.builder().id(5).name("1º A").academicYear(year).periodType(AcademicPeriodType.BIMESTER).active(true).build();
            Subject subj = Subject.builder().id(2).name("Português").active(true).build();
            TeacherClassSubject tcs = TeacherClassSubject.builder()
                    .id(50)
                    .classRoom(cr)
                    .subject(subj)
                    .minAssessmentsPerPeriod(2)
                    .active(true)
                    .build();

            when(teacherClassSubjectRepository.findAll()).thenReturn(List.of(tcs));
            when(assessmentRepository.countByTeacherClassSubjectAndPeriod(tcs, futureEnd)).thenReturn(1L);

            assertThrows(IllegalStateException.class, () -> academicPeriodServiceImpl.close(10));

            // Com mínimo atingido -> fecha com sucesso
            when(assessmentRepository.countByTeacherClassSubjectAndPeriod(tcs, futureEnd)).thenReturn(2L);
            when(academicPeriodRepository.save(futureEnd)).thenReturn(futureEnd);

            AcademicPeriod closed = academicPeriodServiceImpl.close(10);
            assertTrue(closed.isClosed());
        }

        @Test
        @DisplayName("SchoolSettingsServiceImpl cria padrões quando banco está vazio e valida limites de média e frequência")
        void schoolSettings_createsDefaultsAndValidatesRanges() {
            when(schoolSettingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
            when(schoolSettingsRepository.save(any(SchoolSettings.class))).thenAnswer(inv -> inv.getArgument(0));

            SchoolSettings defaults = schoolSettingsServiceImpl.findSettings();
            assertEquals("Escola", defaults.getSchoolName());
            assertEquals(new BigDecimal("6.0"), defaults.getMinimumGrade());
            assertEquals(new BigDecimal("75.00"), defaults.getMinimumAttendance());

            when(schoolSettingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(defaults));

            assertThrows(IllegalArgumentException.class, () ->
                    schoolSettingsServiceImpl.update(SchoolSettings.builder().minimumGrade(new BigDecimal("11.0")).build()));
            assertThrows(IllegalArgumentException.class, () ->
                    schoolSettingsServiceImpl.update(SchoolSettings.builder().minimumAttendance(new BigDecimal("105.0")).build()));
        }

        @Test
        @DisplayName("AcademicPeriodServiceImpl update valida sobreposição cronológica e openNextPeriod fecha atual e abre próximo")
        void updateAndOpenNextPeriod_validatesChronologyAndTransitions() {
            AcademicYear year = AcademicYear.builder().id(1).year(2026).build();
            AcademicPeriod p1 = AcademicPeriod.builder()
                    .id(101)
                    .name("1º Semestre")
                    .academicYear(year)
                    .periodType(AcademicPeriodType.SEMESTER)
                    .startDate(LocalDate.now().minusMonths(4))
                    .endDate(LocalDate.now().minusDays(2))
                    .closed(false)
                    .build();
            AcademicPeriod p2 = AcademicPeriod.builder()
                    .id(102)
                    .name("2º Semestre")
                    .academicYear(year)
                    .periodType(AcademicPeriodType.SEMESTER)
                    .startDate(LocalDate.now().minusDays(5)) // sobreposto com fim do p1!
                    .endDate(LocalDate.now().plusMonths(3))
                    .closed(false)
                    .build();

            when(academicPeriodRepository.findById(102)).thenReturn(Optional.of(p2));
            when(academicPeriodRepository.findByAcademicYearAndPeriodTypeOrderByIdAsc(year, AcademicPeriodType.SEMESTER))
                    .thenReturn(List.of(p1, p2));

            // 1. Update do p2 com startDate anterior ao endDate do p1 -> IllegalStateException
            AcademicPeriod invalidOverlap = AcademicPeriod.builder()
                    .name("2º Semestre")
                    .startDate(LocalDate.now().minusDays(5))
                    .endDate(LocalDate.now().plusMonths(3))
                    .build();
            assertThrows(IllegalStateException.class, () -> academicPeriodServiceImpl.update(102, invalidOverlap));

            // 2. Update com startDate >= endDate -> IllegalStateException
            AcademicPeriod invertedDates = AcademicPeriod.builder()
                    .name("2º Semestre")
                    .startDate(LocalDate.now().plusDays(10))
                    .endDate(LocalDate.now().plusDays(5))
                    .build();
            assertThrows(IllegalStateException.class, () -> academicPeriodServiceImpl.update(102, invertedDates));

            // 3. openNextPeriod com datas sobrepostas -> IllegalStateException
            when(academicYearService.findById(1)).thenReturn(year);
            when(teacherClassSubjectRepository.findAll()).thenReturn(List.of());
            assertThrows(IllegalStateException.class, () ->
                    academicPeriodServiceImpl.openNextPeriod(1, AcademicPeriodType.SEMESTER));

            // 4. Ajustando startDate do p2 para depois do endDate do p1 -> openNextPeriod fecha p1 e retorna p2
            p2.setStartDate(LocalDate.now().minusDays(1));
            AcademicPeriod next = academicPeriodServiceImpl.openNextPeriod(1, AcademicPeriodType.SEMESTER);
            assertTrue(p1.isClosed());
            assertEquals(102, next.getId());

            // 5. Quando o período aberto já é o último da divisão -> openNextPeriod lança IllegalStateException
            assertThrows(IllegalStateException.class, () ->
                    academicPeriodServiceImpl.openNextPeriod(1, AcademicPeriodType.SEMESTER));
        }
    }

    @Nested
    @DisplayName("AuditHistoryServiceImpl, AnnouncementServiceImpl & ChatMessageServiceImpl Unit Tests")
    class AuditAndCommunicationTests {

        @Mock
        private GradeHistoryRepository gradeHistoryRepository;
        @Mock
        private AttendanceHistoryRepository attendanceHistoryRepository;
        @Mock
        private GradeRepository gradeRepository;
        @Mock
        private AttendanceRepository attendanceRepository;
        @Mock
        private AuthenticatedUserService authenticatedUserService;

        @InjectMocks
        private AuditHistoryServiceImpl auditHistoryService;

        @Mock
        private AnnouncementRepository announcementRepository;
        @Mock
        private ClassRoomService classRoomService;
        @Mock
        private ClassRoomRepository classRoomRepository;
        @Mock
        private StudentService studentService;
        @Mock
        private EnrollmentRepository enrollmentRepository;
        @Mock
        private TeacherService teacherService;
        @Mock
        private TeacherClassSubjectRepository teacherClassSubjectRepository;
        @Mock
        private UserRepository userRepository;
        @Mock
        private AcademicYearRepository academicYearRepository;
        @Mock
        private NotificationPublisherService notificationPublisherService;

        @InjectMocks
        private AnnouncementServiceImpl announcementService;

        @Mock
        private ChatMessageRepository chatMessageRepository;
        @Mock
        private ChatReadReceiptRepository chatReadReceiptRepository;
        @Mock
        private ChatHiddenMessageRepository chatHiddenMessageRepository;

        @InjectMocks
        private ChatMessageServiceImpl chatMessageService;

        @Test
        @DisplayName("AuditHistoryServiceImpl força o filtro pelo próprio ID quando o usuário autenticado é TEACHER")
        void searchGradeHistory_whenTeacher_forcesOwnTeacherId() {
            Teacher loggedTeacher = Teacher.builder().id(42).name("Prof").role(UserRole.TEACHER).build();
            when(authenticatedUserService.isTeacher()).thenReturn(true);
            when(authenticatedUserService.getAuthenticatedTeacher()).thenReturn(loggedTeacher);
            when(gradeHistoryRepository.search(eq(42), any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(new PageImpl<>(List.of()));

            AuditHistoryService.HistoryFilter filterWithOtherTeacher =
                    new AuditHistoryService.HistoryFilter(99, null, null, null, null, null, null);
            Page<?> result = auditHistoryService.searchGradeHistory(filterWithOtherTeacher, 0, 10);

            assertTrue(result.isEmpty());
            verify(gradeHistoryRepository).search(eq(42), isNull(), isNull(), isNull(), isNull(), any(), any(), any());
        }

        @Test
        @DisplayName("AnnouncementServiceImpl.create impede que TEACHER envie recado para professores ou turmas que não leciona")
        void announcementCreate_enforcesTeacherRestrictions() {
            Teacher teacherUser = Teacher.builder().id(5).name("Mariana").role(UserRole.TEACHER).build();
            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(teacherUser);

            Announcement toTeachers = Announcement.builder().title("Aviso").message("Msg").targetTeachers(true).build();
            assertThrows(AccessDeniedException.class, () -> announcementService.create(toTeachers, List.of()));

            Announcement toClassRoom = Announcement.builder().title("Aviso").message("Msg").targetTeachers(false).build();
            ClassRoom allowedCr = ClassRoom.builder().id(1).name("1º A").build();
            TeacherClassSubject tcs = TeacherClassSubject.builder().id(10).teacher(teacherUser).classRoom(allowedCr).build();
            when(teacherService.findById(5)).thenReturn(teacherUser);
            when(teacherClassSubjectRepository.findByTeacher(teacherUser)).thenReturn(List.of(tcs));

            // Turma 2 não pertence ao professor -> AccessDeniedException
            assertThrows(AccessDeniedException.class, () -> announcementService.create(toClassRoom, List.of(2)));
        }

        @Test
        @DisplayName("AnnouncementServiceImpl.create dispara notificações em tempo real para turmas e professores")
        void announcementCreate_dispatchesNotificationsToClassroomsAndTeachers() {
            com.andretti101.escolaweb.model.entity.Principal principalUser =
                    com.andretti101.escolaweb.model.entity.Principal.builder().id(1).name("Helena").role(UserRole.PRINCIPAL).build();
            when(authenticatedUserService.getAuthenticatedUser()).thenReturn(principalUser);

            ClassRoom cr1 = ClassRoom.builder().id(1).name("1º A").build();
            when(classRoomRepository.findAllById(List.of(1))).thenReturn(List.of(cr1));
            when(announcementRepository.save(any(Announcement.class))).thenAnswer(inv -> inv.getArgument(0));

            Announcement announcement = Announcement.builder()
                    .title("Reunião Geral")
                    .message("Comunicado para todos")
                    .targetTeachers(true)
                    .build();

            announcementService.create(announcement, List.of(1));

            verify(notificationPublisherService).notifyTeachers(argThat(dto ->
                    "NEW_ANNOUNCEMENT".equals(dto.getType()) &&
                    dto.getTitle().contains("Reunião Geral") &&
                    "Comunicado para todos".equals(dto.getMessage())
            ));

            verify(notificationPublisherService).notifyClassroom(eq(1), argThat(dto ->
                    "NEW_ANNOUNCEMENT".equals(dto.getType()) &&
                    dto.getTitle().contains("Reunião Geral")
            ));
        }

        @Test
        @DisplayName("ChatMessageServiceImpl bloqueia aluno com chatBlocked, valida autoria/moderação em edit/delete e restringe chat global de professores")
        void chatMessageService_blocksChatBlockedStudentAndEnforcesAuthorAndAdminRules() {
            ClassRoom cr = ClassRoom.builder().id(1).name("1º A").build();
            Student blockedStudent = Student.builder()
                    .id(15)
                    .name("Aluno Bloqueado")
                    .role(UserRole.STUDENT)
                    .isChatBlocked(true)
                    .build();

            when(classRoomRepository.findById(1)).thenReturn(Optional.of(cr));
            when(userRepository.findById(15)).thenReturn(Optional.of(blockedStudent));

            assertThrows(IllegalArgumentException.class, () ->
                    chatMessageService.saveMessage(1, 15, "Tentativa de mensagem", null));

            // Aluno não pode enviar mensagem no chat global de professores
            assertThrows(AccessDeniedException.class, () ->
                    chatMessageService.saveGlobalTeacherMessage(15, "Oi professores", null));

            // Edição por outro usuário -> AccessDeniedException
            Student normalStudent = Student.builder().id(16).name("Aluno Normal").role(UserRole.STUDENT).isChatBlocked(false).build();
            ChatMessage msg = ChatMessage.builder()
                    .id(100)
                    .classroom(cr)
                    .sender(normalStudent)
                    .content("Olá")
                    .isDeleted(false)
                    .build();
            when(chatMessageRepository.findById(100)).thenReturn(Optional.of(msg));

            assertThrows(AccessDeniedException.class, () ->
                    chatMessageService.editMessage(100, 99, "Hackeado"));

            // Edição pelo próprio autor -> sucesso
            when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(inv -> inv.getArgument(0));
            ChatMessageResponseDTO edited = chatMessageService.editMessage(100, 16, "Olá editado");
            assertEquals("Olá editado", edited.getContent());
            assertTrue(edited.isEdited());

            // Exclusão por outro aluno (não admin) -> AccessDeniedException
            assertThrows(AccessDeniedException.class, () ->
                    chatMessageService.deleteMessage(100, 99, false));

            // Exclusão por admin (não autor) -> deleted=true e deletedByAdmin=true
            ChatMessageResponseDTO deletedByAdmin = chatMessageService.deleteMessage(100, 1, true);
            assertTrue(deletedByAdmin.isDeleted());
            assertTrue(deletedByAdmin.isDeletedByAdmin());
            assertEquals("", deletedByAdmin.getContent());
        }
    }
}
