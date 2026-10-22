package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.AcademicPeriod;
import com.andretti101.escolaweb.model.entity.Assessment;
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

// Repositório de instrumentos de avaliação com contagem por período letivo e grafo de entidades associadas
@Repository
public interface AssessmentRepository extends JpaRepository<Assessment, Integer> {

    // Carrega a avaliação com período, atribuição, disciplina, professor e turma em um SELECT único
    @Override
    @EntityGraph(attributePaths = {"teacherClassSubject", "teacherClassSubject.subject", "teacherClassSubject.teacher", "teacherClassSubject.classRoom", "period"})
    Optional<Assessment> findById(Integer id);

    @Override
    @EntityGraph(attributePaths = {"teacherClassSubject", "teacherClassSubject.subject", "teacherClassSubject.teacher", "teacherClassSubject.classRoom", "period"})
    List<Assessment> findAll();

    @Override
    @EntityGraph(attributePaths = {"teacherClassSubject", "teacherClassSubject.subject", "teacherClassSubject.teacher", "teacherClassSubject.classRoom", "period"})
    Page<Assessment> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"teacherClassSubject", "teacherClassSubject.subject", "teacherClassSubject.teacher", "teacherClassSubject.classRoom", "period"})
    List<Assessment> findByTeacherClassSubject(TeacherClassSubject teacherClassSubject);

    @EntityGraph(attributePaths = {"teacherClassSubject", "teacherClassSubject.subject", "teacherClassSubject.teacher", "teacherClassSubject.classRoom", "period"})
    List<Assessment> findByPeriod(AcademicPeriod period);

    boolean existsByTeacherClassSubject(TeacherClassSubject teacherClassSubject);
    boolean existsByPeriod(AcademicPeriod period);
    long countByTeacherClassSubjectAndPeriod(TeacherClassSubject teacherClassSubject, AcademicPeriod period);

    @EntityGraph(attributePaths = {"teacherClassSubject", "teacherClassSubject.subject", "teacherClassSubject.teacher", "teacherClassSubject.classRoom", "period"})
    @Query("SELECT a FROM Assessment a WHERE a.teacherClassSubject.teacher.id = :teacherId AND a.teacherClassSubject.classRoom.academicYear.active = true")
    Page<Assessment> findByTeacherActiveAcademicYear(@Param("teacherId") Integer teacherId, Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @Query("UPDATE Assessment a SET a.active = false WHERE a.teacherClassSubject.classRoom.academicYear = :year AND a.active = true")
    int deactivateAllByAcademicYear(@Param("year") com.andretti101.escolaweb.model.entity.AcademicYear academicYear);
}