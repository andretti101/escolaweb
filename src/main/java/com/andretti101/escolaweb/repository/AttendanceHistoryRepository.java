package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.AttendanceHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AttendanceHistoryRepository extends JpaRepository<AttendanceHistory, Integer> {

    @Query(value = """
            SELECT h FROM AttendanceHistory h
            JOIN FETCH h.attendance att
            JOIN FETCH att.student s
            JOIN FETCH att.lesson l
            JOIN FETCH l.teacherClassSubject tcs
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
            SELECT COUNT(h) FROM AttendanceHistory h
            JOIN h.attendance att
            JOIN att.student s
            JOIN att.lesson l
            JOIN l.teacherClassSubject tcs
            JOIN tcs.classRoom c
            JOIN tcs.teacher t
            WHERE (:teacherId IS NULL OR t.id = :teacherId)
              AND (:classRoomId IS NULL OR c.id = :classRoomId)
              AND (:studentId IS NULL OR s.id = :studentId)
              AND (:yearId IS NULL OR c.academicYear.id = :yearId)
              AND h.changedAt >= :from
              AND h.changedAt < :to
            """)
    Page<AttendanceHistory> search(@Param("teacherId") Integer teacherId,
                                   @Param("classRoomId") Integer classRoomId,
                                   @Param("studentId") Integer studentId,
                                   @Param("yearId") Integer yearId,
                                   @Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to,
                                   Pageable pageable);

    @Query("""
            SELECT h FROM AttendanceHistory h
            JOIN FETCH h.attendance att
            JOIN FETCH att.student s
            JOIN FETCH att.lesson l
            JOIN FETCH l.teacherClassSubject tcs
            JOIN FETCH tcs.subject sub
            JOIN FETCH tcs.classRoom c
            JOIN FETCH tcs.teacher t
            LEFT JOIN FETCH h.changedBy u
            WHERE att.id = :attendanceId
            ORDER BY h.changedAt DESC, h.id DESC
            """)
    List<AttendanceHistory> findTimelineByAttendanceId(@Param("attendanceId") Integer attendanceId);
}
