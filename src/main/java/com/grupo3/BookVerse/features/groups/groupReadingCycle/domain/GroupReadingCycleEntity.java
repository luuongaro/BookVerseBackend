package com.grupo3.BookVerse.features.groups.groupReadingCycle.domain;

import com.grupo3.BookVerse.features.books.domain.BookEntity;
import com.grupo3.BookVerse.features.groups.groupGoals.domain.GroupGoalsEntity;
import com.grupo3.BookVerse.features.groups.readingGroups.domain.ReadingGroupEntity;
import com.grupo3.BookVerse.features.stories.domain.StoryEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "group_reading_cycles")
public class GroupReadingCycleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_external", unique = true, nullable = false)
    private UUID idExternal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private ReadingGroupEntity group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id")
    private BookEntity book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_id")
    private StoryEntity story;

    @Column(name = "edition_reference", length = 150)
    private String editionReference;

    @Column(name = "total_chapters")
    private Integer totalChapters;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private GroupReadingCycleStatus status;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "readingCycle", fetch = FetchType.LAZY)
    @Builder.Default
    private List<GroupGoalsEntity> goals = new ArrayList<>();

    @PrePersist
    protected void onCreate() {

        if (idExternal == null) {
            idExternal = UUID.randomUUID();
        }

        if (status == null) {
            status = GroupReadingCycleStatus.PLANNED;
        }

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        validateContentAssociation();
        validateBookReference();
    }

    @PreUpdate
    protected void onUpdate() {
        validateContentAssociation();
        validateBookReference();
    }

    private void validateContentAssociation() {

        boolean hasBook = book != null;
        boolean hasStory = story != null;

        if (hasBook == hasStory) {
            throw new IllegalStateException(
                    "A reading cycle must be associated with exactly one book or one story"
            );
        }
    }

    private void validateBookReference() {

        if (story != null) {

            if (editionReference != null || totalChapters != null) {
                throw new IllegalStateException(
                        "Edition and total chapters are only allowed for book reading cycles"
                );
            }

            return;
        }

        if (book != null) {

            if (editionReference == null || editionReference.isBlank()) {
                throw new IllegalStateException(
                        "Edition reference is required for book reading cycles"
                );
            }

            if (totalChapters == null || totalChapters <= 0) {
                throw new IllegalStateException(
                        "Total chapters must be greater than 0 for book reading cycles"
                );
            }
        }
    }
}