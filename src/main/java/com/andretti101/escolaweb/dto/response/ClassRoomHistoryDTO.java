package com.andretti101.escolaweb.dto.response;

import java.util.List;

public record ClassRoomHistoryDTO(
        Integer id,
        String name,
        String shift,
        String schoolGrade,
        String academicYear,
        List<HistorySubjectDTO> subjects,
        List<HistoryStudentDTO> students
) {
    public record HistorySubjectDTO(
            Integer id,
            String subjectName,
            String teacherName
    ) {}

    public record HistoryStudentDTO(
            Integer id,
            String studentName,
            String registrationNumber,
            String generalSituation
    ) {}
}
