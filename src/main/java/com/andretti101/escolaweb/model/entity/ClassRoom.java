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
@Table(name = "turmas")
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

    @NotBlank(message = "Class number is required.")
    @Size(max = 20, message = "Class number must have at most 20 characters.")
    @Column(nullable = false, length = 20)
    private String numero;

    @NotNull(message = "Shift is required.")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Shift turno;

    @NotNull(message = "Academic year is required.")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ano_letivo", nullable = false)
    private AcademicYear academicYear;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "classRoom", fetch = FetchType.LAZY)
    private List<Student> students;

    @OneToMany(mappedBy = "classRoom", fetch = FetchType.LAZY)
    private List<TeacherClassSubject> teacherClassSubjects;
}
