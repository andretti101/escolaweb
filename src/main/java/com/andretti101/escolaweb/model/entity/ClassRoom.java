package com.andretti101.escolaweb.model.entity;

import com.andretti101.escolaweb.model.enums.Shift;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "class_rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class ClassRoom implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "O nome da turma é obrigatório.")
    @Size(max = 20, message = "O nome da turma deve ter no máximo 20 caracteres.")
    @Column(name = "number", nullable = false, length = 20)
    private String name;

    @Column(name = "creation_year")
    private Integer creationYear;

    @NotNull(message = "O turno é obrigatório.")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Shift shift;

    @NotNull(message = "O ano letivo é obrigatório.")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "classRoom", fetch = FetchType.LAZY)
    private List<Enrollment> enrollments;

    @OneToMany(mappedBy = "classRoom", fetch = FetchType.LAZY)
    private List<TeacherClassSubject> teacherClassSubjects;
}
