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
@Table(name = "boletins")
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

    @NotNull(message = "Student is required.")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_aluno", nullable = false)
    private Student student;

    @NotNull(message = "Teacher-class-subject assignment is required.")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_professor_turma_materia", nullable = false)
    private TeacherClassSubject teacherClassSubject;

    @NotNull(message = "Period is required.")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_periodo", nullable = false)
    private AcademicPeriod period;

    @DecimalMin(value = "0.0", message = "Final average must be at least 0.")
    @DecimalMax(value = "10.0", message = "Final average must be at most 10.")
    @Column(name = "media_final", precision = 5, scale = 2)
    private BigDecimal finalAverage;

    @DecimalMin(value = "0.0", message = "Attendance percentage must be at least 0.")
    @DecimalMax(value = "100.0", message = "Attendance percentage must be at most 100.")
    @Column(name = "frequencia_percentual", precision = 5, scale = 2)
    private BigDecimal attendancePercentage;

    @Enumerated(EnumType.STRING)
    @Column(length = 25)
    private StudentSituation situacao;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
