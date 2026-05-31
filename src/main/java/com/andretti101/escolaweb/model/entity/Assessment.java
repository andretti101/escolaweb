package com.andretti101.escolaweb.model.entity;

import com.andretti101.escolaweb.model.enums.AssessmentType;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "avaliacoes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Assessment implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull(message = "Teacher-class-subject assignment is required.")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_professor_turma_materia", nullable = false)
    private TeacherClassSubject teacherClassSubject;

    @NotNull(message = "Period is required.")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_periodo", nullable = false)
    private AcademicPeriod period;

    @NotBlank(message = "Title is required.")
    @Size(max = 100, message = "Title must have at most 100 characters.")
    @Column(nullable = false, length = 100)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Builder.Default
    @DecimalMin(value = "0.0", message = "Maximum value must be at least 0.")
    @DecimalMax(value = "999.99", message = "Maximum value must be at most 999.99.")
    @Column(name = "valor_maximo", precision = 5, scale = 2)
    private BigDecimal maximumScore = BigDecimal.TEN;

    @Column(name = "data_avaliacao")
    private LocalDate assessmentDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private AssessmentType tipo = AssessmentType.PROVA;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "assessment", fetch = FetchType.LAZY)
    private List<Grade> grades;
}
