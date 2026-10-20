package com.andretti101.escolaweb.model.entity;

import com.andretti101.escolaweb.model.enums.DayOfWeekEnum;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "timetable_slots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class TimetableSlot implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull(message = "A turma é obrigatória.")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", nullable = false)
    private ClassRoom classRoom;

    @NotNull(message = "A vinculação professor-disciplina é obrigatória.")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_class_subject_id", nullable = false)
    private TeacherClassSubject teacherClassSubject;

    @NotNull(message = "O dia da semana é obrigatório.")
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 20)
    private DayOfWeekEnum dayOfWeek;

    @NotNull(message = "O horário de início é obrigatório.")
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @NotNull(message = "O horário de término é obrigatório.")
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @NotNull(message = "A ordem da aula é obrigatória.")
    @Min(value = 1, message = "A ordem da aula deve ser no mínimo 1.")
    @Column(name = "slot_order", nullable = false)
    private Integer slotOrder;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
