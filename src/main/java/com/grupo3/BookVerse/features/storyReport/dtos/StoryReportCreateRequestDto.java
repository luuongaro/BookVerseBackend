package com.grupo3.BookVerse.features.storyReport.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record StoryReportCreateRequestDto(

        @NotNull(message = "Story id is required")
        UUID storyId,

        @NotBlank(message = "Reason cannot be empty")
        @Size(min = 5, max = 1000,
                message = "Reason must be between 5 and 1000 characters")
        String reason,

        @Size(max = 3000,
                message = "Details must not exceed 3000 characters")
        String details

) {}
