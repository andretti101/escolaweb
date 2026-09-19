package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.model.entity.Announcement;

import java.util.List;

public interface AnnouncementService {
    Announcement create(Announcement announcement, List<Integer> classRoomIds);
    Announcement findById(Integer id);
    List<Announcement> findAll();
    List<Announcement> findByAuthorId(Integer authorId);
    List<Announcement> findByStudentClassRooms(Integer studentId);
    List<Announcement> findByTeacherClassRooms(Integer teacherId);
    List<Announcement> findForTeachers();
    Announcement update(Integer id, Announcement updated, List<Integer> classRoomIds, Integer authenticatedUserId);
    void delete(Integer id, Integer userId);
    
    boolean hasUnreadAnnouncements(Integer userId);
    void markAnnouncementsAsRead(Integer userId);
    org.springframework.data.domain.Page<com.andretti101.escolaweb.model.entity.Announcement> findWithFilters(Integer authorId, Integer classRoomId, Boolean targetTeachers, org.springframework.data.domain.Pageable pageable);
}