package com.andretti101.escolaweb.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "anos_letivos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class AcademicYear implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull(message = "Year is required.")
    @Column(nullable = false, unique = true)
    private Integer ano;

    @Builder.Default
    @Column(nullable = false)
    private boolean ativo = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "academicYear", fetch = FetchType.LAZY)
    private List<AcademicPeriod> periods;

    @OneToMany(mappedBy = "academicYear", fetch = FetchType.LAZY)
    private List<ClassRoom> classRooms;
}
