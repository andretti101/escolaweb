package com.andretti101.escolaweb.dto.response;

public record UserStatsDTO(
        long totalUsers,
        long totalStaff,
        long totalTeachers,
        long totalStudents,
        long studentsApproved,
        long studentsPending,
        long studentsFailed
) {}
