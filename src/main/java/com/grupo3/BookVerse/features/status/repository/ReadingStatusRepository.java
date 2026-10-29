package com.grupo3.BookVerse.features.status.repository;

import com.grupo3.BookVerse.features.status.domain.ReadingStatusEntity;
import com.grupo3.BookVerse.features.status.domain.ReadingStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReadingStatusRepository extends JpaRepository<ReadingStatusEntity, Long> {

    Optional<ReadingStatusEntity> findByIdExternal(UUID idExternal);

    List<ReadingStatusEntity> findByUserIdExternal(UUID userId);

    List<ReadingStatusEntity> findByUserIdExternalInAndBookIdExternal(
            List<UUID> userIds,
            UUID bookId
    );

    List<ReadingStatusEntity> findByUserIdExternalInAndStoryIdExternal(
            List<UUID> userIds,
            UUID storyId
    );

    @Query("""
            SELECT COUNT(DISTINCT rs.story.id)
            FROM ReadingStatusEntity rs
            WHERE rs.user.idExternal = :userId
              AND rs.story IS NOT NULL
              AND rs.story.author.id <> rs.user.id
              AND rs.status IN :activeStatuses
            """)
    long countDistinctActiveStoriesByUserExcludingOwnStories(
            @Param("userId") UUID userId,
            @Param("activeStatuses") List<ReadingStatusEnum> activeStatuses
    );

    @Query("""
            SELECT COUNT(DISTINCT rs.story.id)
            FROM ReadingStatusEntity rs
            WHERE rs.user.idExternal = :userId
              AND rs.story IS NOT NULL
              AND rs.story.author.id <> rs.user.id
              AND rs.status IN :activeStatuses
              AND rs.idExternal <> :readingStatusId
            """)
    long countDistinctActiveStoriesByUserExcludingOwnStoriesAndCurrentStatus(
            @Param("userId") UUID userId,
            @Param("activeStatuses") List<ReadingStatusEnum> activeStatuses,
            @Param("readingStatusId") UUID readingStatusId
    );

    @Query("""
            SELECT CASE WHEN COUNT(rs) > 0 THEN true ELSE false END
            FROM ReadingStatusEntity rs
            WHERE rs.user.idExternal = :userId
              AND rs.story.idExternal = :storyId
              AND rs.status IN :activeStatuses
            """)
    boolean existsActiveReadingByUserAndStory(
            @Param("userId") UUID userId,
            @Param("storyId") UUID storyId,
            @Param("activeStatuses") List<ReadingStatusEnum> activeStatuses
    );

    @Query("""
            SELECT CASE WHEN COUNT(rs) > 0 THEN true ELSE false END
            FROM ReadingStatusEntity rs
            WHERE rs.user.idExternal = :userId
              AND rs.story.idExternal = :storyId
              AND rs.status IN :activeStatuses
              AND rs.idExternal <> :readingStatusId
            """)
    boolean existsActiveReadingByUserAndStoryAndCurrentStatusNot(
            @Param("userId") UUID userId,
            @Param("storyId") UUID storyId,
            @Param("activeStatuses") List<ReadingStatusEnum> activeStatuses,
            @Param("readingStatusId") UUID readingStatusId
    );
    boolean existsByUser_IdExternalAndBook_IdExternal(
            UUID userId,
            UUID bookId
    );

    boolean existsByUser_IdExternalAndStory_IdExternal(
            UUID userId,
            UUID storyId
    );

    Optional<ReadingStatusEntity> findByUserIdExternalAndBookIdExternal(
            UUID userId,
            UUID bookId
    );

    boolean existsByUser_IdExternalAndBook_IdExternalAndIdExternalNot(
            UUID userId,
            UUID bookId,
            UUID readingStatusId
    );

    boolean existsByUser_IdExternalAndStory_IdExternalAndIdExternalNot(
            UUID userId,
            UUID storyId,
            UUID readingStatusId
    );




}
