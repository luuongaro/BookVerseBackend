package com.grupo3.BookVerse.features.groups.groupMember.domain;

import com.grupo3.BookVerse.features.groups.readingGroups.domain.ReadingGroupEntity;
import com.grupo3.BookVerse.features.users.domain.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "group_members", uniqueConstraints =
        {@UniqueConstraint(columnNames = {"group_id", "user_id"})})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class GroupMemberEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // PK

    @Column(name = "id_external", unique = true, nullable = false)
    private UUID idExternal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private ReadingGroupEntity group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user; //FK

    @Enumerated(EnumType.STRING)
    @Column(name = "member_type", nullable = false)
    private GroupMemberType memberType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private GroupMemberStatus status;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() {
        if (idExternal == null) {
            idExternal = UUID.randomUUID();
        }
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = GroupMemberStatus.ACTIVE;
        }
    }

}
