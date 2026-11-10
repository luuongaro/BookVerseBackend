package com.grupo3.BookVerse.features.groups.groupReadingParticipant.domain;

import com.grupo3.BookVerse.features.groups.groupMember.domain.GroupMemberEntity;
import com.grupo3.BookVerse.features.groups.groupReadingCycle.domain.GroupReadingCycleEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "group_reading_participants",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {
                                "reading_cycle_id",
                                "group_member_id"
                        }
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupReadingParticipantEntity {

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
            name = "reading_cycle_id",
            nullable = false
    )
    private GroupReadingCycleEntity readingCycle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "group_member_id",
            nullable = false
    )
    private GroupMemberEntity groupMember;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private GroupReadingParticipantStatus status;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        if (idExternal == null) {
            idExternal = UUID.randomUUID();
        }

        if (status == null) {
            status = GroupReadingParticipantStatus.ACTIVE;
        }

        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }

        validateMembershipAndCycle();
    }

    @PreUpdate
    protected void onUpdate() {
        validateMembershipAndCycle();
    }

    private void validateMembershipAndCycle() {
        if (readingCycle == null
                || readingCycle.getGroup() == null
                || groupMember == null
                || groupMember.getGroup() == null) {
            return;
        }

        Long cycleGroupId =
                readingCycle.getGroup().getId();

        Long memberGroupId =
                groupMember.getGroup().getId();

        if (!java.util.Objects.equals(
                cycleGroupId,
                memberGroupId
        )) {
            throw new IllegalStateException(
                    "The participant membership must belong to the reading cycle's group"
            );
        }
    }
}