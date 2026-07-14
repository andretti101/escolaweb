package com.andretti101.escolaweb.model.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.util.List;

@Entity
@Table(name = "teachers")
@DiscriminatorValue("TEACHER")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Teacher extends User {

    private static final long serialVersionUID = 1L;

    @OneToMany(mappedBy = "teacher", fetch = FetchType.LAZY)
    private List<TeacherClassSubject> teacherClassSubjects;
}
