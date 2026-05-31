package com.andretti101.escolaweb.model.entity;

import com.andretti101.escolaweb.model.enums.PeriodNumber;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "periodos_letivos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class AcademicPeriod implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull(message = "Academic year is required.")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ano_letivo", nullable = false)
    private AcademicYear academicYear;

    @NotNull(message = "Period number is required.")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 1)
    private PeriodNumber numero;

    @Column(name = "data_inicio")
    private LocalDate startDate;

    @Column(name = "data_fim")
    private LocalDate endDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "period", fetch = FetchType.LAZY)
    private List<Assessment> assessments;

    @OneToMany(mappedBy = "period", fetch = FetchType.LAZY)
    private List<ReportCard> reportCards;
}
