package com.andretti101.escolaweb.model.entity;

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
import java.util.List;

@Entity
@Table(
    name = "notas",
    uniqueConstraints = @UniqueConstraint(columnNames = {"id_aluno", "id_avaliacao"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Grade implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull(message = "Student is required.")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_aluno", nullable = false)
    private Student student;

    @NotNull(message = "Assessment is required.")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_avaliacao", nullable = false)
    private Assessment assessment;

    @DecimalMin(value = "0.0", message = "Grade must be at least 0.")
    @DecimalMax(value = "10.0", message = "Grade must be at most 10.")
    @Column(precision = 5, scale = 2)
    private BigDecimal nota;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "grade", fetch = FetchType.LAZY)
    private List<GradeHistory> history;
}
