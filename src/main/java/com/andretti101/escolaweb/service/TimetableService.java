package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.dto.request.TimetableSlotRequestDTO;
import com.andretti101.escolaweb.dto.response.TimetableSlotResponseDTO;
import com.andretti101.escolaweb.dto.response.WeeklyScheduleDTO;

public interface TimetableService {

    TimetableSlotResponseDTO addSlot(TimetableSlotRequestDTO dto);

    TimetableSlotResponseDTO updateSlot(Integer id, TimetableSlotRequestDTO dto);

    void removeSlot(Integer slotId);

    WeeklyScheduleDTO getScheduleByClassroom(Integer classroomId);

    WeeklyScheduleDTO getScheduleForAuthenticatedStudent();

    WeeklyScheduleDTO getScheduleForAuthenticatedTeacher();

    WeeklyScheduleDTO getScheduleForAuthenticatedTeacher(Integer academicYearId);

    WeeklyScheduleDTO getScheduleByTeacher(Integer teacherId);

    WeeklyScheduleDTO getScheduleByTeacher(Integer teacherId, Integer academicYearId);

    com.andretti101.escolaweb.dto.response.TimetableGenerationResponseDTO generateTimetable(
            com.andretti101.escolaweb.dto.request.TimetableGenerationRequestDTO dto
    );
}
