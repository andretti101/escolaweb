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
@Table(name = "school_settings")
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

    @Size(max = 150, message = "O nome da escola deve ter no máximo 150 caracteres.")
    @Column(name = "school_name", length = 150)
    private String schoolName;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", length = 20)
    private PeriodType periodType = PeriodType.TRIMESTER;

    @Builder.Default
    @DecimalMin(value = "0.0", message = "A frequência mínima deve ser de pelo menos 0.")
    @DecimalMax(value = "100.0", message = "A frequência mínima deve ser no máximo 100.")
    @Column(name = "minimum_attendance", precision = 5, scale = 2)
    private BigDecimal minimumAttendance = new BigDecimal("70.00");

    @Builder.Default
    @DecimalMin(value = "0.0", message = "A média mínima deve ser de pelo menos 0.")
    @DecimalMax(value = "10.0", message = "A média mínima deve ser no máximo 10.")
    @Column(name = "minimum_average", precision = 3, scale = 1)
    private BigDecimal minimumAverage = new BigDecimal("6.0");

    @Builder.Default
    @Column(name = "recovery_enabled", nullable = false)
    private boolean recoveryEnabled = true;

    @Builder.Default
    @Column(name = "report_card_released", nullable = false)
    private boolean reportCardReleased = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}