package com.grupo3.BookVerse.features.storyReport.repository;

import com.grupo3.BookVerse.features.storyReport.domain.StoryReportEntity;
import com.grupo3.BookVerse.features.storyReport.domain.StoryReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoryReportRepository
        extends JpaRepository<StoryReportEntity, Long> {

    Optional<StoryReportEntity> findByIdExternal(UUID idExternal);

    List<StoryReportEntity> findAllByOrderByCreatedAtDesc();

    List<StoryReportEntity> findByStatusOrderByCreatedAtDesc(
            StoryReportStatus status
    );

    List<StoryReportEntity> findAllByStory_IdAndStatus(
            Long storyId,
            StoryReportStatus status
    );

    boolean existsByStory_IdAndReporterUser_IdAndStatus(
            Long storyId,
            Long reporterUserId,
            StoryReportStatus status
    );
}
