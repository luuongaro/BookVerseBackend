package com.grupo3.BookVerse.features.chapters.repository;

import com.grupo3.BookVerse.features.chapters.domain.ChapterEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.grupo3.BookVerse.features.chapters.dto.ChapterSummaryDto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChapterRepository extends JpaRepository<ChapterEntity, Long> {

    long countByStoryId(Long storyId);

    long countByStoryIdAndDeletedFalseAndPublishedTrue(Long storyId);

    @Query("""
            SELECT new com.grupo3.BookVerse.features.chapters.dto.ChapterSummaryDto(
                c.idExternal, c.chapterNumber, c.title, c.published, c.createdAt)
            FROM ChapterEntity c
            WHERE c.story.id = :storyId AND c.deleted = false
              AND (:includeDrafts = true OR c.published = true)
            ORDER BY c.chapterNumber ASC
            """)
    List<ChapterSummaryDto> findSummaries(@Param("storyId") Long storyId,
                                          @Param("includeDrafts") boolean includeDrafts);

    Optional<ChapterEntity> findByIdExternalAndDeletedFalse(UUID idExternal);

    Page<ChapterEntity> findAllByDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    Page<ChapterEntity> findByStoryIdAndDeletedFalseOrderByChapterNumberAsc(Long storyId, Pageable pageable);

    Optional<ChapterEntity> findTopByStoryIdOrderByChapterNumberDesc(Long storyId);

    List<ChapterEntity> findByStoryIdAndDeletedFalseOrderByChapterNumberAsc(
            Long storyId
    );

    Optional<ChapterEntity> findTopByStoryIdAndDeletedFalseOrderByChapterNumberDesc(
            Long storyId
    );

    Optional<ChapterEntity>
    findByStoryIdExternalAndChapterNumber(UUID storyId, Integer chapterNumber);

    Page<ChapterEntity> findByStoryIdAndDeletedFalseAndPublishedTrueOrderByChapterNumberAsc(
            Long storyId, Pageable pageable);

    Optional<ChapterEntity>
    findByStoryIdExternalAndChapterNumberAndDeletedFalseAndPublishedTrue(
            UUID storyId,
            Integer chapterNumber
    );

    Optional<ChapterEntity>
    findTopByStoryIdExternalAndDeletedFalseAndPublishedTrueOrderByChapterNumberDesc(
            UUID storyId
    );

}

