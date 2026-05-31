package com.andretti101.escolaweb.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "historico_frequencia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class AttendanceHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull(message = "Attendance record is required.")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_chamada", nullable = false)
    private Attendance attendance;

    @Column(name = "status_antigo", length = 30)
    private String oldStatus;

    @Column(name = "status_novo", length = 30)
    private String newStatus;

    @CreationTimestamp
    @Column(name = "alterado_em", updatable = false)
    private LocalDateTime changedAt;
}
