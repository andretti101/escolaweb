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
    boolean existsByClassRoomAndSubject(ClassRoom classRoom, Subject subject);
    boolean existsByTeacher(Teacher teacher);
    boolean existsByClassRoom(ClassRoom classRoom);
    boolean existsBySubject(Subject subject);
    boolean existsByTeacher_IdAndClassRoom_Id(Integer teacherId, Integer classRoomId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE TeacherClassSubject t SET t.active = false WHERE t.classRoom.academicYear = :year AND t.active = true")
    int deactivateAllByAcademicYear(@org.springframework.data.repository.query.Param("year") com.andretti101.escolaweb.model.entity.AcademicYear academicYear);
}
