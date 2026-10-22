package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.TimetableSlot;
import com.andretti101.escolaweb.model.enums.DayOfWeekEnum;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

// Repositório de slots de horário da grade semanal com detecção de colisões de tempo para turmas e professores
@Repository
public interface TimetableSlotRepository extends JpaRepository<TimetableSlot, Integer> {

    // Carrega o slot com associações de turma, atribuição docente e disciplina via EntityGraph
    @Override
    @EntityGraph(attributePaths = {"classRoom", "teacherClassSubject", "teacherClassSubject.teacher", "teacherClassSubject.subject"})
    Optional<TimetableSlot> findById(Integer id);

    @EntityGraph(attributePaths = {"classRoom", "teacherClassSubject", "teacherClassSubject.teacher", "teacherClassSubject.subject"})
    List<TimetableSlot> findByClassRoom_IdOrderByDayOfWeekAscSlotOrderAscStartTimeAsc(Integer classRoomId);

    @EntityGraph(attributePaths = {"classRoom", "teacherClassSubject", "teacherClassSubject.teacher", "teacherClassSubject.subject"})
    List<TimetableSlot> findByTeacherClassSubject_Teacher_IdOrderByDayOfWeekAscSlotOrderAscStartTimeAsc(Integer teacherId);

    @EntityGraph(attributePaths = {"classRoom", "teacherClassSubject", "teacherClassSubject.teacher", "teacherClassSubject.subject"})
    List<TimetableSlot> findByTeacherClassSubject_Id(Integer teacherClassSubjectId);

    @EntityGraph(attributePaths = {"classRoom", "teacherClassSubject", "teacherClassSubject.teacher", "teacherClassSubject.subject"})
    @Query("SELECT ts FROM TimetableSlot ts WHERE ts.teacherClassSubject.teacher.id = :teacherId " +
           "AND (:academicYearId IS NULL OR ts.classRoom.academicYear.id = :academicYearId) " +
           "ORDER BY ts.dayOfWeek ASC, ts.slotOrder ASC, ts.startTime ASC")
    List<TimetableSlot> findByTeacherAndAcademicYear(
            @Param("teacherId") Integer teacherId,
            @Param("academicYearId") Integer academicYearId
    );

    @EntityGraph(attributePaths = {"classRoom", "teacherClassSubject", "teacherClassSubject.teacher", "teacherClassSubject.subject"})
    @Query("SELECT ts FROM TimetableSlot ts WHERE ts.classRoom.id = :classRoomId AND ts.dayOfWeek = :dayOfWeek " +
           "AND ts.startTime < :endTime AND ts.endTime > :startTime " +
           "AND (:excludeSlotId IS NULL OR ts.id <> :excludeSlotId)")
    List<TimetableSlot> findClassRoomCollisions(
            @Param("classRoomId") Integer classRoomId,
            @Param("dayOfWeek") DayOfWeekEnum dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeSlotId") Integer excludeSlotId
    );

    @EntityGraph(attributePaths = {"classRoom", "teacherClassSubject", "teacherClassSubject.teacher", "teacherClassSubject.subject"})
    @Query("SELECT ts FROM TimetableSlot ts WHERE ts.teacherClassSubject.teacher.id = :teacherId " +
           "AND (:academicYearId IS NULL OR ts.classRoom.academicYear.id = :academicYearId) " +
           "AND ts.dayOfWeek = :dayOfWeek " +
           "AND ts.startTime < :endTime AND ts.endTime > :startTime " +
           "AND (:excludeSlotId IS NULL OR ts.id <> :excludeSlotId)")
    List<TimetableSlot> findTeacherCollisions(
            @Param("teacherId") Integer teacherId,
            @Param("academicYearId") Integer academicYearId,
            @Param("dayOfWeek") DayOfWeekEnum dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeSlotId") Integer excludeSlotId
    );

    boolean existsByClassRoom_Id(Integer classRoomId);

    boolean existsByTeacherClassSubject_Id(Integer teacherClassSubjectId);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @Query("DELETE FROM TimetableSlot ts WHERE ts.classRoom.id IN :classRoomIds")
    void deleteByClassRoom_IdIn(@Param("classRoomIds") java.util.Collection<Integer> classRoomIds);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @Query("DELETE FROM TimetableSlot ts WHERE ts.classRoom.id = :classRoomId")
    void deleteByClassRoom_Id(@Param("classRoomId") Integer classRoomId);

    List<TimetableSlot> findByClassRoom_IdIn(java.util.Collection<Integer> classRoomIds);

    @EntityGraph(attributePaths = {"classRoom", "teacherClassSubject", "teacherClassSubject.teacher", "teacherClassSubject.subject"})
    @Query("SELECT ts FROM TimetableSlot ts WHERE ts.classRoom.academicYear.id = :academicYearId " +
           "AND ts.classRoom.id NOT IN :excludeClassroomIds")
    List<TimetableSlot> findByAcademicYearIdAndClassRoomIdNotIn(
            @Param("academicYearId") Integer academicYearId,
            @Param("excludeClassroomIds") java.util.Collection<Integer> excludeClassroomIds
    );
}
