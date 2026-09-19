package com.andretti101.escolaweb.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "announcements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Announcement implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "O título é obrigatório.")
    @Size(max = 100, message = "O título deve ter no máximo 100 caracteres.")
    @Column(nullable = false, length = 100)
    private String title;

    @NotBlank(message = "A mensagem é obrigatória.")
    @Size(max = 2000, message = "A mensagem deve ter no máximo 2000 caracteres.")
    @Column(nullable = false, length = 2000)
    private String message;

    @Column(name = "event_date")
    private LocalDate eventDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @NotNull(message = "O autor é obrigatório.")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "announcement_classrooms",
            joinColumns = @JoinColumn(name = "announcement_id"),
            inverseJoinColumns = @JoinColumn(name = "class_room_id")
    )
    private Set<ClassRoom> classRooms = new HashSet<>();

    @Column(name = "target_teachers")
    private Boolean targetTeachers;
}
