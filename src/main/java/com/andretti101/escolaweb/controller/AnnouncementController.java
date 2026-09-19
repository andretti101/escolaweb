package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.AnnouncementRequestDTO;
import com.andretti101.escolaweb.dto.response.AnnouncementResponseDTO;
import com.andretti101.escolaweb.dto.response.ClassRoomSimpleDTO;
import com.andretti101.escolaweb.model.entity.Announcement;
import com.andretti101.escolaweb.model.entity.ClassRoom;
import com.andretti101.escolaweb.service.AnnouncementService;
import com.andretti101.escolaweb.service.AuthenticatedUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final AuthenticatedUserService authenticatedUserService;

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL')")
    public ResponseEntity<AnnouncementResponseDTO> create(@Valid @RequestBody AnnouncementRequestDTO dto) {
        Announcement announcement = toEntity(dto);
        Announcement created = announcementService.create(announcement, dto.classRoomIds());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SECRETARY', 'PRINCIPAL')")
    public ResponseEntity<org.springframework.data.domain.Page<AnnouncementResponseDTO>> findAll(
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer authorId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer classRoomId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Boolean targetTeachers,
            org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<com.andretti101.escolaweb.model.entity.Announcement> page = announcementService.findWithFilters(authorId, classRoomId, targetTeachers, pageable);
        return ResponseEntity.ok(page.map(this::toResponse));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'SECRETARY', 'PRINCIPAL')")
    public ResponseEntity<AnnouncementResponseDTO> findById(@PathVariable Integer id) {
        Announcement announcement = announcementService.findById(id);
        return ResponseEntity.ok(toResponse(announcement));
    }

    @GetMapping("/my-announcements")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL')")
    public ResponseEntity<List<AnnouncementResponseDTO>> findByAuthor() {
        Integer userId = authenticatedUserService.getAuthenticatedUser().getId();
        List<Announcement> announcements = announcementService.findByAuthorId(userId);
        return ResponseEntity.ok(announcements.stream().map(this::toResponse).toList());
    }

    @GetMapping("/my-teaching-classrooms")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<AnnouncementResponseDTO>> findByTeacherClassRooms() {
        try {
            Integer teacherId = authenticatedUserService.getAuthenticatedUser().getId();
            List<Announcement> announcements = announcementService.findByTeacherClassRooms(teacherId);
            return ResponseEntity.ok(announcements.stream().map(this::toResponse).toList());
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
    @GetMapping("/for-teachers")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<AnnouncementResponseDTO>> findForTeachers() {
        List<Announcement> announcements = announcementService.findForTeachers();
        return ResponseEntity.ok(announcements.stream().map(this::toResponse).toList());
    }


    @GetMapping("/my-classroom")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<AnnouncementResponseDTO>> findByStudentClassRooms() {
        Integer studentId = authenticatedUserService.getAuthenticatedStudent().getId();
        List<Announcement> announcements = announcementService.findByStudentClassRooms(studentId);
        return ResponseEntity.ok(announcements.stream().map(this::toResponse).toList());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL')")
    public ResponseEntity<AnnouncementResponseDTO> update(
            @PathVariable Integer id,
            @Valid @RequestBody AnnouncementRequestDTO dto) {
        Integer userId = authenticatedUserService.getAuthenticatedUser().getId();
        Announcement updated = toEntity(dto);
        Announcement result = announcementService.update(id, updated, dto.classRoomIds(), userId);
        return ResponseEntity.ok(toResponse(result));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        Integer userId = authenticatedUserService.getAuthenticatedUser().getId();
        announcementService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }

    // ── Mapping

    private Announcement toEntity(AnnouncementRequestDTO dto) {
        Announcement announcement = new Announcement();
        announcement.setTitle(dto.title());
        announcement.setMessage(dto.message());
        announcement.setEventDate(dto.eventDate());
        announcement.setTargetTeachers(dto.targetTeachers() != null ? dto.targetTeachers() : false);
        return announcement;
    }

    private AnnouncementResponseDTO toResponse(Announcement a) {
        List<ClassRoomSimpleDTO> classRoomDtos = a.getClassRooms().stream()
                .map(this::toClassRoomSimple)
                .toList();

        return new AnnouncementResponseDTO(
                a.getId(),
                a.getTitle(),
                a.getMessage(),
                a.getEventDate(),
                a.getCreatedAt(),
                a.getAuthor().getId(),
                a.getAuthor().getName(),
                a.getAuthor().getRole().getLabel(),
                classRoomDtos,
                a.getTargetTeachers() != null ? a.getTargetTeachers() : false
        );
    }

    private ClassRoomSimpleDTO toClassRoomSimple(ClassRoom c) {
        return new ClassRoomSimpleDTO(c.getId(), c.getName());
    }

    @GetMapping("/has-unread")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Boolean> hasUnreadAnnouncements() {
        Integer userId = authenticatedUserService.getAuthenticatedUser().getId();
        return ResponseEntity.ok(announcementService.hasUnreadAnnouncements(userId));
    }

    @PostMapping("/mark-read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAsRead() {
        Integer userId = authenticatedUserService.getAuthenticatedUser().getId();
        announcementService.markAnnouncementsAsRead(userId);
        return ResponseEntity.ok().build();
    }
}
