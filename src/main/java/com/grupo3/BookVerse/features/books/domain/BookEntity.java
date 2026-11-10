package com.grupo3.BookVerse.features.books.domain;

import com.grupo3.BookVerse.features.groups.groupReadingCycle.domain.GroupReadingCycleEntity;
import com.grupo3.BookVerse.features.status.domain.ReadingStatusEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "books")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_external", unique = true, nullable = false)
    private UUID idExternal;

    @Column(name = "google_book_id", unique = true, nullable = false)
    private String googleBookId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 20)
    private String isbn;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column
    private String publisher;

    @Column(name = "published_date")
    private String publishedDate;

    @Column
    private String language;

    @Column
    private String categories;

    @ElementCollection
    @CollectionTable(
            name = "book_author_names",
            joinColumns = @JoinColumn(name = "book_id")
    )
    @Column(name = "author_name")
    @Builder.Default
    private Set<String> authors = new HashSet<>();

    @Column(nullable = false)
    private Boolean deleted;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "book", fetch = FetchType.LAZY)
    @Builder.Default
    private List<ReadingStatusEntity> readingStatuses = new ArrayList<>();

    @OneToMany(mappedBy = "book", fetch = FetchType.LAZY)
    @Builder.Default
    private List<GroupReadingCycleEntity> readingCycles = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        this.idExternal = UUID.randomUUID();
        this.deleted = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}