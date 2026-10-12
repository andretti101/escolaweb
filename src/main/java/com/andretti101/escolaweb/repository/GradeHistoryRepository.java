package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.GradeHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GradeHistoryRepository extends JpaRepository<GradeHistory, Integer> {

    @Query(value = """
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
            WHERE (:teacherId IS NULL OR t.id = :teacherId)
              AND (:classRoomId IS NULL OR c.id = :classRoomId)
              AND (:studentId IS NULL OR s.id = :studentId)
              AND (:yearId IS NULL OR c.academicYear.id = :yearId)
              AND h.changedAt >= :from
              AND h.changedAt < :to
            """,
            countQuery = """
            SELECT COUNT(h) FROM GradeHistory h
            JOIN h.grade g
            JOIN g.student s
            JOIN g.assessment a
            JOIN a.teacherClassSubject tcs
            JOIN tcs.classRoom c
            JOIN tcs.teacher t
            WHERE (:teacherId IS NULL OR t.id = :teacherId)
              AND (:classRoomId IS NULL OR c.id = :classRoomId)
              AND (:studentId IS NULL OR s.id = :studentId)
              AND (:yearId IS NULL OR c.academicYear.id = :yearId)
              AND h.changedAt >= :from
              AND h.changedAt < :to
            """)
    Page<GradeHistory> search(@Param("teacherId") Integer teacherId,
                              @Param("classRoomId") Integer classRoomId,
                              @Param("studentId") Integer studentId,
                              @Param("yearId") Integer yearId,
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
            ORDER BY h.changedAt DESC, h.id DESC
            """)
    List<GradeHistory> findTimelineByGradeId(@Param("gradeId") Integer gradeId);
}
