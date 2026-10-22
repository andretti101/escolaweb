package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.ClassRoom;
import com.andretti101.escolaweb.model.entity.Subject;
import com.andretti101.escolaweb.model.entity.Teacher;
import com.andretti101.escolaweb.model.entity.TeacherClassSubject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// Repositório de atribuição docente: vínculo entre professor, turma e disciplina com consultas otimizadas para ano ativo
@Repository
public interface TeacherClassSubjectRepository extends JpaRepository<TeacherClassSubject, Integer> {

    // Carrega a atribuição com professor, turma, ano letivo e disciplina em consulta única
    @Override
    @EntityGraph(attributePaths = {"teacher", "classRoom", "classRoom.academicYear", "subject"})
    Optional<TeacherClassSubject> findById(Integer id);

    @Override
    @EntityGraph(attributePaths = {"teacher", "classRoom", "classRoom.academicYear", "subject"})
    List<TeacherClassSubject> findAll();

    @Override
    @EntityGraph(attributePaths = {"teacher", "classRoom", "classRoom.academicYear", "subject"})
    Page<TeacherClassSubject> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"teacher", "classRoom", "classRoom.academicYear", "subject"})
    List<TeacherClassSubject> findByTeacher(Teacher teacher);

    @EntityGraph(attributePaths = {"teacher", "classRoom", "classRoom.academicYear", "subject"})
    Page<TeacherClassSubject> findByTeacher(Teacher teacher, Pageable pageable);

    @EntityGraph(attributePaths = {"teacher", "classRoom", "classRoom.academicYear", "subject"})
    @Query("SELECT t FROM TeacherClassSubject t WHERE t.teacher.id = :teacherId AND t.classRoom.academicYear.active = true")
    Page<TeacherClassSubject> findByTeacherActiveAcademicYear(@Param("teacherId") Integer teacherId, Pageable pageable);

    @EntityGraph(attributePaths = {"teacher", "classRoom", "classRoom.academicYear", "subject"})
    List<TeacherClassSubject> findByTeacherIdAndActiveTrue(Integer teacherId);

    @EntityGraph(attributePaths = {"teacher", "classRoom", "classRoom.academicYear", "subject"})
    List<TeacherClassSubject> findByClassRoom(ClassRoom classRoom);

    @EntityGraph(attributePaths = {"teacher", "classRoom", "classRoom.academicYear", "subject"})
    List<TeacherClassSubject> findByClassRoomAndActiveTrue(ClassRoom classRoom);

    @EntityGraph(attributePaths = {"teacher", "classRoom", "classRoom.academicYear", "subject"})
    List<TeacherClassSubject> findByClassRoom_IdInAndActiveTrue(java.util.Collection<Integer> classRoomIds);

    @EntityGraph(attributePaths = {"teacher", "classRoom", "classRoom.academicYear", "subject"})
    List<TeacherClassSubject> findBySubject(Subject subject);

    boolean existsByClassRoomAndSubject(ClassRoom classRoom, Subject subject);
    boolean existsByTeacher(Teacher teacher);
    boolean existsByClassRoom(ClassRoom classRoom);
    boolean existsBySubject(Subject subject);
    boolean existsByTeacher_IdAndClassRoom_Id(Integer teacherId, Integer classRoomId);

    @org.springframework.data.jpa.repository.Query("SELECT tcs FROM TeacherClassSubject tcs WHERE tcs.active = true AND tcs.classRoom.active = true AND tcs.classRoom.academicYear.id = :yearId AND tcs.classRoom.periodType = :periodType")
    List<TeacherClassSubject> findActiveByYearAndPeriodType(@org.springframework.data.repository.query.Param("yearId") Integer yearId, @org.springframework.data.repository.query.Param("periodType") com.andretti101.escolaweb.model.enums.AcademicPeriodType periodType);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @org.springframework.data.jpa.repository.Query("UPDATE TeacherClassSubject t SET t.active = false WHERE t.classRoom.academicYear = :year AND t.active = true")
    int deactivateAllByAcademicYear(@org.springframework.data.repository.query.Param("year") com.andretti101.escolaweb.model.entity.AcademicYear academicYear);
}
