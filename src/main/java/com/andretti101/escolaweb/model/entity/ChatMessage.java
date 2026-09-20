package com.andretti101.escolaweb.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class ChatMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @NotNull(message = "O remetente é obrigatório.")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @NotNull(message = "A turma é obrigatória.")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", nullable = false)
    private ClassRoom classroom;

    @Column(name = "is_edited", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean isEdited = false;

    @Column(name = "is_deleted", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean isDeleted = false;

    @Column(name = "deleted_by_admin", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean deletedByAdmin = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replied_to_id", nullable = true)
    private ChatMessage repliedTo;
}
