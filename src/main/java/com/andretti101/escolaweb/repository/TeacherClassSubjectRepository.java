package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.ClassRoom;
import com.andretti101.escolaweb.model.entity.Subject;
import com.andretti101.escolaweb.model.entity.Teacher;
import com.andretti101.escolaweb.model.entity.TeacherClassSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeacherClassSubjectRepository extends JpaRepository<TeacherClassSubject, Integer> {
    List<TeacherClassSubject> findByTeacher(Teacher teacher);
    List<TeacherClassSubject> findByClassRoom(ClassRoom classRoom);
    List<TeacherClassSubject> findBySubject(Subject subject);
    boolean existsByTeacherAndClassRoomAndSubject(Teacher teacher, ClassRoom classRoom, Subject subject);
    boolean existsByTeacher(Teacher teacher);
    boolean existsByClassRoom(ClassRoom classRoom);
    boolean existsBySubject(Subject subject);
}
