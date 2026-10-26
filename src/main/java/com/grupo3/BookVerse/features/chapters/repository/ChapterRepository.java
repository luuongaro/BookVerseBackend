package com.grupo3.BookVerse.features.chapters.repository;

import com.grupo3.BookVerse.features.chapters.domain.ChapterEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ChapterRepository extends JpaRepository<ChapterEntity, Long> {

    Optional<ChapterEntity> findByIdExternalAndDeletedFalse(UUID idExternal);

    Page<ChapterEntity> findAllByDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    Page<ChapterEntity> findByStoryIdAndDeletedFalseOrderByChapterNumberAsc(Long storyId, Pageable pageable);

    Optional<ChapterEntity> findTopByStoryIdOrderByChapterNumberDesc(Long storyId);

    Optional<ChapterEntity>
    findByStoryIdExternalAndChapterNumber(UUID storyId, Integer chapterNumber);

    Page<ChapterEntity> findByStoryIdAndDeletedFalseAndPublishedTrueOrderByChapterNumberAsc(
            Long storyId, Pageable pageable);
}


