package com.grupo3.BookVerse.features.groups.groupMembershipRequest.domain;

import com.grupo3.BookVerse.features.groups.readingGroups.domain.ReadingGroupEntity;
import com.grupo3.BookVerse.features.users.domain.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "group_membership_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMembershipRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "id_external",
            nullable = false,
            unique = true
    )
    private UUID idExternal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "group_id",
            nullable = false
    )
    private ReadingGroupEntity group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "target_user_id",
            nullable = false
    )
    private UserEntity targetUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "created_by_id",
            nullable = false
    )
    private UserEntity createdBy;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "type",
            nullable = false
    )
    private GroupMembershipRequestType type;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false
    )
    private GroupMembershipRequestStatus status;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @PrePersist
    protected void onCreate() {

        if (idExternal == null) {
            idExternal = UUID.randomUUID();
        }

        if (status == null) {
            status = GroupMembershipRequestStatus.PENDING;
        }

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}