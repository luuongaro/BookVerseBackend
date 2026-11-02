package com.grupo3.BookVerse.features.users.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_profile_appearance")
@Getter @Setter @NoArgsConstructor
public class ProfileAppearanceEntity {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(name = "cover_index", nullable = false)
    private int coverIndex;

    @Column(name = "avatar_index", nullable = false)
    private int avatarIndex;
}
