package com.grupo3.BookVerse.features.storyReport.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

public record StoryReportResponseDto(

        UUID reportId,
        UUID storyId,
        String storyTitle,
        String storyAuthorUsername,
        UUID reporterUserId,
        String reporterUsername,
        UUID moderatorUserId,
        String reason,
        String details,
        String resolutionComment,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime resolvedAt

) {}
