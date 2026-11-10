package com.grupo3.BookVerse.features.users.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "profile_badges")
@Getter @Setter @NoArgsConstructor
public class ProfileBadgeEntity {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "badge", nullable = false, length = 32)
    private ProfileBadgeType badge;
}
