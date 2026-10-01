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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
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
        
        com.andretti101.escolaweb.model.entity.SchoolSettings settings = schoolSettingsService.findSettings();
        java.math.BigDecimal minimumGrade = year.getMinimumGrade() != null ? year.getMinimumGrade() : settings.getMinimumGrade();
        java.math.BigDecimal minimumAttendance = year.getMinimumAttendance() != null ? year.getMinimumAttendance() : settings.getMinimumAttendance();
        com.andretti101.escolaweb.model.enums.AcademicPeriodType periodType = year.getPeriodType() != null ? year.getPeriodType() : settings.getPeriodType();
        
        List<ReportCardDTO> results = new java.util.ArrayList<>();
        
        for (com.andretti101.escolaweb.model.entity.Enrollment enrollment : enrollments) {
            com.andretti101.escolaweb.model.entity.Student student = enrollment.getStudent();
            com.andretti101.escolaweb.model.entity.ClassRoom classRoom = enrollment.getClassRoom();
            
            List<com.andretti101.escolaweb.model.entity.TeacherClassSubject> tcsList = teacherClassSubjectRepository.findByClassRoom(classRoom);
            List<com.andretti101.escolaweb.model.entity.AcademicPeriod> periods = academicPeriodRepository.findByAcademicYear(year);
            
            int totalPeriods = getPeriodsCount(periodType);
            java.math.BigDecimal targetScore = minimumGrade.multiply(java.math.BigDecimal.valueOf(totalPeriods));
            
            List<com.andretti101.escolaweb.model.entity.Grade> studentGrades = gradesByStudent.getOrDefault(student.getId(), new java.util.ArrayList<>());
            List<com.andretti101.escolaweb.model.entity.Attendance> studentAttendances = attendancesByStudent.getOrDefault(student.getId(), new java.util.ArrayList<>());
            
            List<ReportCardDTO.SubjectSituationDTO> subjectSituations = new java.util.ArrayList<>();
            for (com.andretti101.escolaweb.model.entity.TeacherClassSubject tcs : tcsList) {
                List<com.andretti101.escolaweb.model.entity.Grade> tcsGrades = studentGrades.stream()
                        .filter(g -> g.getAssessment().getTeacherClassSubject().getId().equals(tcs.getId()))
                        .collect(Collectors.toList());
                        
                List<ReportCardDTO.PeriodGradeDTO> periodGrades = new java.util.ArrayList<>();
                java.math.BigDecimal closedPeriodSum = java.math.BigDecimal.ZERO;
                int closedPeriodsWithGrades = 0;
                
                for (com.andretti101.escolaweb.model.entity.AcademicPeriod period : periods) {
                    List<com.andretti101.escolaweb.model.entity.Grade> gradesInPeriod = tcsGrades.stream()
                            .filter(g -> g.getAssessment().getPeriod().getId().equals(period.getId()))
                            .collect(Collectors.toList());
                    
                    java.math.BigDecimal periodAverage = null;
                    if (!gradesInPeriod.isEmpty()) {
                        java.math.BigDecimal sum = gradesInPeriod.stream().filter(g -> g.getValue() != null).map(com.andretti101.escolaweb.model.entity.Grade::getValue)
                                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                        periodAverage = sum.divide(java.math.BigDecimal.valueOf(gradesInPeriod.size()), 1, java.math.RoundingMode.HALF_UP);
                    }
                    if (period.isClosed() && periodAverage != null) {
                        closedPeriodSum = closedPeriodSum.add(periodAverage);
                        closedPeriodsWithGrades++;
                    }
                    periodGrades.add(new ReportCardDTO.PeriodGradeDTO(
                            period.getId(), period.getName(), period.isClosed(), periodAverage));
                }
                
                int remainingPeriods = totalPeriods - closedPeriodsWithGrades;
                java.math.BigDecimal maxPossibleScore = closedPeriodSum.add(java.math.BigDecimal.TEN.multiply(java.math.BigDecimal.valueOf(remainingPeriods)));
                
                com.andretti101.escolaweb.model.enums.StudentSituation situation;
                if (closedPeriodsWithGrades == 0) {
                    situation = com.andretti101.escolaweb.model.enums.StudentSituation.PENDING;
                } else if (closedPeriodSum.compareTo(targetScore) >= 0) {
                    situation = com.andretti101.escolaweb.model.enums.StudentSituation.APPROVED;
                } else if (maxPossibleScore.compareTo(targetScore) < 0) {
                    situation = com.andretti101.escolaweb.model.enums.StudentSituation.FAILED;
                } else {
                    situation = com.andretti101.escolaweb.model.enums.StudentSituation.PENDING;
                }
                
                subjectSituations.add(new ReportCardDTO.SubjectSituationDTO(
                        tcs.getId(), tcs.getSubject().getName(), periodGrades, closedPeriodSum, targetScore, maxPossibleScore, situation));
            }
            
            ReportCardDTO.FrequencySituationDTO frequency;
            if (tcsList.isEmpty()) {
                frequency = new ReportCardDTO.FrequencySituationDTO(java.math.BigDecimal.valueOf(100), minimumAttendance, com.andretti101.escolaweb.model.enums.StudentSituation.APPROVED);
            } else {
                java.math.BigDecimal totalFrequency = java.math.BigDecimal.ZERO;
                int count = 0;
                for (com.andretti101.escolaweb.model.entity.TeacherClassSubject tcs : tcsList) {
                    List<com.andretti101.escolaweb.model.entity.Attendance> tcsAtt = studentAttendances.stream()
                            .filter(a -> a.getLesson().getTeacherClassSubject().getId().equals(tcs.getId()))
                            .collect(Collectors.toList());
                            
                    int totalClasses = tcsAtt.stream().mapToInt(a -> a.getLesson().getLessonCount() != null ? a.getLesson().getLessonCount().getValue() : 1).sum();
                    int absentClasses = tcsAtt.stream()
                            .filter(a -> a.getStatus() == com.andretti101.escolaweb.model.enums.AttendanceStatus.ABSENT)
                            .mapToInt(a -> a.getLesson().getLessonCount() != null ? a.getLesson().getLessonCount().getValue() : 1)
                            .sum();
                            
                    java.math.BigDecimal freq = java.math.BigDecimal.valueOf(100);
                    if (totalClasses > 0) {
                        int presentClasses = totalClasses - absentClasses;
                        freq = java.math.BigDecimal.valueOf(presentClasses).multiply(java.math.BigDecimal.valueOf(100))
                                .divide(java.math.BigDecimal.valueOf(totalClasses), 2, java.math.RoundingMode.HALF_UP);
                    }
                    totalFrequency = totalFrequency.add(freq);
                    count++;
                }
                java.math.BigDecimal generalFrequency = count > 0 ? totalFrequency.divide(java.math.BigDecimal.valueOf(count), 2, java.math.RoundingMode.HALF_UP) : java.math.BigDecimal.valueOf(100);
                com.andretti101.escolaweb.model.enums.StudentSituation sit = generalFrequency.compareTo(minimumAttendance) >= 0 ? com.andretti101.escolaweb.model.enums.StudentSituation.APPROVED : com.andretti101.escolaweb.model.enums.StudentSituation.FAILED;
                frequency = new ReportCardDTO.FrequencySituationDTO(generalFrequency, minimumAttendance, sit);
            }
            
            com.andretti101.escolaweb.model.enums.StudentSituation generalSituation = calculateGeneralSituation(subjectSituations, frequency);
            
            results.add(new ReportCardDTO(
                    student.getId(), student.getName(), student.getRegistrationNumber(),
                    year.getId(), year.getYear(), classRoom.getName(),
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
        
        java.math.BigDecimal minimumGrade = year.getMinimumGrade() != null ? year.getMinimumGrade() : settings.getMinimumGrade();
        java.math.BigDecimal minimumAttendance = year.getMinimumAttendance() != null ? year.getMinimumAttendance() : settings.getMinimumAttendance();
        com.andretti101.escolaweb.model.enums.AcademicPeriodType periodType = year.getPeriodType() != null ? year.getPeriodType() : settings.getPeriodType();

        Enrollment enrollment = enrollmentRepository.findByStudent(student).stream()
                .filter(e -> e.getClassRoom().getAcademicYear().getId().equals(academicYearId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "O aluno '" + student.getName() + "' No possui matrícula No ano letivo " + year.getYear() + "."));

        ClassRoom classRoom = enrollment.getClassRoom();

        List<AcademicPeriod> allPeriods = academicPeriodRepository.findByAcademicYearOrderByIdAsc(year);
        int totalPeriods = allPeriods.size();
        if (totalPeriods == 0) {
            totalPeriods = getPeriodsCount(periodType);
        }

        BigDecimal targetScore = minimumGrade.multiply(BigDecimal.valueOf(totalPeriods));

        List<TeacherClassSubject> tcsList = teacherClassSubjectRepository.findByClassRoom(classRoom);

        List<ReportCardDTO.SubjectSituationDTO> subjects = new ArrayList<>();
        for (TeacherClassSubject tcs : tcsList) {
            ReportCardDTO.SubjectSituationDTO subjectSituation = calculateSubjectSituation(
                    student, tcs, allPeriods, totalPeriods, targetScore);
            subjects.add(subjectSituation);
        }

        ReportCardDTO.FrequencySituationDTO frequencySituation = calculateFrequencySituation(
                student, tcsList, minimumAttendance);

        StudentSituation generalSituation = calculateGeneralSituation(subjects, frequencySituation);

        return new ReportCardDTO(
                student.getId(),
                student.getName(),
                student.getRegistrationNumber(),
                year.getId(),
                year.getYear(),
                classRoom.getName(),
                minimumGrade,
                totalPeriods,
                targetScore,
                subjects,
                frequencySituation,
                generalSituation
        );
    }

    private ReportCardDTO.SubjectSituationDTO calculateSubjectSituation(
            Student student,
            TeacherClassSubject tcs,
            List<AcademicPeriod> allPeriods,
            int totalPeriods,
            BigDecimal targetScore) {

        List<Grade> allGrades = gradeRepository.findByStudentAndAssessment_TeacherClassSubject(student, tcs);

        Map<Integer, List<Grade>> gradesByPeriodId = allGrades.stream()
                .filter(g -> g.getValue() != null && g.getAssessment() != null && g.getAssessment().getPeriod() != null)
                .collect(Collectors.groupingBy(g -> g.getAssessment().getPeriod().getId()));

        List<ReportCardDTO.PeriodGradeDTO> periodGrades = new ArrayList<>();
        BigDecimal closedPeriodSum = BigDecimal.ZERO;
        int closedPeriodsWithGrades = 0;

        for (AcademicPeriod period : allPeriods) {
            List<Grade> periodGradesList = gradesByPeriodId.getOrDefault(period.getId(), List.of());
                        BigDecimal periodAverage = null;

            if (!periodGradesList.isEmpty()) {
                BigDecimal sum = periodGradesList.stream()
                        .map(Grade::getValue)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                periodAverage = sum.divide(BigDecimal.valueOf(periodGradesList.size()), 2, RoundingMode.HALF_UP);
                
                if (period.isClosed()) {
                    closedPeriodSum = closedPeriodSum.add(periodAverage);
                    closedPeriodsWithGrades++;
                }
            }

            periodGrades.add(new ReportCardDTO.PeriodGradeDTO(
                    period.getId(),
                    period.getName(),
                    period.isClosed(),
                    periodAverage
            ));
        }

        int remainingPeriods = totalPeriods - closedPeriodsWithGrades;
        BigDecimal maxPossibleScore = closedPeriodSum.add(
                BigDecimal.TEN.multiply(BigDecimal.valueOf(remainingPeriods)));

        StudentSituation situation;
        if (closedPeriodsWithGrades == 0) {
            situation = StudentSituation.PENDING;
        } else if (closedPeriodSum.compareTo(targetScore) >= 0) {
            situation = StudentSituation.APPROVED;
        } else if (maxPossibleScore.compareTo(targetScore) < 0) {
            situation = StudentSituation.FAILED;
        } else {
            situation = StudentSituation.PENDING;
        }

        return new ReportCardDTO.SubjectSituationDTO(
                tcs.getId(),
                tcs.getSubject().getName(),
                periodGrades,
                closedPeriodSum,
                targetScore,
                maxPossibleScore,
                situation
        );
    }

    private ReportCardDTO.FrequencySituationDTO calculateFrequencySituation(
            Student student,
            List<TeacherClassSubject> tcsList,
            BigDecimal minimumAttendance) {

        if (tcsList.isEmpty()) {
            return new ReportCardDTO.FrequencySituationDTO(
                    BigDecimal.valueOf(100), minimumAttendance, StudentSituation.APPROVED);
        }

        BigDecimal totalFrequency = BigDecimal.ZERO;
        int count = 0;
        for (TeacherClassSubject tcs : tcsList) {
            BigDecimal freq = attendanceService.calculateFrequency(student.getId(), tcs.getId());
            totalFrequency = totalFrequency.add(freq);
            count++;
        }

        BigDecimal generalFrequency = count > 0
                ? totalFrequency.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(100);

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

    // HELPERS

    private int getPeriodsCount(AcademicPeriodType type) {
        if (type == null) return 3;
        return switch (type) {
            case BIMESTER -> 4;
            case TRIMESTER -> 3;
            case QUADRIMESTER -> 3;
            case SEMESTER -> 2;
            case ANNUAL -> 1;
        };
    }
}

