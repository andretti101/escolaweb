package com.andretti101.escolaweb.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "students")
@DiscriminatorValue("STUDENT")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Student extends User {

    private static final long serialVersionUID = 1L;

    @Size(max = 20, message = "O telefone deve ter no máximo 20 caracteres.")
    @Column(name = "own_phone", length = 20)
    private String ownPhone;

    @Size(max = 20, message = "O telefone do responsável deve ter no máximo 20 caracteres.")
    @Column(name = "guardian_phone", length = 20)
    private String guardianPhone;

    @NotBlank(message = "O número de matrícula é obrigatório.")
    @Size(max = 30, message = "O número de matrícula deve ter no máximo 30 caracteres.")
    @Column(name = "enrollment", nullable = false, unique = true, length = 30)
    private String registrationNumber;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @OneToMany(mappedBy = "student", fetch = FetchType.LAZY)
    private List<Enrollment> enrollments;

    @OneToMany(mappedBy = "student", fetch = FetchType.LAZY)
    private List<Attendance> attendances;

    @OneToMany(mappedBy = "student", fetch = FetchType.LAZY)
    private List<Grade> grades;
}
