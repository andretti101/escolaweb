package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Integer> {

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"author", "classRooms"})
    @Query("SELECT DISTINCT a FROM Announcement a LEFT JOIN a.classRooms c WHERE a.archived = false AND ((c.id IN :classRoomIds) OR (a.targetTeachers = true)) ORDER BY a.createdAt DESC")
    List<Announcement> findByClassRoomIdsOrTargetTeachers(@Param("classRoomIds") List<Integer> classRoomIds);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"author", "classRooms"})
    @Query("SELECT DISTINCT a FROM Announcement a JOIN a.classRooms c WHERE a.archived = false AND c.id IN :classRoomIds ORDER BY a.createdAt DESC")
    List<Announcement> findByClassRoomIds(@Param("classRoomIds") List<Integer> classRoomIds);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"author", "classRooms"})
    @Query("SELECT a FROM Announcement a WHERE a.archived = false AND a.targetTeachers = true ORDER BY a.createdAt DESC")
    List<Announcement> findByTargetTeachersTrue();

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"author", "classRooms"})
    @Query("SELECT a FROM Announcement a WHERE a.archived = false ORDER BY a.createdAt DESC")
    List<Announcement> findAllByOrderByCreatedAtDesc();

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"author", "classRooms"})
    @Query("SELECT a FROM Announcement a WHERE a.archived = false AND a.author.id = :authorId ORDER BY a.eventDate ASC")
    List<Announcement> findByAuthorIdOrderByEventDateAsc(@Param("authorId") Integer authorId);

    @Query("SELECT COUNT(a) FROM Announcement a JOIN a.classRooms c WHERE a.archived = false AND c.id IN :classRoomIds AND a.author.id != :userId AND a.createdAt > :lastRead")
    long countUnreadByClassRoomIds(@Param("classRoomIds") List<Integer> classRoomIds, @Param("userId") Integer userId, @Param("lastRead") java.time.LocalDateTime lastRead);

    @Query("SELECT COUNT(a) FROM Announcement a WHERE a.archived = false AND a.targetTeachers = true AND a.author.id != :userId AND a.createdAt > :lastRead")
    long countUnreadForTeachers(@Param("userId") Integer userId, @Param("lastRead") java.time.LocalDateTime lastRead);

    @Query("SELECT COUNT(a) FROM Announcement a WHERE a.archived = false AND a.author.id != :userId AND a.createdAt > :lastRead")
    long countAllUnread(@Param("userId") Integer userId, @Param("lastRead") java.time.LocalDateTime lastRead);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"author", "classRooms", "classRooms.academicYear"})
    @Query(value = "SELECT DISTINCT a FROM Announcement a LEFT JOIN a.classRooms c LEFT JOIN c.academicYear ay WHERE a.archived = false AND " +
           "(:authorId IS NULL OR a.author.id = :authorId) AND " +
           "(:classRoomId IS NULL OR c.id = :classRoomId) AND " +
           "(:academicYearId IS NULL OR ay.id = :academicYearId OR ay.year = :yearNumber OR ((c.id IS NULL OR a.targetTeachers = true) AND YEAR(a.createdAt) = :yearNumber)) AND " +
           "(:targetTeachers IS NULL OR COALESCE(a.targetTeachers, false) = :targetTeachers) " +
           "ORDER BY a.createdAt DESC",
           countQuery = "SELECT count(DISTINCT a) FROM Announcement a LEFT JOIN a.classRooms c LEFT JOIN c.academicYear ay WHERE a.archived = false AND " +
           "(:authorId IS NULL OR a.author.id = :authorId) AND " +
           "(:classRoomId IS NULL OR c.id = :classRoomId) AND " +
           "(:academicYearId IS NULL OR ay.id = :academicYearId OR ay.year = :yearNumber OR ((c.id IS NULL OR a.targetTeachers = true) AND YEAR(a.createdAt) = :yearNumber)) AND " +
           "(:targetTeachers IS NULL OR COALESCE(a.targetTeachers, false) = :targetTeachers)")
    org.springframework.data.domain.Page<Announcement> findWithFilters(
            @Param("authorId") Integer authorId,
            @Param("classRoomId") Integer classRoomId,
            @Param("academicYearId") Integer academicYearId,
            @Param("yearNumber") Integer yearNumber,
            @Param("targetTeachers") Boolean targetTeachers, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"author", "classRooms", "classRooms.academicYear"})
    @Query(value = "SELECT DISTINCT a FROM Announcement a LEFT JOIN a.classRooms c LEFT JOIN c.academicYear ay WHERE a.archived = true AND " +
           "(:authorId IS NULL OR a.author.id = :authorId) AND " +
           "(:classRoomId IS NULL OR c.id = :classRoomId) AND " +
           "(:academicYearId IS NULL OR ay.id = :academicYearId OR ay.year = :yearNumber OR ((c.id IS NULL OR a.targetTeachers = true) AND YEAR(a.createdAt) = :yearNumber)) AND " +
           "(:targetTeachers IS NULL OR COALESCE(a.targetTeachers, false) = :targetTeachers) " +
           "ORDER BY a.createdAt DESC",
           countQuery = "SELECT count(DISTINCT a) FROM Announcement a LEFT JOIN a.classRooms c LEFT JOIN c.academicYear ay WHERE a.archived = true AND " +
           "(:authorId IS NULL OR a.author.id = :authorId) AND " +
           "(:classRoomId IS NULL OR c.id = :classRoomId) AND " +
           "(:academicYearId IS NULL OR ay.id = :academicYearId OR ay.year = :yearNumber OR ((c.id IS NULL OR a.targetTeachers = true) AND YEAR(a.createdAt) = :yearNumber)) AND " +
           "(:targetTeachers IS NULL OR COALESCE(a.targetTeachers, false) = :targetTeachers)")
    org.springframework.data.domain.Page<Announcement> findArchivedWithFilters(
            @Param("authorId") Integer authorId,
            @Param("classRoomId") Integer classRoomId,
            @Param("academicYearId") Integer academicYearId,
            @Param("yearNumber") Integer yearNumber,
            @Param("targetTeachers") Boolean targetTeachers,
            org.springframework.data.domain.Pageable pageable);

    @Modifying
    @Query("UPDATE Announcement a SET a.archived = true WHERE a.archived = false")
    int archiveAll();
}