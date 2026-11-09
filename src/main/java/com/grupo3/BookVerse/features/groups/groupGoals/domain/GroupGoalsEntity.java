package com.grupo3.BookVerse.features.groups.groupGoals.domain;

import com.grupo3.BookVerse.features.groups.groupReadingCycle.domain.GroupReadingCycleEntity;
import com.grupo3.BookVerse.features.groups.readingGroups.domain.ReadingGroupEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "group_goals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupGoalsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_external", unique = true, nullable = false)
    private UUID idExternal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private ReadingGroupEntity group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reading_cycle_id")
    private GroupReadingCycleEntity readingCycle;

    @Enumerated(EnumType.STRING)
    @Column(name = "goal_type", nullable = false)
    private GoalType goalType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private GoalStatus status;


    @Column(name = "target_progress", nullable = false)
    private Integer targetProgress;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {

        if (idExternal == null) {
            idExternal = UUID.randomUUID();
        }

        if (status == null) {
            status = GoalStatus.ACTIVE;
        }

        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
