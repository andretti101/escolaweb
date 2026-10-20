package com.andretti101.escolaweb.model.entity;

import com.andretti101.escolaweb.model.enums.DayOfWeekEnum;
import com.andretti101.escolaweb.model.enums.MealPeriodEnum;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "meal_menu_slots", uniqueConstraints = {
        @UniqueConstraint(name = "uk_meal_menu_slots_year_day_period", columnNames = {"academic_year_id", "day_of_week", "meal_period"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class MealMenuSlot implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull(message = "O ano letivo é obrigatório.")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @NotNull(message = "O dia da semana é obrigatório.")
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 20)
    private DayOfWeekEnum dayOfWeek;

    @NotNull(message = "O período da refeição é obrigatório.")
    @Enumerated(EnumType.STRING)
    @Column(name = "meal_period", nullable = false, length = 20)
    private MealPeriodEnum mealPeriod;

    @NotBlank(message = "O título da refeição é obrigatório.")
    @Size(max = 150, message = "O título deve ter no máximo 150 caracteres.")
    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres.")
    @Column(name = "description", length = 500)
    private String description;

    @Size(max = 255, message = "As observações nutricionais devem ter no máximo 255 caracteres.")
    @Column(name = "nutritional_notes", length = 255)
    private String nutritionalNotes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
