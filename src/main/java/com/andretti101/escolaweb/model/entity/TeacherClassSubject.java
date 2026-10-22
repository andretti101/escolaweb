package com.andretti101.escolaweb.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

// Entidade associativa que consolida a atribuição de um professor a uma disciplina e turma com regras de carga horária
@Entity
@Table(
        name = "teacher_class_subject",
        uniqueConstraints = @UniqueConstraint(columnNames = {"class_room_id", "subject_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class TeacherClassSubject implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_room_id", nullable = false)
    private ClassRoom classRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @NotNull(message = "O número de aulas semanais é obrigatório.")
    @Min(value = 1, message = "O professor pode dar no mínimo 1 aula semanal para a turma.")
    @Max(value = 4, message = "O professor pode dar no máximo 4 aulas semanais para a turma.")
    @Builder.Default
    @Column(name = "weekly_lessons", nullable = false)
    private Integer weeklyLessons = 2;

    @Min(value = 1, message = "O número mínimo de avaliações deve ser ao menos 1.")
    @Column(name = "min_assessments_per_period")
    private Integer minAssessmentsPerPeriod;

    @Min(value = 1, message = "O número máximo de avaliações deve ser ao menos 1.")
    @Column(name = "max_assessments_per_period")
    private Integer maxAssessmentsPerPeriod;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "teacherClassSubject", fetch = FetchType.LAZY)
    private List<Lesson> lessons;

    @OneToMany(mappedBy = "teacherClassSubject", fetch = FetchType.LAZY)
    private List<Assessment> assessments;
}
