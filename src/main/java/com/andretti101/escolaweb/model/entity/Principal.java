package com.andretti101.escolaweb.model.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@Entity
@Table(name = "principals")
@DiscriminatorValue("PRINCIPAL")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Principal extends User {

    private static final long serialVersionUID = 1L;
}
