package com.andretti101.escolaweb.model.entity;

import com.andretti101.escolaweb.model.enums.UserType;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.validator.constraints.br.CPF;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Name is required.")
    @Size(max = 150, message = "Name must have at most 150 characters.")
    @Column(nullable = false, length = 150)
    private String nome;

    @Email(message = "Invalid email.")
    @NotBlank(message = "Email is required.")
    @Size(max = 150, message = "Email must have at most 150 characters.")
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotBlank(message = "Password is required.")
    @Column(nullable = false, length = 255)
    private String senha;

    @Size(max = 20, message = "Phone number must have at most 20 characters.")
    @Column(length = 20)
    private String telefone;

    @CPF(message = "Invalid CPF.")
    @Column(unique = true, length = 14)
    private String cpf;

    @Builder.Default
    @Column(nullable = false)
    private boolean ativo = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserType tipo;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
