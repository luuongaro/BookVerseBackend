package com.grupo3.BookVerse.features.stories.repository;

import com.grupo3.BookVerse.features.stories.domain.StoryAccessType;
import com.grupo3.BookVerse.features.stories.domain.StoryEntity;
import com.grupo3.BookVerse.features.friends.Friendship;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoryRepository extends JpaRepository<StoryEntity, Long> {

    Optional<StoryEntity> findByIdExternal(UUID idExternal);

    Optional<StoryEntity> findByIdExternalAndDeletedFalse(UUID idExternal);

    Page<StoryEntity> findAllByDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    Page<StoryEntity> findByAuthorIdNotAndDeletedFalseAndHiddenFalseOrderByCreatedAtDesc(Long authorId, Pageable pageable);

    @Query("""
            SELECT s FROM StoryEntity s WHERE s.author.id <> :readerId AND s.deleted = false AND s.hidden = false
              AND (:title = '' OR LOCATE(LOWER(:title), LOWER(s.title)) > 0)
              AND (:author = '' OR LOCATE(LOWER(:author), LOWER(s.author.username)) > 0)
              AND (:genre = '' OR LOWER(s.genre) = LOWER(:genre))
              AND (:friendsOnly = false OR EXISTS (
                  SELECT f.id FROM Friendship f WHERE f.status = :acceptedStatus AND
                    ((f.userLow.id = :readerId AND f.userHigh.id = s.author.id)
                      OR (f.userHigh.id = :readerId AND f.userLow.id = s.author.id))))
            ORDER BY s.createdAt DESC, s.id DESC
            """)
    Page<StoryEntity> searchExplore(@Param("readerId") Long readerId, @Param("title") String title,
                                   @Param("author") String author, @Param("genre") String genre,
                                   @Param("friendsOnly") boolean friendsOnly,
                                   @Param("acceptedStatus") Friendship.Status acceptedStatus, Pageable pageable);

    @Query("""
            SELECT DISTINCT s.genre FROM StoryEntity s
            WHERE s.author.id <> :readerId AND s.deleted = false AND s.hidden = false
              AND s.genre IS NOT NULL AND TRIM(s.genre) <> ''
            ORDER BY s.genre
            """)
    List<String> findExploreGenres(@Param("readerId") Long readerId);

    Page<StoryEntity> findByAuthorIdAndDeletedFalseOrderByCreatedAtDesc(Long authorId, Pageable pageable);

    Page<StoryEntity> findByAuthorIdAndDeletedFalseAndHiddenFalseOrderByCreatedAtDescIdDesc(Long authorId, Pageable pageable);

    long countByAuthorIdAndDeletedFalse(Long authorId);

    @Query("""
            SELECT s FROM StoryEntity s
            WHERE s.author.idExternal = :authorId AND s.deleted = false AND s.hidden = false
              AND EXISTS (SELECT c.id FROM ChapterEntity c
                          WHERE c.story = s AND c.published = true AND c.deleted = false)
            ORDER BY s.createdAt DESC, s.id DESC
            """)
    java.util.List<StoryEntity> findPublishedForAuthor(@Param("authorId") UUID authorId, Pageable pageable);

    @Modifying(
            flushAutomatically = true,
            clearAutomatically = true
    )
    @Query("""
        UPDATE StoryEntity story
        SET story.accessType = :newAccessType
        WHERE story.author.id = :authorId
          AND story.accessType = :currentAccessType
          AND story.deleted = false
        """)
    int updateAccessTypeByAuthorId(
            @Param("authorId") Long authorId,
            @Param("currentAccessType")
            StoryAccessType currentAccessType,
            @Param("newAccessType")
            StoryAccessType newAccessType
    );

    List<StoryEntity> findByAuthorIdAndDeletedFalse(Long authorId);
}
