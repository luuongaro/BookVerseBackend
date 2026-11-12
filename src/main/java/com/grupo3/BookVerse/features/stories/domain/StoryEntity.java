package com.grupo3.BookVerse.features.stories.domain;

import com.grupo3.BookVerse.features.chapters.domain.ChapterEntity;
import com.grupo3.BookVerse.features.users.domain.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "stories")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_external", nullable = false, unique = true, updatable = false)
    private UUID idExternal;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 80)
    private String genre;

    @Column(nullable = false, length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_type", nullable = false)
    private StoryAccessType accessType;

    @Column(name = "is_hidden", nullable = false)
    private boolean hidden;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private UserEntity author;

    @OneToMany(mappedBy = "story", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChapterEntity> chapters = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (idExternal == null) {
            idExternal = UUID.randomUUID();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        hidden = false;
        deleted = false;

        if (accessType == null) {
            accessType = StoryAccessType.FREE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
