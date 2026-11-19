package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.GradeHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GradeHistoryRepository extends JpaRepository<GradeHistory, Integer> {

    @Query(value = """
            SELECT h FROM GradeHistory h
            LEFT JOIN FETCH h.grade g
            LEFT JOIN FETCH g.student s
            LEFT JOIN FETCH g.assessment a
            LEFT JOIN FETCH a.period p
            LEFT JOIN FETCH a.teacherClassSubject tcs
            LEFT JOIN FETCH tcs.subject sub
            LEFT JOIN FETCH tcs.classRoom c
            LEFT JOIN FETCH c.academicYear ay
            LEFT JOIN FETCH tcs.teacher t
            LEFT JOIN FETCH h.changedBy u
            WHERE (
                  (h.previousGrade IS NOT NULL AND h.newGrade IS NOT NULL AND h.previousGrade <> h.newGrade)
               OR (h.previousGrade IS NOT NULL AND h.newGrade IS NULL)
               OR (h.grade IS NULL AND h.newGrade IS NULL)
               OR (h.gradeKind = com.andretti101.escolaweb.model.enums.GradeKind.RECOVERY
                   AND h.previousGrade IS NULL AND h.newGrade IS NOT NULL)
            )
              AND (:teacherId IS NULL OR t.id = :teacherId)
              AND (:classRoomId IS NULL OR c.id = :classRoomId)
              AND (:studentId IS NULL OR s.id = :studentId)
              AND (:yearId IS NULL OR ay.id = :yearId)
              AND (:periodId IS NULL OR p.id = :periodId)
              AND h.changedAt >= :from
              AND h.changedAt < :to
            """,
            countQuery = """
            SELECT COUNT(h) FROM GradeHistory h
            LEFT JOIN h.grade g
            LEFT JOIN g.student s
            LEFT JOIN g.assessment a
            LEFT JOIN a.period p
            LEFT JOIN a.teacherClassSubject tcs
            LEFT JOIN tcs.classRoom c
            LEFT JOIN c.academicYear ay
            LEFT JOIN tcs.teacher t
            WHERE (
                  (h.previousGrade IS NOT NULL AND h.newGrade IS NOT NULL AND h.previousGrade <> h.newGrade)
               OR (h.previousGrade IS NOT NULL AND h.newGrade IS NULL)
               OR (h.grade IS NULL AND h.newGrade IS NULL)
               OR (h.gradeKind = com.andretti101.escolaweb.model.enums.GradeKind.RECOVERY
                   AND h.previousGrade IS NULL AND h.newGrade IS NOT NULL)
            )
              AND (:teacherId IS NULL OR t.id = :teacherId)
              AND (:classRoomId IS NULL OR c.id = :classRoomId)
              AND (:studentId IS NULL OR s.id = :studentId)
              AND (:yearId IS NULL OR ay.id = :yearId)
              AND (:periodId IS NULL OR p.id = :periodId)
              AND h.changedAt >= :from
              AND h.changedAt < :to
            """)
    Page<GradeHistory> search(@Param("teacherId") Integer teacherId,
                              @Param("classRoomId") Integer classRoomId,
                              @Param("studentId") Integer studentId,
                              @Param("yearId") Integer yearId,
                              @Param("periodId") Integer periodId,
                              @Param("from") LocalDateTime from,
                              @Param("to") LocalDateTime to,
                              Pageable pageable);

    @Query("""
            SELECT h FROM GradeHistory h
            JOIN FETCH h.grade g
            JOIN FETCH g.student s
            JOIN FETCH g.assessment a
            LEFT JOIN FETCH a.period p
            JOIN FETCH a.teacherClassSubject tcs
            JOIN FETCH tcs.subject sub
            JOIN FETCH tcs.classRoom c
            JOIN FETCH tcs.teacher t
            LEFT JOIN FETCH h.changedBy u
            WHERE g.id = :gradeId
              AND (
                  (h.previousGrade IS NOT NULL AND h.newGrade IS NOT NULL AND h.previousGrade <> h.newGrade)
               OR (h.previousGrade IS NOT NULL AND h.newGrade IS NULL)
               OR (h.grade IS NULL AND h.newGrade IS NULL)
               OR (h.gradeKind = com.andretti101.escolaweb.model.enums.GradeKind.RECOVERY
                   AND h.previousGrade IS NULL AND h.newGrade IS NOT NULL)
              )
            ORDER BY h.changedAt DESC, h.id DESC
            """)
    List<GradeHistory> findTimelineByGradeId(@Param("gradeId") Integer gradeId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE GradeHistory h SET h.grade = null WHERE h.grade.id = :gradeId")
    int unlinkByGradeId(@Param("gradeId") Integer gradeId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE GradeHistory h SET h.grade = null WHERE h.grade.id IN (SELECT g.id FROM Grade g WHERE g.assessment.id = :assessmentId)")
    int unlinkByAssessmentId(@Param("assessmentId") Integer assessmentId);
}
