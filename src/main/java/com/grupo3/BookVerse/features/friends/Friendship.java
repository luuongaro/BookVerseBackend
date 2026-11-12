package com.grupo3.BookVerse.features.friends;

import com.grupo3.BookVerse.features.users.domain.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name = "friendships", uniqueConstraints = @UniqueConstraint(columnNames = {"user_low_id", "user_high_id"}))
@Getter @Setter @NoArgsConstructor
public class Friendship {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "id_external", nullable = false, unique = true) private UUID idExternal;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_low_id") private UserEntity userLow;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_high_id") private UserEntity userHigh;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "requester_id") private UserEntity requester;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Status status;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    public enum Status { PENDING, ACCEPTED }
}
