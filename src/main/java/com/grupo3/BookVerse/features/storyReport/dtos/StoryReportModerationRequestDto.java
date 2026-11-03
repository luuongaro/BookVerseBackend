package com.grupo3.BookVerse.features.storyReport.dtos;

import jakarta.validation.constraints.Size;

public record StoryReportModerationRequestDto(

        @Size(max = 3000,
                message = "Resolution comment must not exceed 3000 characters")
        String resolutionComment

) {}
