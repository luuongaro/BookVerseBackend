package com.grupo3.BookVerse.features.reviews.dto;

import jakarta.validation.constraints.*;

import java.util.UUID;

public record ReviewCreateRequestDto(

        UUID bookId,
        UUID storyId,

        @NotNull(message = "Rating is required")
        @Min(value = 1, message = "Rating must be at least 1")
        @Max(value = 5, message = "Rating cannot exceed 5")
        Integer rating,

        @NotBlank(message = "Review content cannot be empty")
        @Size(
                min = 10,
                max = 5000,
                message = "Review content must be between 10 and 5000 characters"
        )
        String content

) {
}