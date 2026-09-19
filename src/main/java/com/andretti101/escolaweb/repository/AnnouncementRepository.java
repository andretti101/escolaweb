package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Integer> {

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"author", "classRooms"})
    @Query("SELECT DISTINCT a FROM Announcement a LEFT JOIN a.classRooms c WHERE (c.id IN :classRoomIds) OR (a.targetTeachers = true) ORDER BY a.createdAt DESC")
    List<Announcement> findByClassRoomIdsOrTargetTeachers(@Param("classRoomIds") List<Integer> classRoomIds);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"author", "classRooms"})
    @Query("SELECT DISTINCT a FROM Announcement a JOIN a.classRooms c WHERE c.id IN :classRoomIds ORDER BY a.createdAt DESC")
    List<Announcement> findByClassRoomIds(@Param("classRoomIds") List<Integer> classRoomIds);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"author", "classRooms"})
    @Query("SELECT a FROM Announcement a WHERE a.targetTeachers = true ORDER BY a.createdAt DESC")
    List<Announcement> findByTargetTeachersTrue();

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"author", "classRooms"})
    List<Announcement> findAllByOrderByCreatedAtDesc();

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"author", "classRooms"})
    List<Announcement> findByAuthorIdOrderByEventDateAsc(Integer authorId);

    @Query("SELECT COUNT(a) FROM Announcement a JOIN a.classRooms c WHERE c.id IN :classRoomIds AND a.author.id != :userId AND a.createdAt > :lastRead")
    long countUnreadByClassRoomIds(@org.springframework.data.repository.query.Param("classRoomIds") List<Integer> classRoomIds, @org.springframework.data.repository.query.Param("userId") Integer userId, @org.springframework.data.repository.query.Param("lastRead") java.time.LocalDateTime lastRead);

    @Query("SELECT COUNT(a) FROM Announcement a WHERE a.targetTeachers = true AND a.author.id != :userId AND a.createdAt > :lastRead")
    long countUnreadForTeachers(@org.springframework.data.repository.query.Param("userId") Integer userId, @org.springframework.data.repository.query.Param("lastRead") java.time.LocalDateTime lastRead);

    @Query("SELECT COUNT(a) FROM Announcement a WHERE a.author.id != :userId AND a.createdAt > :lastRead")
    long countAllUnread(@org.springframework.data.repository.query.Param("userId") Integer userId, @org.springframework.data.repository.query.Param("lastRead") java.time.LocalDateTime lastRead);


    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"author", "classRooms"})
    @Query("SELECT DISTINCT a FROM Announcement a LEFT JOIN a.classRooms c WHERE " +
           "(:authorId IS NULL OR a.author.id = :authorId) AND " +
           "(:classRoomId IS NULL OR c.id = :classRoomId) AND " +
           "(:targetTeachers IS NULL OR a.targetTeachers = :targetTeachers)")
    org.springframework.data.domain.Page<Announcement> findWithFilters(
            @org.springframework.data.repository.query.Param("authorId") Integer authorId,
            @org.springframework.data.repository.query.Param("classRoomId") Integer classRoomId,
            @org.springframework.data.repository.query.Param("targetTeachers") Boolean targetTeachers, org.springframework.data.domain.Pageable pageable);

}