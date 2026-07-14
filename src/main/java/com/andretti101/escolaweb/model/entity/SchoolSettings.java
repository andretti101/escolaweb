package com.andretti101.escolaweb.model.entity;

import com.andretti101.escolaweb.model.enums.AcademicPeriodType;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
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

    @Size(max = 255, message = "O endereço deve ter no máximo 255 caracteres.")
    @Column(length = 255)
    private String address;

    @Size(max = 20, message = "O telefone deve ter no máximo 20 caracteres.")
    @Column(length = 20)
    private String phone;

    @Email(message = "E-mail inválido.")
    @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres.")
    @Column(length = 150)
    private String email;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", length = 20)
    private AcademicPeriodType periodType = AcademicPeriodType.TRIMESTER;

    @Builder.Default
    @DecimalMin(value = "0.0", message = "A frequência mínima deve ser de pelo menos 0.")
    @DecimalMax(value = "100.0", message = "A frequência mínima deve ser no máximo 100.")
    @Column(name = "minimum_attendance", precision = 5, scale = 2)
    private BigDecimal minimumAttendance = new BigDecimal("70.00");

    @Builder.Default
    @DecimalMin(value = "0.0", message = "A média mínima deve ser de pelo menos 0.")
    @DecimalMax(value = "10.0", message = "A média mínima deve ser no máximo 10.")
    @Column(name = "minimum_average", precision = 3, scale = 1)
    private BigDecimal minimumGrade = new BigDecimal("6.0");

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}