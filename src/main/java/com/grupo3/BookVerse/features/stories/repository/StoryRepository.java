package com.grupo3.BookVerse.features.stories.repository;

import com.grupo3.BookVerse.features.stories.domain.StoryAccessType;
import com.grupo3.BookVerse.features.stories.domain.StoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoryRepository extends JpaRepository<StoryEntity, Long> {

    Optional<StoryEntity> findByIdExternal(UUID idExternal);

    Optional<StoryEntity> findByIdExternalAndDeletedFalse(UUID idExternal);

    Page<StoryEntity> findAllByDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    Page<StoryEntity> findByAuthorIdAndDeletedFalseOrderByCreatedAtDesc(Long authorId, Pageable pageable);

    long countByAuthorIdAndDeletedFalse(Long authorId);

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
}
