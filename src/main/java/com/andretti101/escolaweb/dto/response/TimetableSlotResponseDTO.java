package com.andretti101.escolaweb.dto.response;

import com.andretti101.escolaweb.model.entity.TimetableSlot;
import com.andretti101.escolaweb.model.enums.DayOfWeekEnum;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalTime;

public record TimetableSlotResponseDTO(
        Integer id,
        Integer classRoomId,
        String classRoomName,
        Integer teacherClassSubjectId,
        Integer teacherId,
        String teacherName,
        Integer subjectId,
        String subjectName,
        DayOfWeekEnum dayOfWeek,
        String dayOfWeekLabel,
        @JsonFormat(pattern = "HH:mm")
        LocalTime startTime,
        @JsonFormat(pattern = "HH:mm")
        LocalTime endTime,
        Integer slotOrder
) {
    public static TimetableSlotResponseDTO fromEntity(TimetableSlot slot) {
        if (slot == null) return null;
        return new TimetableSlotResponseDTO(
                slot.getId(),
                slot.getClassRoom() != null ? slot.getClassRoom().getId() : null,
                slot.getClassRoom() != null ? slot.getClassRoom().getName() : null,
                slot.getTeacherClassSubject() != null ? slot.getTeacherClassSubject().getId() : null,
                (slot.getTeacherClassSubject() != null && slot.getTeacherClassSubject().getTeacher() != null)
                        ? slot.getTeacherClassSubject().getTeacher().getId() : null,
                (slot.getTeacherClassSubject() != null && slot.getTeacherClassSubject().getTeacher() != null)
                        ? slot.getTeacherClassSubject().getTeacher().getName() : null,
                (slot.getTeacherClassSubject() != null && slot.getTeacherClassSubject().getSubject() != null)
                        ? slot.getTeacherClassSubject().getSubject().getId() : null,
                (slot.getTeacherClassSubject() != null && slot.getTeacherClassSubject().getSubject() != null)
                        ? slot.getTeacherClassSubject().getSubject().getName() : null,
                slot.getDayOfWeek(),
                slot.getDayOfWeek() != null ? slot.getDayOfWeek().getLabel() : null,
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getSlotOrder()
        );
    }
}
