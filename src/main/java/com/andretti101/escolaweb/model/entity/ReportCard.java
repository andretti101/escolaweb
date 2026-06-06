package com.andretti101.escolaweb.model.entity;

import com.andretti101.escolaweb.model.enums.StudentSituation;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class ReportCard implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull(message = "O aluno é obrigatório.")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @NotNull(message = "A atribuição de professor, turma e matéria é obrigatória.")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_class_subject_id", nullable = false)
    private TeacherClassSubject teacherClassSubject;

    @NotNull(message = "O período é obrigatório.")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "period_id", nullable = false)
    private AcademicPeriod period;

    @DecimalMin(value = "0.0", message = "A média final deve ser de pelo menos 0.")
    @DecimalMax(value = "10.0", message = "A média final deve ser no máximo 10.")
    @Column(name = "final_average", precision = 5, scale = 2)
    private BigDecimal finalAverage;

    @DecimalMin(value = "0.0", message = "O percentual de frequência deve ser de pelo menos 0.")
    @DecimalMax(value = "100.0", message = "O percentual de frequência deve ser no máximo 100.")
    @Column(name = "attendance_percentage", precision = 5, scale = 2)
    private BigDecimal attendancePercentage;

    @Enumerated(EnumType.STRING)
    @Column(length = 25)
    private StudentSituation situation;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}