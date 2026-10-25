package com.grupo3.BookVerse.features.reviews.dto;

import com.grupo3.BookVerse.features.reviews.domain.ReviewTakedownStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReviewResponseDto(

        UUID reviewId,
        UUID reviewerId,
        String reviewerName,
        UUID bookId,
        UUID storyId,
        Integer rating,
        String content,
        boolean deleted,
        ReviewTakedownStatus takedownStatus,
        LocalDateTime takedownAt,
        String takedownReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {}