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
@Table(name = "assessments")
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

    @NotNull(message = "A atribuição de professor, turma e matéria é obrigatória.")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_class_subject_id", nullable = false)
    private TeacherClassSubject teacherClassSubject;

    @NotNull(message = "O período é obrigatório.")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "period_id", nullable = false)
    private AcademicPeriod period;

    @NotBlank(message = "O título é obrigatório.")
    @Size(max = 100, message = "O título deve ter no máximo 100 caracteres.")
    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @DecimalMin(value = "0.0", message = "O valor máximo deve ser de pelo menos 0.")
    @DecimalMax(value = "999.99", message = "O valor máximo deve ser no máximo 999.99.")
    @Column(name = "maximum_score", precision = 5, scale = 2)
    private BigDecimal maximumScore = BigDecimal.TEN;

    @Column(name = "assessment_date")
    private LocalDate assessmentDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private AssessmentType type = AssessmentType.EXAM; // Atenção: alterado de PROVA para EXAM!

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "assessment", fetch = FetchType.LAZY)
    private List<Grade> grades;
}