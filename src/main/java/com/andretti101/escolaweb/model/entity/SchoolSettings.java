package com.andretti101.escolaweb.model.entity;

import com.andretti101.escolaweb.model.enums.PeriodType;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "configuracoes_escola")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class SchoolSettings implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Size(max = 150, message = "School name must have at most 150 characters.")
    @Column(name = "nome_escola", length = 150)
    private String schoolName;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_periodo", length = 10)
    private PeriodType periodType = PeriodType.TRIMESTRE;

    @Builder.Default
    @DecimalMin(value = "0.0", message = "Minimum attendance must be at least 0.")
    @DecimalMax(value = "100.0", message = "Minimum attendance must be at most 100.")
    @Column(name = "frequencia_minima", precision = 5, scale = 2)
    private BigDecimal minimumAttendance = new BigDecimal("70.00");

    @Builder.Default
    @DecimalMin(value = "0.0", message = "Minimum average must be at least 0.")
    @DecimalMax(value = "10.0", message = "Minimum average must be at most 10.")
    @Column(name = "media_minima", precision = 5, scale = 2)
    private BigDecimal minimumAverage = new BigDecimal("6.00");

    @Builder.Default
    @Column(name = "recuperacao_ativa", nullable = false)
    private boolean recoveryEnabled = true;

    @Builder.Default
    @Column(name = "boletim_liberado", nullable = false)
    private boolean reportCardReleased = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
