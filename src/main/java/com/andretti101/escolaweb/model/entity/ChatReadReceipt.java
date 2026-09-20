package com.andretti101.escolaweb.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_read_receipts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatReadReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", nullable = false)
    private ClassRoom classroom;

    @Column(name = "last_read_message_id", nullable = false)
    private Integer lastReadMessageId;

    @CreationTimestamp
    @Column(name = "last_read_at", nullable = false)
    private LocalDateTime lastReadAt;
}
