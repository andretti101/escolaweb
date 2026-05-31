package com.andretti101.escolaweb.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "aulas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Lesson implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull(message = "Teacher-class-subject assignment is required.")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_professor_turma_materia", nullable = false)
    private TeacherClassSubject teacherClassSubject;

    @NotNull(message = "Lesson date is required.")
    @Column(name = "data_aula", nullable = false)
    private LocalDate lessonDate;

    @Column(columnDefinition = "TEXT")
    private String conteudo;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "lesson", fetch = FetchType.LAZY)
    private List<Attendance> attendances;
}
