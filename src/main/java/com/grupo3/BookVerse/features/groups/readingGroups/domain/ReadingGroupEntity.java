package com.grupo3.BookVerse.features.groups.readingGroups.domain;

import com.grupo3.BookVerse.features.books.domain.BookEntity;
import com.grupo3.BookVerse.features.groups.groupGoals.domain.GroupGoalsEntity;
import com.grupo3.BookVerse.features.groups.groupComment.domain.GroupCommentEntity;
import com.grupo3.BookVerse.features.groups.groupMember.domain.GroupMemberEntity;
import com.grupo3.BookVerse.features.stories.domain.StoryEntity;
import com.grupo3.BookVerse.features.users.domain.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

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
@Table(name = "reading_groups")
public class ReadingGroupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; //PK

    @Column(name = "id_external", unique = true, nullable = false)
    private UUID idExternal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id")
    private BookEntity book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_id")
    private StoryEntity story;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private UserEntity createdBy;

    @OneToMany(mappedBy = "group", fetch = FetchType.LAZY)
    private List<GroupCommentEntity> comments = new ArrayList<>();

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "edition_reference", length = 150)
    private String editionReference;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "total_chapters")
    private Integer totalChapters;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "group", fetch = FetchType.LAZY)
    private List<GroupMemberEntity> members = new ArrayList<>();

    @OneToMany(mappedBy = "group", fetch = FetchType.LAZY)
    private List<GroupGoalsEntity> goals = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.idExternal == null) {
            this.idExternal = UUID.randomUUID();
        }
        validateContentAssociation();
    }

    @PreUpdate
    protected void onUpdate() {
        validateContentAssociation();
    }

    private void validateContentAssociation() {
        boolean hasBook = this.book != null;
        boolean hasStory = this.story != null;

        if (hasBook == hasStory) {
            throw new IllegalStateException("A reading group must be associated with exactly one book or one story");
        }
    }
}