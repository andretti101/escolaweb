package com.andretti101.escolaweb.service.impl;

import com.andretti101.escolaweb.dto.response.ReportCardDTO;
import com.andretti101.escolaweb.model.entity.*;
import com.andretti101.escolaweb.model.enums.AcademicPeriodType;
import com.andretti101.escolaweb.model.enums.StudentSituation;
import com.andretti101.escolaweb.repository.AcademicPeriodRepository;
import com.andretti101.escolaweb.repository.EnrollmentRepository;
import com.andretti101.escolaweb.repository.GradeRepository;
import com.andretti101.escolaweb.repository.TeacherClassSubjectRepository;
import com.andretti101.escolaweb.service.AcademicYearService;
import com.andretti101.escolaweb.service.AttendanceService;
import com.andretti101.escolaweb.service.ReportCardService;
import com.andretti101.escolaweb.service.SchoolSettingsService;
import com.andretti101.escolaweb.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportCardServiceImpl implements ReportCardService {

    private final StudentService studentService;
    private final AcademicYearService academicYearService;
    private final SchoolSettingsService schoolSettingsService;
    private final AttendanceService attendanceService;
    private final AcademicPeriodRepository academicPeriodRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TeacherClassSubjectRepository teacherClassSubjectRepository;
    private final GradeRepository gradeRepository;
    private final com.andretti101.escolaweb.repository.AttendanceRepository attendanceRepository;

    @Transactional(readOnly = true)
    @Override
    public List<ReportCardDTO> generateReportCardsBatch(List<com.andretti101.escolaweb.model.entity.Enrollment> enrollments, com.andretti101.escolaweb.model.entity.AcademicYear year) {
        if(enrollments.isEmpty()) return new java.util.ArrayList<>();
        
        List<com.andretti101.escolaweb.model.entity.Student> students = enrollments.stream().map(com.andretti101.escolaweb.model.entity.Enrollment::getStudent).collect(Collectors.toList());
        List<com.andretti101.escolaweb.model.entity.Grade> allGrades = gradeRepository.findByStudentsAndYear(students, year);
        List<com.andretti101.escolaweb.model.entity.Attendance> allAttendances = attendanceRepository.findByStudentsAndYear(students, year);
        
        Map<Integer, List<com.andretti101.escolaweb.model.entity.Grade>> gradesByStudent = allGrades.stream().collect(Collectors.groupingBy(g -> g.getStudent().getId()));
        Map<Integer, List<com.andretti101.escolaweb.model.entity.Attendance>> attendancesByStudent = allAttendances.stream().collect(Collectors.groupingBy(a -> a.getStudent().getId()));
        
        SchoolSettings settings = schoolSettingsService.findSettings();
        BigDecimal minimumGrade = year.getMinimumGrade() != null ? year.getMinimumGrade() : settings.getMinimumGrade();
        BigDecimal minimumAttendance = year.getMinimumAttendance() != null ? year.getMinimumAttendance() : settings.getMinimumAttendance();
        
        List<ReportCardDTO> results = new ArrayList<>();
        Map<Integer, List<TeacherClassSubject>> tcsByClassRoomId = new java.util.HashMap<>();
        Map<AcademicPeriodType, List<AcademicPeriod>> periodsByType = new java.util.HashMap<>();
        
        for (Enrollment enrollment : enrollments) {
            Student student = enrollment.getStudent();
            ClassRoom classRoom = enrollment.getClassRoom();
            
            List<TeacherClassSubject> tcsList = tcsByClassRoomId.computeIfAbsent(
                    classRoom.getId(), _id -> teacherClassSubjectRepository.findByClassRoom(classRoom));
            AcademicPeriodType periodType = classRoom.getPeriodType();
            List<AcademicPeriod> periods = periodsByType.computeIfAbsent(
                    periodType, pt -> academicPeriodRepository.findByAcademicYearAndPeriodTypeOrderByIdAsc(year, pt));
            
            int totalPeriods = !periods.isEmpty() ? periods.size() : (periodType != null ? periodType.getPeriodsCount() : 3);
            BigDecimal targetScore = minimumGrade.multiply(BigDecimal.valueOf(totalPeriods));
            
            List<com.andretti101.escolaweb.model.entity.Grade> studentGrades = gradesByStudent.getOrDefault(student.getId(), new java.util.ArrayList<>());
            List<com.andretti101.escolaweb.model.entity.Attendance> studentAttendances = attendancesByStudent.getOrDefault(student.getId(), new java.util.ArrayList<>());
            
            Map<Integer, List<Grade>> gradesByTcsId = studentGrades.stream()
                    .filter(g -> g.getAssessment() != null && g.getAssessment().getTeacherClassSubject() != null)
                    .collect(Collectors.groupingBy(g -> g.getAssessment().getTeacherClassSubject().getId()));
            
            List<ReportCardDTO.SubjectSituationDTO> subjectSituations = new java.util.ArrayList<>();
            for (com.andretti101.escolaweb.model.entity.TeacherClassSubject tcs : tcsList) {
                List<Grade> tcsGrades = gradesByTcsId.getOrDefault(tcs.getId(), List.of());
                subjectSituations.add(buildSubjectSituation(tcs, tcsGrades, periods, totalPeriods, targetScore));
            }
            
            ReportCardDTO.FrequencySituationDTO frequency;
            if (tcsList.isEmpty()) {
                frequency = new ReportCardDTO.FrequencySituationDTO(java.math.BigDecimal.valueOf(100), minimumAttendance, com.andretti101.escolaweb.model.enums.StudentSituation.APPROVED);
            } else {
                int overallTotalClasses = 0;
                int overallPresentClasses = 0;
                for (com.andretti101.escolaweb.model.entity.TeacherClassSubject tcs : tcsList) {
                    List<com.andretti101.escolaweb.model.entity.Attendance> tcsAtt = studentAttendances.stream()
                            .filter(a -> a.getLesson().getTeacherClassSubject().getId().equals(tcs.getId()))
                            .collect(Collectors.toList());
                            
                    int totalClasses = tcsAtt.stream().mapToInt(a -> a.getLesson().getLessonCount() != null ? a.getLesson().getLessonCount().getValue() : 1).sum();
                    int absentClasses = tcsAtt.stream()
                            .filter(a -> a.getStatus() == com.andretti101.escolaweb.model.enums.AttendanceStatus.ABSENT)
                            .mapToInt(a -> a.getLesson().getLessonCount() != null ? a.getLesson().getLessonCount().getValue() : 1)
                            .sum();
                            
                    overallTotalClasses += totalClasses;
                    overallPresentClasses += (totalClasses - absentClasses);
                }
                // Frequência mínima legal de 75% (LDB art. 24, VI) ponderada pelo total real de aulas ministradas
                java.math.BigDecimal generalFrequency = overallTotalClasses > 0
                        ? java.math.BigDecimal.valueOf(overallPresentClasses).multiply(java.math.BigDecimal.valueOf(100))
                                .divide(java.math.BigDecimal.valueOf(overallTotalClasses), 2, java.math.RoundingMode.HALF_UP)
                        : java.math.BigDecimal.valueOf(100);
                com.andretti101.escolaweb.model.enums.StudentSituation sit = generalFrequency.compareTo(minimumAttendance) >= 0 ? com.andretti101.escolaweb.model.enums.StudentSituation.APPROVED : com.andretti101.escolaweb.model.enums.StudentSituation.FAILED;
                frequency = new ReportCardDTO.FrequencySituationDTO(generalFrequency, minimumAttendance, sit);
            }
            
            com.andretti101.escolaweb.model.enums.StudentSituation generalSituation = calculateGeneralSituation(subjectSituations, frequency);
            
            results.add(new ReportCardDTO(
                    student.getId(), student.getName(), student.getRegistrationNumber(),
                    year.getId(), year.getYear(), classRoom.getName(),
                    periodType, periodType != null ? periodType.getLabel() : null,
                    minimumGrade, totalPeriods, targetScore,
                    subjectSituations, frequency, generalSituation
            ));
        }
        
        return results;
    }

    @Override
    public ReportCardDTO generateReportCard(Integer studentId, Integer academicYearId) {
        Student student = studentService.findById(studentId);
        AcademicYear year = academicYearService.findById(academicYearId);
        SchoolSettings settings = schoolSettingsService.findSettings();
        
        BigDecimal minimumGrade = year.getMinimumGrade() != null ? year.getMinimumGrade() : settings.getMinimumGrade();
        BigDecimal minimumAttendance = year.getMinimumAttendance() != null ? year.getMinimumAttendance() : settings.getMinimumAttendance();

        Enrollment enrollment = enrollmentRepository.findByStudent(student).stream()
                .filter(e -> e.getClassRoom().getAcademicYear().getId().equals(academicYearId))
                .sorted(Comparator.comparing(Enrollment::isActive).reversed())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "O aluno '" + student.getName() + "' não possui matrícula no ano letivo " + year.getYear() + "."));

        ClassRoom classRoom = enrollment.getClassRoom();
        AcademicPeriodType periodType = classRoom.getPeriodType();

        List<AcademicPeriod> allPeriods = academicPeriodRepository
                .findByAcademicYearAndPeriodTypeOrderByIdAsc(year, periodType);
        int totalPeriods = !allPeriods.isEmpty() ? allPeriods.size() : (periodType != null ? periodType.getPeriodsCount() : 3);

        BigDecimal targetScore = minimumGrade.multiply(BigDecimal.valueOf(totalPeriods));

        List<TeacherClassSubject> tcsList = teacherClassSubjectRepository.findByClassRoom(classRoom);

        List<Grade> bulkStudentGrades = gradeRepository.findByStudentsAndYear(List.of(student), year);
        Map<Integer, List<Grade>> gradesByTcsId = bulkStudentGrades != null
                ? bulkStudentGrades.stream()
                        .filter(g -> g.getAssessment() != null && g.getAssessment().getTeacherClassSubject() != null)
                        .collect(Collectors.groupingBy(g -> g.getAssessment().getTeacherClassSubject().getId()))
                : Map.of();

        List<ReportCardDTO.SubjectSituationDTO> subjects = new ArrayList<>();
        for (TeacherClassSubject tcs : tcsList) {
            List<Grade> tcsGrades = gradesByTcsId.get(tcs.getId());
            if (tcsGrades == null) {
                tcsGrades = (bulkStudentGrades == null || bulkStudentGrades.isEmpty())
                        ? gradeRepository.findByStudentAndAssessment_TeacherClassSubject(student, tcs)
                        : List.of();
            }
            ReportCardDTO.SubjectSituationDTO subjectSituation = buildSubjectSituation(
                    tcs, tcsGrades != null ? tcsGrades : List.of(), allPeriods, totalPeriods, targetScore);
            subjects.add(subjectSituation);
        }

        List<Attendance> bulkAttendances = attendanceRepository != null
                ? attendanceRepository.findByStudentsAndYear(List.of(student), year)
                : List.of();
        ReportCardDTO.FrequencySituationDTO frequencySituation = calculateFrequencySituation(
                student, tcsList, minimumAttendance, bulkAttendances);

        StudentSituation generalSituation = calculateGeneralSituation(subjects, frequencySituation);

        return new ReportCardDTO(
                student.getId(),
                student.getName(),
                student.getRegistrationNumber(),
                year.getId(),
                year.getYear(),
                classRoom.getName(),
                periodType,
                periodType != null ? periodType.getLabel() : null,
                minimumGrade,
                totalPeriods,
                targetScore,
                subjects,
                frequencySituation,
                generalSituation
        );
    }

    private ReportCardDTO.SubjectSituationDTO buildSubjectSituation(
            TeacherClassSubject tcs,
            List<Grade> allGrades,
            List<AcademicPeriod> allPeriods,
            int totalPeriods,
            BigDecimal targetScore) {

        Map<Integer, List<Grade>> gradesByPeriodId = allGrades.stream()
                .filter(g -> g.getEffectiveValue() != null && g.getAssessment() != null && g.getAssessment().getPeriod() != null)
                .collect(Collectors.groupingBy(g -> g.getAssessment().getPeriod().getId()));

        List<ReportCardDTO.PeriodGradeDTO> periodGrades = new ArrayList<>();
        BigDecimal closedPeriodSum = BigDecimal.ZERO;
        BigDecimal gradedPeriodSum = BigDecimal.ZERO;
        int gradedPeriodsCount = 0;

        for (AcademicPeriod period : allPeriods) {
            List<Grade> periodGradesList = gradesByPeriodId.getOrDefault(period.getId(), List.of());
            BigDecimal periodAverage = null;

            if (!periodGradesList.isEmpty()) {
                BigDecimal sum = periodGradesList.stream()
                        .map(Grade::getEffectiveValue)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                periodAverage = sum.divide(BigDecimal.valueOf(periodGradesList.size()), 2, RoundingMode.HALF_UP);
                gradedPeriodSum = gradedPeriodSum.add(periodAverage);
                gradedPeriodsCount++;

                if (period.isClosed()) {
                    closedPeriodSum = closedPeriodSum.add(periodAverage);
                }
            }

            periodGrades.add(new ReportCardDTO.PeriodGradeDTO(
                    period.getId(),
                    period.getName(),
                    period.isClosed(),
                    periodAverage
            ));
        }

        int remainingPeriods = !allPeriods.isEmpty() ? (int) allPeriods.stream().filter(p -> !p.isClosed()).count() : totalPeriods;
        BigDecimal maxPossibleScore = closedPeriodSum.add(
                BigDecimal.TEN.multiply(BigDecimal.valueOf(remainingPeriods)));

        StudentSituation situation;
        if (remainingPeriods == 0) {
            situation = closedPeriodSum.compareTo(targetScore) >= 0
                    ? StudentSituation.APPROVED
                    : StudentSituation.FAILED;
        } else if (closedPeriodSum.compareTo(targetScore) >= 0) {
            situation = StudentSituation.APPROVED;
        } else if (maxPossibleScore.compareTo(targetScore) < 0) {
            situation = StudentSituation.FAILED;
        } else {
            situation = StudentSituation.PENDING;
        }

        BigDecimal finalAverage = null;
        if (remainingPeriods == 0 && totalPeriods > 0) {
            finalAverage = closedPeriodSum.divide(BigDecimal.valueOf(totalPeriods), 2, RoundingMode.HALF_UP);
        } else if (gradedPeriodsCount > 0) {
            finalAverage = gradedPeriodSum.divide(BigDecimal.valueOf(gradedPeriodsCount), 2, RoundingMode.HALF_UP);
        }

        return new ReportCardDTO.SubjectSituationDTO(
                tcs.getId(),
                tcs.getSubject().getName(),
                periodGrades,
                closedPeriodSum,
                targetScore,
                maxPossibleScore,
                situation,
                finalAverage
        );
    }

    private ReportCardDTO.FrequencySituationDTO calculateFrequencySituation(
            Student student,
            List<TeacherClassSubject> tcsList,
            BigDecimal minimumAttendance,
            List<Attendance> preloadedAttendances) {

        if (tcsList.isEmpty()) {
            return new ReportCardDTO.FrequencySituationDTO(
                    BigDecimal.valueOf(100), minimumAttendance, StudentSituation.APPROVED);
        }

        Set<Integer> tcsIds = tcsList.stream().map(TeacherClassSubject::getId).collect(Collectors.toSet());
        List<Attendance> studentAttendances = (preloadedAttendances != null && !preloadedAttendances.isEmpty())
                ? preloadedAttendances
                : (attendanceRepository != null ? attendanceRepository.findByStudent(student) : List.of());

        List<Attendance> classAttendances = studentAttendances != null
                ? studentAttendances.stream()
                        .filter(a -> a.getLesson() != null
                                && a.getLesson().getTeacherClassSubject() != null
                                && tcsIds.contains(a.getLesson().getTeacherClassSubject().getId()))
                        .toList()
                : List.of();

        BigDecimal generalFrequency;
        if (!classAttendances.isEmpty()) {
            int totalClasses = classAttendances.stream()
                    .mapToInt(a -> a.getLesson().getLessonCount() != null ? a.getLesson().getLessonCount().getValue() : 1)
                    .sum();
            int absentClasses = classAttendances.stream()
                    .filter(a -> a.getStatus() == com.andretti101.escolaweb.model.enums.AttendanceStatus.ABSENT)
                    .mapToInt(a -> a.getLesson().getLessonCount() != null ? a.getLesson().getLessonCount().getValue() : 1)
                    .sum();
            int presentClasses = totalClasses - absentClasses;
            generalFrequency = totalClasses > 0
                    ? BigDecimal.valueOf(presentClasses).multiply(BigDecimal.valueOf(100))
                            .divide(BigDecimal.valueOf(totalClasses), 2, RoundingMode.HALF_UP)
                    : BigDecimal.valueOf(100);
        } else {
            BigDecimal totalFrequency = BigDecimal.ZERO;
            int count = 0;
            for (TeacherClassSubject tcs : tcsList) {
                BigDecimal freq = attendanceService.calculateFrequency(student.getId(), tcs.getId());
                if (freq != null) {
                    totalFrequency = totalFrequency.add(freq);
                    count++;
                }
            }
            generalFrequency = count > 0
                    ? totalFrequency.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
                    : BigDecimal.valueOf(100);
        }

        StudentSituation situation = generalFrequency.compareTo(minimumAttendance) >= 0
                ? StudentSituation.APPROVED
                : StudentSituation.FAILED;

        return new ReportCardDTO.FrequencySituationDTO(
                generalFrequency, minimumAttendance, situation);
    }


    private StudentSituation calculateGeneralSituation(
            List<ReportCardDTO.SubjectSituationDTO> subjects,
            ReportCardDTO.FrequencySituationDTO frequency) {

        List<StudentSituation> allSituations = new ArrayList<>();
        for (ReportCardDTO.SubjectSituationDTO subject : subjects) {
            allSituations.add(subject.situation());
        }
        allSituations.add(frequency.situation());

        if (allSituations.contains(StudentSituation.FAILED)) {
            return StudentSituation.FAILED;
        }
        if (allSituations.contains(StudentSituation.PENDING)) {
            return StudentSituation.PENDING;
        }
        return StudentSituation.APPROVED;
    }
}
