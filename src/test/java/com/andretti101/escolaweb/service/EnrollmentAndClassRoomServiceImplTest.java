package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.model.entity.*;
import com.andretti101.escolaweb.model.enums.AcademicPeriodType;
import com.andretti101.escolaweb.model.enums.SchoolGrade;
import com.andretti101.escolaweb.model.enums.Shift;
import com.andretti101.escolaweb.repository.*;
import com.andretti101.escolaweb.service.impl.ClassRoomServiceImpl;
import com.andretti101.escolaweb.service.impl.EnrollmentServiceImpl;
import com.andretti101.escolaweb.service.impl.SubjectServiceImpl;
import com.andretti101.escolaweb.service.impl.TeacherClassSubjectServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentAndClassRoomServiceImplTest {

    @Nested
    @DisplayName("EnrollmentServiceImpl Unit Tests")
    class EnrollmentServiceTests {

        @Mock
        private EnrollmentRepository enrollmentRepository;
        @Mock
        private GradeRepository gradeRepository;
        @Mock
        private AttendanceRepository attendanceRepository;
        @Mock
        private StudentService studentService;
        @Mock
        private ClassRoomService classRoomService;

        @InjectMocks
        private EnrollmentServiceImpl enrollmentService;

        private AcademicYear year2026;
        private Student student;
        private ClassRoom classRoomA;
        private ClassRoom classRoomB;

        @BeforeEach
        void setUp() {
            year2026 = AcademicYear.builder().id(1).year(2026).active(true).build();
            student = Student.builder().id(11).name("Lucas").active(true).build();
            classRoomA = ClassRoom.builder().id(10).name("1º A").academicYear(year2026).active(true).build();
            classRoomB = ClassRoom.builder().id(11).name("1º B").academicYear(year2026).active(true).build();
        }

        @Test
        @DisplayName("create matricula aluno ativo em turma ativa sem matrícula duplicada no ano")
        void create_validEnrollment_savesAsActive() {
            Enrollment toCreate = Enrollment.builder()
                    .student(student)
                    .classRoom(classRoomA)
                    .enrollmentDate(LocalDate.of(2026, 2, 1))
                    .build();

            when(studentService.findById(11)).thenReturn(student);
            when(classRoomService.findById(10)).thenReturn(classRoomA);
            when(enrollmentRepository.existsByStudentAndClassRoom_AcademicYearAndActiveTrue(student, year2026)).thenReturn(false);
            when(enrollmentRepository.existsByStudentAndClassRoom(student, classRoomA)).thenReturn(false);
            when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));

            Enrollment saved = enrollmentService.create(toCreate);
            assertTrue(saved.isActive());
            verify(enrollmentRepository).save(toCreate);
        }

        @Test
        @DisplayName("create rejeita aluno inativo, turma inativa ou matrícula ativa existente no mesmo ano letivo")
        void create_invalidStates_throwsIllegalState() {
            Enrollment toCreate = Enrollment.builder().student(student).classRoom(classRoomA).build();
            when(studentService.findById(11)).thenReturn(student);
            when(classRoomService.findById(10)).thenReturn(classRoomA);

            student.setActive(false);
            assertThrows(IllegalStateException.class, () -> enrollmentService.create(toCreate));

            student.setActive(true);
            classRoomA.setActive(false);
            assertThrows(IllegalStateException.class, () -> enrollmentService.create(toCreate));

            classRoomA.setActive(true);
            when(enrollmentRepository.existsByStudentAndClassRoom_AcademicYearAndActiveTrue(student, year2026)).thenReturn(true);
            assertThrows(IllegalStateException.class, () -> enrollmentService.create(toCreate));
        }

        @Test
        @DisplayName("transferClassroom valida turma ativa, turma diferente e mesmo ano letivo")
        void transferClassroom_validatesTargetClassRoom() {
            Enrollment existing = Enrollment.builder().id(50).student(student).classRoom(classRoomA).active(true).build();
            when(enrollmentRepository.findById(50)).thenReturn(Optional.of(existing));

            // Mesma turma -> erro
            when(classRoomService.findById(10)).thenReturn(classRoomA);
            assertThrows(IllegalStateException.class, () -> enrollmentService.transferClassroom(50, 10));

            // Turma inativa -> erro
            classRoomB.setActive(false);
            when(classRoomService.findById(11)).thenReturn(classRoomB);
            assertThrows(IllegalStateException.class, () -> enrollmentService.transferClassroom(50, 11));

            // Outro ano letivo -> erro
            classRoomB.setActive(true);
            classRoomB.setAcademicYear(AcademicYear.builder().id(2).year(2025).build());
            assertThrows(IllegalStateException.class, () -> enrollmentService.transferClassroom(50, 11));

            // Transferência válida
            classRoomB.setAcademicYear(year2026);
            Enrollment transferred = enrollmentService.transferClassroom(50, 11);
            assertEquals(classRoomB, transferred.getClassRoom());
        }
    }

    @Nested
    @DisplayName("ClassRoomServiceImpl, SubjectServiceImpl & TeacherClassSubjectServiceImpl Unit Tests")
    class ClassRoomSubjectAndTcsTests {

        @Mock
        private ClassRoomRepository classRoomRepository;
        @Mock
        private AcademicPeriodRepository academicPeriodRepository;
        @Mock
        private EnrollmentRepository enrollmentRepository;
        @Mock
        private TeacherClassSubjectRepository teacherClassSubjectRepository;
        @Mock
        private AcademicYearService academicYearService;
        @Mock
        private AcademicYearRepository academicYearRepository;
        @Mock
        private SubjectRepository subjectRepository;
        @Mock
        private TeacherRepository teacherRepository;
        @Mock
        private LessonRepository lessonRepository;
        @Mock
        private AssessmentRepository assessmentRepository;
        @Mock
        private TeacherService teacherService;
        @Mock
        private ClassRoomService classRoomService;
        @Mock
        private SubjectService subjectService;

        @InjectMocks
        private ClassRoomServiceImpl classRoomServiceImpl;

        @InjectMocks
        private SubjectServiceImpl subjectServiceImpl;

        @InjectMocks
        private TeacherClassSubjectServiceImpl tcsServiceImpl;

        @Test
        @DisplayName("ClassRoomServiceImpl.create exige divisão criada no ano letivo e nome único")
        void classRoomCreate_validatesDivisionAndUniqueName() {
            AcademicYear year = AcademicYear.builder().id(1).year(2026).build();
            ClassRoom cr = ClassRoom.builder()
                    .name("1º A")
                    .shift(Shift.MORNING)
                    .periodType(AcademicPeriodType.TRIMESTER)
                    .schoolGrade(SchoolGrade.MEDIO_1)
                    .academicYear(year)
                    .build();

            when(academicYearService.findById(1)).thenReturn(year);
            when(academicPeriodRepository.existsByAcademicYearAndPeriodType(year, AcademicPeriodType.TRIMESTER)).thenReturn(false);

            assertThrows(IllegalStateException.class, () -> classRoomServiceImpl.create(cr));

            when(academicPeriodRepository.existsByAcademicYearAndPeriodType(year, AcademicPeriodType.TRIMESTER)).thenReturn(true);
            when(classRoomRepository.existsByAcademicYearAndName(year, "1º A")).thenReturn(true);
            assertThrows(IllegalStateException.class, () -> classRoomServiceImpl.create(cr));

            when(classRoomRepository.existsByAcademicYearAndName(year, "1º A")).thenReturn(false);
            when(classRoomRepository.save(cr)).thenReturn(cr);
            ClassRoom saved = classRoomServiceImpl.create(cr);
            assertTrue(saved.isActive());
        }

        @Test
        @DisplayName("SubjectServiceImpl.create e delete validam unicidade do nome e vínculos existentes")
        void subjectCreateAndDelete_validatesConstraints() {
            Subject existing = Subject.builder().id(3).name("História").active(true).build();
            when(subjectRepository.findByNameIgnoreCase("História")).thenReturn(Optional.of(existing));

            assertThrows(IllegalStateException.class, () ->
                    subjectServiceImpl.create(Subject.builder().name("História").build()));

            when(subjectRepository.findById(3)).thenReturn(Optional.of(existing));
            when(teacherClassSubjectRepository.existsBySubject(existing)).thenReturn(true);
            assertThrows(IllegalStateException.class, () -> subjectServiceImpl.delete(3));
        }

        @Test
        @DisplayName("TeacherClassSubjectServiceImpl.create valida habilitação do professor e limites min/max de avaliações")
        void tcsCreate_validatesTeacherQualificationAndAssessmentLimits() {
            Subject math = Subject.builder().id(1).name("Matemática").active(true).build();
            Subject history = Subject.builder().id(2).name("História").active(true).build();
            Teacher teacher = Teacher.builder().id(5).name("Carlos").active(true).subjects(Set.of(math)).build();
            ClassRoom cr = ClassRoom.builder().id(10).name("1º A").active(true).build();

            when(teacherService.findById(5)).thenReturn(teacher);
            when(classRoomService.findById(10)).thenReturn(cr);
            when(subjectService.findById(2)).thenReturn(history);

            TeacherClassSubject unqualified = TeacherClassSubject.builder()
                    .teacher(teacher)
                    .classRoom(cr)
                    .subject(history)
                    .minAssessmentsPerPeriod(1)
                    .maxAssessmentsPerPeriod(3)
                    .build();

            assertThrows(IllegalStateException.class, () -> tcsServiceImpl.create(unqualified));

            when(subjectService.findById(1)).thenReturn(math);
            when(teacherClassSubjectRepository.existsByClassRoomAndSubject(cr, math)).thenReturn(false);

            TeacherClassSubject invalidLimits = TeacherClassSubject.builder()
                    .teacher(teacher)
                    .classRoom(cr)
                    .subject(math)
                    .minAssessmentsPerPeriod(5)
                    .maxAssessmentsPerPeriod(2)
                    .build();

            assertThrows(IllegalArgumentException.class, () -> tcsServiceImpl.create(invalidLimits));
        }
    }
}
