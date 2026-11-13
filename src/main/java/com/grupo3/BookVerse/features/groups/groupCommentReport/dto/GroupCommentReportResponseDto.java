package com.grupo3.BookVerse.features.groups.groupCommentReport.dto;

import com.grupo3.BookVerse.features.groups.groupCommentReport.domain.GroupCommentReportStatus;
import com.grupo3.BookVerse.features.status.domain.ProgressType;

import java.time.LocalDateTime;
import java.util.UUID;

public record GroupCommentReportResponseDto(

        UUID reportId,

        UUID commentId,

        UUID groupId,

        UUID readingCycleId,

        UUID commentAuthorId,

        String commentAuthorUsername,

        String commentContent,

        ProgressType commentProgressType,

        Integer commentProgressValue,

        UUID reporterUserId,

        String reporterUsername,

        UUID moderatorUserId,

        String reason,

        String details,

        String resolutionComment,

        GroupCommentReportStatus status,

        LocalDateTime createdAt,

        LocalDateTime updatedAt,

        LocalDateTime resolvedAt

) {
}