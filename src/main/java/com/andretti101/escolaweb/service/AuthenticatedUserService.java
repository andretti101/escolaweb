package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.model.entity.Student;
import com.andretti101.escolaweb.model.entity.Teacher;
import com.andretti101.escolaweb.model.entity.TeacherClassSubject;
import com.andretti101.escolaweb.model.entity.User;

public interface AuthenticatedUserService {

    /** Returns the full {@link User} entity for the logged‑in user. */
    User getAuthenticatedUser();
    Teacher getAuthenticatedTeacher();
    Student getAuthenticatedStudent();
    boolean isTeacher();
    boolean isStudent();
    void enforceTeacherOwnership(TeacherClassSubject tcs);
    void enforceStudentOwnership(Integer studentId);
}
